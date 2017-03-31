package ru.asm.core.torrent;

import com.frostwire.jlibtorrent.*;
import com.frostwire.jlibtorrent.alerts.Alert;
import com.frostwire.jlibtorrent.alerts.AlertType;
import com.frostwire.jlibtorrent.alerts.BlockFinishedAlert;
import com.frostwire.jlibtorrent.alerts.TorrentAddedAlert;
import org.apache.commons.lang.mutable.MutableFloat;
import org.slf4j.*;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * User: artem.smirnov
 * Date: 13.03.2017
 * Time: 12:32
 */
public class TorrentClient {

    public static final int TIMEOUT = 30;
    public static final Logger logger = LoggerFactory.getLogger(TorrentClient.class);

    static {
        System.setProperty("jlibtorrent.jni.path", "C:\\TEMP\\find-music\\jlibtorrent\\jlibtorrent-windows-1.2.0.6\\lib\\x86_64\\jlibtorrent.dll");
    }

    private SessionManager s = null;

    public void initializeSession() {
        try {
            logger.info("Using libtorrent version: " + LibTorrent.version());

            s = new SessionManager();
            s.start();

            final CountDownLatch signal = new CountDownLatch(1);

            final Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    long nodes = s.stats().dhtNodes();
                    // wait for at least 10 nodes in the DHT.
                    if (nodes >= 10) {
                        logger.info("DHT contains " + nodes + " nodes");
                        signal.countDown();
                        timer.cancel();
                    }
                }
            }, 0, 1000);

            logger.info("Waiting for nodes in DHT (10 seconds)...");
            boolean r = signal.await(10, TimeUnit.SECONDS);
            if (!r) {
                logger.info("DHT bootstrap timeout");
                System.exit(0);
            }
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
            throw new TorrentClientException(ERR_SESSION_INIT_FAILED);
        }
    }

    public void destroySession() {
        s.stop();
        s = null;
    }

    public void download(TorrentInfo ti,
                         File saveDir,
                         Priority[] priorities) {
        try {
            final CountDownLatch signal = new CountDownLatch(1);

            final MutableFloat progress = new MutableFloat(0d);
            final AlertListener listener = new AlertListener() {
                @Override
                public int[] types() {
                    return null;
                }

                @Override
                public void alert(Alert<?> alert) {
                    AlertType type = alert.type();

                    switch (type) {
                        case TORRENT_ADDED:
                            logger.info("Torrent '{}' added", ti.name());
                            ((TorrentAddedAlert) alert).handle().resume();
                            break;
                        case BLOCK_FINISHED:
                            BlockFinishedAlert a = (BlockFinishedAlert) alert;
                            float p = (float) (a.handle().status().progress() * 100.0);
                            final Float prevProgress = (Float) progress.getValue();
                            if (p > prevProgress) {
                                logger.info(String.format("Progress: %.2f%% for torrent '%s' (total download: %s)", p, a.torrentName(), s.stats().totalDownload()));
                                progress.setValue(p);
                            }
                            break;
                        case TORRENT_FINISHED:
                            logger.info("Torrent '{}' finished", ti.name());
                            signal.countDown();
                            break;
                    }
                }
            };

            s.addListener(listener);
            s.download(ti, saveDir, null, priorities, null);
            signal.await();
            s.removeListener(listener);

        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
            throw new TorrentClientException(ERR_DOWNLOAD_ERROR);
        }
    }

    public TorrentInfo findByMagnet(String uri) {
        final TorrentInfo torrentInfo = fetchMagnet(uri);
        return torrentInfo;
    }

    public TorrentInfo findByHash(String hash) {
        String uri = String.format("magnet:?xt=urn:btih:%s", hash);
        final TorrentInfo torrentInfo = fetchMagnet(uri);

        return torrentInfo;
    }

    private TorrentInfo fetchMagnet(String uri) {
        try {
            logger.info("Fetching the magnet uri, please wait...");
            byte[] data = s.fetchMagnet(uri, 30);

            if (data != null) {
                return TorrentInfo.bdecode(data);
            } else {
                logger.error("Failed to retrieve the magnet");
                return null;
            }
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
            throw new TorrentClientException(ERR_MAGNET_RETRIEVAL_FAILED);
        }

    }

    public static class TorrentClientException extends RuntimeException {

        private String errCode;

        public TorrentClientException(String errCode) {
            super(errCode);
            this.errCode = errCode;
        }

        public String getErrCode() {
            return errCode;
        }
    }

    public static final String ERR_SESSION_INIT_FAILED = "ERR_SESSION_INIT_FAILED";
    public static final String ERR_MAGNET_RETRIEVAL_FAILED = "ERR_MAGNET_RETRIEVAL_FAILED";
    public static final String ERR_DOWNLOAD_ERROR = "ERR_DOWNLOAD_ERROR";
}

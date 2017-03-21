package ru.asm.core.torrent;

import com.frostwire.jlibtorrent.*;
import com.frostwire.jlibtorrent.alerts.Alert;
import com.frostwire.jlibtorrent.alerts.AlertType;
import com.frostwire.jlibtorrent.alerts.BlockFinishedAlert;
import com.frostwire.jlibtorrent.alerts.TorrentAddedAlert;
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
            System.out.println("Using libtorrent version: " + LibTorrent.version());

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
                        System.out.println("DHT contains " + nodes + " nodes");
                        signal.countDown();
                        timer.cancel();
                    }
                }
            }, 0, 1000);

            System.out.println("Waiting for nodes in DHT (10 seconds)...");
            boolean r = signal.await(10, TimeUnit.SECONDS);
            if (!r) {
                System.out.println("DHT bootstrap timeout");
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

    public void download(String name,
                         TorrentInfo ti,
                         File saveDir,
                         Priority[] priorities,
                         File resumeFile) {
        try {

            System.out.println("Using libtorrent version: " + LibTorrent.version());

            final CountDownLatch signal = new CountDownLatch(1);
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
                            System.out.println("Torrent added");
                            ((TorrentAddedAlert) alert).handle().resume();
                            break;
                        case BLOCK_FINISHED:
                            BlockFinishedAlert a = (BlockFinishedAlert) alert;
                            float p = (float) (a.handle().status().progress() * 100.0);
                            System.out.printf("Progress: %.2f%% for torrent name: %s (total download: %s)%n", p, a.torrentName(), s.stats().totalDownload());
                            break;
                        case TORRENT_FINISHED:
                            System.out.println("Torrent finished");
                            signal.countDown();
                            break;
                    }
                }
            };

            s.addListener(listener);
            s.download(ti, saveDir);
            signal.await();
            s.removeListener(listener);

        } catch (Throwable e) {
            e.printStackTrace();
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
            System.out.println("Fetching the magnet uri, please wait...");
            byte[] data = s.fetchMagnet(uri, 30);

            if (data != null) {
                return TorrentInfo.bdecode(data);
            } else {
                System.out.println("Failed to retrieve the magnet");
                throw new AssertionError();
            }
        } catch (Throwable e) {
            throw new TorrentClientException(ERR_MAGNET_RETRIEVAL_FAILED);
        } finally {
            s.stop();
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

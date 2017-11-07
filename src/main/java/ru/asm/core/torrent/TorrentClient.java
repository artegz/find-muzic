package ru.asm.core.torrent;

import com.frostwire.jlibtorrent.*;
import com.frostwire.jlibtorrent.alerts.Alert;
import com.frostwire.jlibtorrent.alerts.AlertType;
import com.frostwire.jlibtorrent.alerts.BlockFinishedAlert;
import com.frostwire.jlibtorrent.alerts.TorrentAddedAlert;
import org.apache.commons.lang.mutable.MutableFloat;
import org.joda.time.DateTime;
import org.joda.time.MutableDateTime;
import org.joda.time.Seconds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.asm.core.AppConfiguration;
import ru.asm.core.progress.TaskProgress;

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

    private static final String ERR_SESSION_INIT_FAILED = "ERR_SESSION_INIT_FAILED";
    private static final String ERR_MAGNET_RETRIEVAL_FAILED = "ERR_MAGNET_RETRIEVAL_FAILED";
    private static final String ERR_DOWNLOAD_ERROR = "ERR_DOWNLOAD_ERROR";

    // todo asm: make methods async with return future

    public static final Logger logger = LoggerFactory.getLogger(TorrentClient.class);

    static {
        System.setProperty("jlibtorrent.jni.path", AppConfiguration.JLIBTORRENT_DLL_LOCATION);
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
                         Priority[] priorities,
                         TaskProgress pTaskProgress) throws TorrentClientException {
        try {
            TaskProgress taskProgress;
            if (pTaskProgress == null) {
                taskProgress = new TaskProgress();
            } else {
                taskProgress = pTaskProgress;
            }

            final CountDownLatch signal = new CountDownLatch(1);

            final MutableDateTime lastActivity = new MutableDateTime(new DateTime());

            final MutableFloat progress = new MutableFloat(0d);
            final AlertListener listener = new AlertListener() {

                private long tId = 0;

                @Override
                public int[] types() {
                    return null;
                }

                @Override
                public void alert(Alert<?> alert) {
                    AlertType type = alert.type();

                    switch (type) {
                        case TORRENT_ADDED:
                            logger.debug("Torrent '{}' added", ti.name());
                            ((TorrentAddedAlert) alert).handle().resume();
                            lastActivity.setDate(new DateTime());
                            tId = taskProgress.startSubTask("downloading...");
                            break;
                        case BLOCK_FINISHED:
                            BlockFinishedAlert a = (BlockFinishedAlert) alert;
                            float p = (float) (a.handle().status().progress() * 100.0);
                            final Float prevProgress = (Float) progress.getValue();
                            if (p > prevProgress) {
                                logger.debug(String.format("Progress: %.2f%% for torrent '%s' (total download: %s)", p, a.torrentName(), s.stats().totalDownload()));
                                progress.setValue(p);
                            }
                            lastActivity.setDate(new DateTime());
                            taskProgress.setSubTaskProgress(tId, (double) a.handle().status().progress());
                            break;
                        case TORRENT_FINISHED:
                            logger.debug("Torrent '{}' finished", ti.name());
                            lastActivity.setDate(new DateTime());
                            signal.countDown();
                            taskProgress.completeSubTask(tId);
                            break;
                    }
                }
            };

            s.addListener(listener);
            s.download(ti, saveDir, null, priorities, null);

            while (true) {
                final boolean finished = signal.await(60, TimeUnit.SECONDS);
                if (finished) {
                    break;
                }

                final Seconds seconds = Seconds.secondsBetween(lastActivity, DateTime.now());
                if (seconds.getSeconds() > 120) {
                    final TorrentHandle torrentHandle = s.find(ti.infoHash());
                    s.remove(torrentHandle);
                    logger.warn("torrent {} downloading interrupted due to time out expired", ti.name());
                    break;
                }
            }

            s.removeListener(listener);

        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
            throw new TorrentClientException(ERR_DOWNLOAD_ERROR);
        }
    }

    public TorrentInfo findByMagnet(String uri) {
        return fetchMagnet(uri);
    }

    private TorrentInfo fetchMagnet(String uri) {
        try {
            logger.debug("Fetching the magnet uri, please wait... ({})", uri);
            byte[] data = s.fetchMagnet(uri, AppConfiguration.getFetchMagnetTimeout());

            if (data != null) {
                logger.debug("Magnet retrieved");
                return TorrentInfo.bdecode(data);
            } else {
                logger.debug("Failed to retrieve the magnet");
                return null;
            }
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
            throw new TorrentClientException(ERR_MAGNET_RETRIEVAL_FAILED);
        }

    }

    public static class TorrentClientException extends RuntimeException {

        private String errCode;

        TorrentClientException(String errCode) {
            super(errCode);
            this.errCode = errCode;
        }

        public String getErrCode() {
            return errCode;
        }
    }
}

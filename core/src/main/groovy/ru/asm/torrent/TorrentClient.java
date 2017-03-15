package ru.asm.torrent;

import com.frostwire.jlibtorrent.*;
import com.frostwire.jlibtorrent.alerts.Alert;
import com.frostwire.jlibtorrent.alerts.AlertType;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * User: artem.smirnov
 * Date: 13.03.2017
 * Time: 12:32
 */
public class TorrentClient {

    public static final int TIMEOUT = 30;
    public static final Object MONITOR = new Object();

    static {
        System.setProperty("jlibtorrent.jni.path", "C:\\TEMP\\torrents\\jlibtorrent-windows-1.1.0.31\\lib\\x86_64\\jlibtorrent.dll");
    }

    private Session s = null;

    private volatile boolean initialized = false;

    private void initializeSession() {
        try {
            if (!initialized) {
                synchronized (this) {
                    if (!initialized) {
                        s = new Session();

                        final CountDownLatch signal = new CountDownLatch(1);

                        // the session stats are posted about once per second.
                        AlertListener l = new AlertListener() {
                            @Override
                            public int[] types() {
                                return new int[]{AlertType.SESSION_STATS.swig(), AlertType.DHT_STATS.swig()};
                            }

                            @Override
                            public void alert(Alert<?> alert) {
                                if (alert.type().equals(AlertType.SESSION_STATS)) {
                                    s.postDHTStats();
                                }

                                if (alert.type().equals(AlertType.DHT_STATS)) {

                                    long nodes = s.getStats().dhtNodes();
                                    // wait for at least 10 nodes in the DHT.
                                    if (nodes >= 10) {
                                        System.out.println("DHT contains " + nodes + " nodes");
                                        signal.countDown();
                                    }
                                }
                            }
                        };

                        s.addListener(l);
                        s.postDHTStats();

                        System.out.println("Waiting for nodes in DHT (10 seconds)...");
                        boolean r = signal.await(10, TimeUnit.SECONDS);
                        if (!r) {
                            System.out.println("DHT bootstrap timeout");
                            throw new AssertionError();
                        }

                        // no more trigger of DHT stats
                        s.removeListener(l);

                        initialized = true;
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
            throw new TorrentClientException(ERR_SESSION_INIT_FAILED);
        }

    }

    public void download(String name,
                         TorrentInfo ti,
                         File saveDir,
                         Priority[] priorities,
                         File resumeFile) {
        try {
            TorrentHandle th = s.findTorrent(ti.infoHash());

            if (th != null) {
                // found a download with the same hash, just adjust the priorities if needed
                if (priorities != null) {
                    if (ti.numFiles() != priorities.length) {
                        throw new IllegalArgumentException("The priorities length should be equals to the number of files");
                    }

                    th.prioritizeFiles(priorities);
                } else {
                    // did they just add the entire torrent (therefore not selecting any priorities)
                    final Priority[] wholeTorrentPriorities = Priority.array(Priority.NORMAL, ti.numFiles());
                    th.prioritizeFiles(wholeTorrentPriorities);
                }
            } else { // new download
                s.asyncAddTorrent(ti, saveDir, priorities, resumeFile);
            }


            while (true) {
                th = s.findTorrent(ti.infoHash());

                final DateTimeFormatter dateTimeFormatter = DateTimeFormat.mediumDateTime();
                final DateTime now = new DateTime();

                if (th != null) {
                    final TorrentStatus.State state = th.getStatus().getState();
                    if (state == TorrentStatus.State.FINISHED) {
                        System.out.printf("%s [%s] downloading finished%n", dateTimeFormatter.print(now), name);
                        return;
                    }
                    if (state == TorrentStatus.State.DOWNLOADING) {
                        System.out.printf("%s [%s] progress: %s%%%n", dateTimeFormatter.print(now), name, th.getStatus().getProgress());
                    } else {
                        System.out.printf("%s [%s] state: %s%n", dateTimeFormatter.print(now), name, state);
                    }
                } else {
                    System.out.printf("%s [%s] torrent not found in session%n", dateTimeFormatter.print(now), name);
                }

                TimeUnit.SECONDS.sleep(60L);
            }

        } catch (Throwable e) {
            e.printStackTrace();
            throw new TorrentClientException(ERR_DOWNLOAD_ERROR);
        }
    }

    public TorrentInfo findByMagnet(String uri) {
        initializeSession();
        final TorrentInfo torrentInfo = fetchMagnet(uri, TIMEOUT);
        return torrentInfo;
    }

    public TorrentInfo findByHash(String hash) {
        initializeSession();

        String uri = String.format("magnet:?xt=urn:btih:%s", hash);
        final TorrentInfo torrentInfo = fetchMagnet(uri, TIMEOUT);

        return torrentInfo;
    }

    private TorrentInfo fetchMagnet(String uri, int timeout) {
        System.out.println("Fetching the magnet uri, please wait...");
        byte[] data = s.fetchMagnet(uri, timeout);

        if (data != null) {
            return TorrentInfo.bdecode(data);
        } else {
            System.out.println("Failed to retrieve the magnet");
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

package edu;

import com.frostwire.jlibtorrent.AlertListener;
import com.frostwire.jlibtorrent.Downloader;
import com.frostwire.jlibtorrent.Entry;
import com.frostwire.jlibtorrent.Session;
import com.frostwire.jlibtorrent.alerts.Alert;
import com.frostwire.jlibtorrent.alerts.AlertType;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * User: artem.smirnov
 * Date: 02.06.2016
 * Time: 15:45
 */
public class TorrentLoaderSample {

    // demos:
    // https://github.com/frostwire/frostwire-jlibtorrent/tree/master/src/test/java/com/frostwire/jlibtorrent/demo

    public static void main(String[] args) throws Throwable {
        System.setProperty("jlibtorrent.jni.path", "C:\\TEMP\\torrents\\jlibtorrent-windows-1.1.0.31\\lib\\x86_64\\jlibtorrent.dll");

        //String uri = "magnet:?xt=urn:btih:86d0502ead28e495c9e67665340f72aa72fe304e&dn=Frostwire.5.3.6.+%5BWindows%5D&tr=udp%3A%2F%2Ftracker.openbittorrent.com%3A80&tr=udp%3A%2F%2Ftracker.publicbt.com%3A80&tr=udp%3A%2F%2Ftracker.istole.it%3A6969&tr=udp%3A%2F%2Fopen.demonii.com%3A1337";
        String uri = "magnet:?xt=urn:btih:a83cc13bf4a07e85b938dcf06aa707955687ca7c";

        final Session s = new Session();

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

        Downloader d = new Downloader(s);

        System.out.println("Waiting for nodes in DHT (10 seconds)...");
        boolean r = signal.await(10, TimeUnit.SECONDS);
        if (!r) {
            System.out.println("DHT bootstrap timeout");
            System.exit(0);
        }

        // no more trigger of DHT stats
        s.removeListener(l);


        System.out.println("Fetching the magnet uri, please wait...");
        byte[] data = d.fetchMagnet(uri, 30000);

        if (data != null) {
            System.out.println(Entry.bdecode(data));
        } else {
            System.out.println("Failed to retrieve the magnet");
        }
    }

//    public static void main(String[] args) {
//
//        System.setProperty("jlibtorrent.jni.path", "C:\\TEMP\\torrents\\jlibtorrent-windows-1.1.0.31\\lib\\x86_64\\jlibtorrent.dll");
//
//        File torrentFile = new File("C:\\TEMP\\torrents\\test.torrent");
//
//        TorrentInfo ti = new TorrentInfo(torrentFile);
//        System.out.println("info-hash: " + ti.infoHash());
//        System.out.println(ti.toEntry());
//
//        final FileStorage files = ti.files();
//
//
//        final int numFiles = files.numFiles();
//
//        for (int i = 0; i < numFiles; i++) {
//            System.out.println(files.filePath(i));
//        }
//    }
}

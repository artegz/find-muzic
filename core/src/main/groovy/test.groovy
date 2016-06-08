import com.frostwire.jlibtorrent.TorrentInfo
import edu.fm.dist.DistinctionEstimator

/**
 * User: artem.smirnov
 * Date: 12.11.2015
 * Time: 10:45
 */

DistinctionEstimator.ignoreParentheses = true

//println DistinctionEstimator.convert("Rainbow - Difficult To Cure")
//println DistinctionEstimator.convert("Rainbow - Difficult To Cure (Beethoven's Ninth)")
//println DistinctionEstimator.convert("Pink Floyd - Another Brick In The Wall, Pt 2 (2011 Remaster)")
//println DistinctionEstimator.convert("Rainbow - Starstruck (Los Angeles Mix)")

println new Date(1350259200000)



//def args = {"C:\\TEMP\\torrents\\test.torrent"};

File torrentFile = new File("C:\\TEMP\\torrents\\test.torrent");

TorrentInfo ti = new TorrentInfo(torrentFile);
System.out.println("info-hash: " + ti.infoHash());
System.out.println(ti.toEntry());
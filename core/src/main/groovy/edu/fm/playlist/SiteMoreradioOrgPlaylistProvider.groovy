package edu.fm.playlist

import edu.fm.SongDescriptor
import groovy.util.logging.Slf4j
import org.apache.http.client.utils.URIBuilder
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

import java.text.SimpleDateFormat
/**
 * User: artem.smirnov
 * Date: 11.11.2015
 * Time: 12:38
 */
@Slf4j
class SiteMoreradioOrgPlaylistProvider implements PlaylistProvider {

    public static final int MAX_TIMEOUT = 60 * 1000
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:41.0) Gecko/20100101 Firefox/41.0"

    public static Map<String, String> ids = [ "nashe" : "3" ]

    public Set<SongDescriptor> fetchPlaylist(Date dateFrom, Date dateTo, String station) {
        def songs = new TreeSet<SongDescriptor>()

        List<Date> days = getDaysInInterval(dateFrom, dateTo)
        days.each {
            try {
                def date = new SimpleDateFormat("dd.MM.yyyy").format(it)
                log.info("searching songs on ${date}...")

                Set<SongDescriptor> daySongs = getSongs(station, it)
                songs.addAll(daySongs)

                log.info("${daySongs.size()} found on ${date}...")
                log.info("succeeded")
            } catch (Throwable e) {
                log.error(e.getMessage())
                log.error("failed")
            }
            sleep(100)
        }

        songs
    }

    private Set<SongDescriptor> getSongs(String station, Date dateOn) {
        Set<SongDescriptor> foundSongNames = new TreeSet<>()

        def id = ids.get(station)
        if (id == null) {
            throw new Exception("unknown station '${station}'");
        }

        //http://www.moreradio.ru/playlist_radio.php?id=3&date=2015.11.10&l=nashe
        URIBuilder urlBuilder = new URIBuilder();
        urlBuilder.setScheme("http")
                .setHost("www.moreradio.org")
                .setPath(getPath(station, dateOn));

        Document doc = Jsoup.connect(urlBuilder.build().toASCIIString())
                .timeout(MAX_TIMEOUT)
                .userAgent(USER_AGENT)
                .cookie("AllTrackRadio", "3")
                .get();

        def playlistDiv = doc.getElementById("playlist")
        def contentDivs = playlistDiv.getElementsByClass("plItemGrey")

        contentDivs.each { div ->
            def artistDiv = div.getElementsByClass("artist").first()
            if (artistDiv == null) {
                log.warn("artist div is null: ${div.text()}")
                return
            }

            def artistLink = artistDiv.getElementsByTag("a").first()
            if (artistLink == null) {
                return
            }

            def artist = artistLink.attr("title")

            def trackDiv = div.getElementsByClass("track").first()
            if (trackDiv == null) {
                log.warn("track div is null: ${div.text()}")
                return
            }

//            def trackLink = trackDiv.getElementsByTag("a").first()
//            if (trackLink == null) {
//                log.warn("track link is null")
//                return
//            }

            def trackSpan = trackDiv.getElementsByTag("span").first()
            if (trackSpan == null) {
                log.warn("track not found: ${div.text()}")
                return
            }
            def track = trackSpan.text()

            if (artist != null && track != null) {
                foundSongNames.add(new SongDescriptor(artist, track))
            } else {
                log.warn("invalid track ${artist} - ${track}")
            }

        }

        foundSongNames
    }

    private GString getPath(String station, Date dateOn) {
        def format = new SimpleDateFormat("dd_MMMMM_yyyy", Locale.ENGLISH).format(dateOn).toLowerCase()
        "/playlist_radio/${station}/${format}"
    }

    private Date nextDay(Date date) {
        def d = new GregorianCalendar()
        d.setTime(date)
        d.add(Calendar.DAY_OF_YEAR, 1)
        def time = d.getTime()
        time
    }

    private ArrayList<Date> getDaysInInterval(Date dateFrom, Date dateTo) {
        def dates = new ArrayList<Date>()

        Date d = dateFrom;
        while (d.compareTo(dateTo) <= 0) {
            dates.add(d)
            d = nextDay(d)
        }
        dates
    }

}

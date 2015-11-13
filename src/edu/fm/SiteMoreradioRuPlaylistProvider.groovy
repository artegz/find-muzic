package edu.fm
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
class SiteMoreradioRuPlaylistProvider {

    public static final int MAX_TIMEOUT = 60 * 1000
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:41.0) Gecko/20100101 Firefox/41.0"

    public static Map<String, String> ids = [ "nashe" : "3" ]

    public Set<String> fetchSongs(Date dateFrom, Date dateTo, String station) {
        def songs = new TreeSet<String>()

        List<Date> days = getDaysInInterval(dateFrom, dateTo)
        days.each {
            try {
                def date = new SimpleDateFormat("dd.MM.yyyy").format(it)
                log.info("searching songs on ${date}...")

                Set<String> daySongs = getSongs(station, it)
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

    private Set<String> getSongs(String station, Date dateOn) {
        Set<String> foundSongNames = new TreeSet<>()

        def id = ids.get(station)
        if (id == null) {
            throw new Exception("unknown station '${station}'");
        }

        //http://www.moreradio.ru/playlist_radio.php?id=3&date=2015.11.10&l=nashe
        URIBuilder urlBuilder = new URIBuilder();
        urlBuilder.setScheme("http")
                .setHost("www.moreradio.ru")
                .setPath("/playlist_radio.php")
                .setParameter("id", id)
                .addParameter("date", new SimpleDateFormat("yyyy.MM.dd").format(dateOn))
                .addParameter("l", station);

        Document doc = Jsoup.connect(urlBuilder.build().toASCIIString()).timeout(MAX_TIMEOUT).userAgent(USER_AGENT).get();

        def contentDivs = doc.getElementsByClass("tx_radiostation")

        if (contentDivs.size() == 2) {
            def contentDiv = contentDivs[0]
            def aTags = contentDiv.getElementsByTag("a")

            aTags.each {
                foundSongNames.add(
                    it.text()
                )
            }
        } else {
            throw new Exception("bad format")
        }

        foundSongNames
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
        while (d.compareTo(dateTo) < 0) {
            dates.add(d)
            d = nextDay(d)
        }
        dates
    }

}

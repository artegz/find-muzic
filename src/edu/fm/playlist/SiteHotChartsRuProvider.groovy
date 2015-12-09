package edu.fm.playlist

import org.apache.http.client.utils.URIBuilder
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

import java.text.SimpleDateFormat

/**
 * User: artem.smirnov
 * Date: 09.12.2015
 * Time: 17:11
 */
class SiteHotChartsRuProvider implements PlaylistProvider {

    public static final int MAX_TIMEOUT = 60 * 1000
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:41.0) Gecko/20100101 Firefox/41.0"

    /*
    history - http://hotcharts.ru/nashe/history/08.12.2015/
    hits - http://hotcharts.ru/nashe/archive/2015.htm
     */

    private ListType listType;

    SiteHotChartsRuProvider(ListType listType) {
        this.listType = listType
    }

    public Set<String> fetchPlaylist(Date dateFrom, Date dateTo, String station) {
        def songs = new TreeSet<String>()

        List<Date> days
        if (listType == ListType.history) {
            days = getDaysInInterval(dateFrom, dateTo)
        } else {
            days = Collections.singletonList(new Date())
        }
        days.each {
            Set<String> daySongs = getSongs(station, it)
            songs.addAll(daySongs)
            sleep(100)
        }
        songs
    }

    private Set<String> getSongs(String station, Date dateOn) {
        Set<String> foundSongNames = new TreeSet<>()

        URIBuilder urlBuilder = new URIBuilder();
        urlBuilder.setScheme("http")
                .setHost("www.hotcharts.ru")
        if (listType == ListType.history) {
            def formattedDate = new SimpleDateFormat("dd.MM.yyyy").format(dateOn)
            urlBuilder.setPath("/${station}/history/${formattedDate}/");
        } else {
            urlBuilder.setPath("/${station}/archive/2015.htm");
        }

        Document doc = Jsoup.connect(urlBuilder.build().toASCIIString()).timeout(MAX_TIMEOUT).userAgent(USER_AGENT).get();

        def songBoxes = doc.getElementsByClass("song_box")

        songBoxes.each {
            foundSongNames.add("${it.child(1).child(0).text()} - ${it.child(1).child(1).text()}")
        }

        foundSongNames
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

    private Date nextDay(Date date) {
        def d = new GregorianCalendar()
        d.setTime(date)
        d.add(Calendar.DAY_OF_YEAR, 1)
        def time = d.getTime()
        time
    }

    enum ListType {
        history,
        top
    }
}

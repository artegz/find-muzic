package edu.fm.links
import edu.fm.Context
import org.apache.http.client.utils.URIBuilder
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
/**
 * User: artem.smirnov
 * Date: 08.12.2015
 * Time: 17:35
 */
class SiteHotChartsRuLinkProvider implements LinkProvider {

    public static final int MAX_TIMEOUT = 60 * 1000
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:41.0) Gecko/20100101 Firefox/41.0"

    public static void main(String[] args) {
        new SiteHotChartsRuLinkProvider().fetchLink("Мельница - Что Ты Знаешь")
    }

    LinkContainer fetchLink(String songName) {
        URIBuilder searchQueryUrlBuilder = new URIBuilder(); // http://hotcharts.ru/mp3/?page=search
        searchQueryUrlBuilder.setScheme("http").setHost("hotcharts.ru").setPath("/mp3/")
                .setParameter("page", "search");

        def doc = Jsoup.connect(searchQueryUrlBuilder.build().toASCIIString())
                .timeout(MAX_TIMEOUT)
                .userAgent(USER_AGENT)
                .data("song", songName.replaceAll("& ", ""))
                .post()

        Elements songBoxDivs = doc.getElementsByAttributeValue("class", "song_box")


        if (!songBoxDivs.isEmpty()) {
            Element mostMatchingDiv = Context.get().distinctionEstimator.getSimilar(songBoxDivs, songName, {
                def artistSong = it.child(1)
                "${artistSong.child(0).text()} - ${artistSong.child(1).text()}"
            })

            def searchSongHref = mostMatchingDiv.child(1).child(1).attr("href")


            URIBuilder songUrlBuilder = new URIBuilder();
            songUrlBuilder.setScheme("http").setHost("hotcharts.ru")
            def songUrl = songUrlBuilder.build().toASCIIString() + searchSongHref
            def songDoc = Jsoup.connect(songUrl)
                    .timeout(MAX_TIMEOUT)
                    .userAgent(USER_AGENT)
//                    .data("song", songName.replaceAll("& ", ""))
                    .get()

            def foundLinksList = songDoc.getElementsByClass("b_list_links")



            if (!foundLinksList.isEmpty()) {
                def listItems = foundLinksList[0].getElementsByTag("li")

                listItems.each {
                    if (it.hasAttr("data-direct-file") && it.attr("data-direct-file").equals("1")) {
                        def links = it.getElementsByTag("a")

                        if (!links.isEmpty()) {
                            def downloadLink = links[0].attr("href")

                            if ("hotcharts.ru".equals(it.getElementsByClass("source")[0].text())) {
                                println "http://hotcharts.ru/" + downloadLink
                            } else {
                                println downloadLink
                            }
                        }
                    }
                }
            }
        }

        null // todo: ...
    }
}

package edu.fm.links
import edu.fm.Context
import edu.fm.SongDescriptor
import groovy.util.logging.Slf4j
import org.apache.http.client.utils.URIBuilder
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
/**
 * User: artem.smirnov
 * Date: 08.12.2015
 * Time: 17:35
 */
@Slf4j
class SiteHotChartsRuLinkProvider implements LinkProvider {

    public static final int MAX_TIMEOUT = 60 * 1000
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:41.0) Gecko/20100101 Firefox/41.0"

    LinkContainer fetchLink(SongDescriptor song) {
        String songName = Context.get().songDescriptorMapper.formatSongDescriptor(song)

        def linkContainers = new ArrayList<LinkContainer>()

        URIBuilder searchQueryUrlBuilder = new URIBuilder(); // http://hotcharts.ru/mp3/?page=search
        searchQueryUrlBuilder.setScheme("http").setHost("hotcharts.ru").setPath("/mp3/")
                .setParameter("page", "search");

        def doc = Jsoup.connect(searchQueryUrlBuilder.build().toASCIIString())
                .timeout(MAX_TIMEOUT)
                .userAgent(USER_AGENT)
                .data("song", prepareSearchValue(song))
                .post()

        Elements songBoxDivs = doc.getElementsByAttributeValue("class", "song_box")

        if (!songBoxDivs.isEmpty()) {
            log.info("${songBoxDivs.size()} matches found... ")

            Element mostMatchingDiv = Context.get().distinctionEstimator.getSimilar(songBoxDivs, songName, {
                def artistSong = it.child(1)
                SiteHotChartsRuTools.getSeparatedName(artistSong)
            })

            def separatedName = SiteHotChartsRuTools.getSeparatedName(mostMatchingDiv.child(1))
            log.info("accepted '${separatedName}'... ")

            // url to resolve download link
            def searchSongHref = mostMatchingDiv.child(1).child(1).attr("href")

            log.info("resolving download link from '${searchSongHref}'... ")

            URIBuilder songUrlBuilder = new URIBuilder();
            songUrlBuilder.setScheme("http").setHost("hotcharts.ru")
            def songUrl = songUrlBuilder.build().toASCIIString() + searchSongHref
            def songDoc = Jsoup.connect(songUrl)
                    .timeout(MAX_TIMEOUT)
                    .userAgent(USER_AGENT)
                    .get()

            def foundLinksList = songDoc.getElementsByClass("b_list_links")

            if (!foundLinksList.isEmpty()) {
                log.info("${foundLinksList.size()} candidate links found... ")

                def listItems = foundLinksList[0].getElementsByTag("li")

                listItems.each {
                    if (it.hasAttr("data-direct-file") && it.attr("data-direct-file").equals("1")) {
                        def links = it.getElementsByTag("a")

                        if (!links.isEmpty()) {
                            def downloadLink = links[0].attr("href")
                            def source = it.getElementsByClass("source")[0].text()

                            if ("hotcharts.ru".equals(source)) {
                                log.info("download link resolved - '${downloadLink}'... ")
                                linkContainers.add(new LinkContainer(separatedName, songName, "http://hotcharts.ru/" + downloadLink))
                            } else {
                                log.info("download link resolved - '${downloadLink}'... ")
                                linkContainers.add(new LinkContainer(separatedName, songName, downloadLink))
                            }
                        } else {
                            throw new Exception("element with data found but link missing")
                        }
                    } else {
                        // no data to download - skip
                    }
                }
            } else {
                throw new Exception("unable to resolve download link - nothing found")
            }
        } else {
            throw new Exception("nothing found")
        }

        if (linkContainers.isEmpty()) {
            throw new Exception("nothing found")
        }

        linkContainers.get(0)
    }

    private String prepareSearchValue(SongDescriptor sd) {
        sd.artist.replaceAll("& -", "") + " - " + sd.title.replaceAll("& -", "")
    }
}

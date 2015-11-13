package edu.fm

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
import org.apache.http.client.utils.URIBuilder
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

/**
 * User: artem.smirnov
 * Date: 13.11.2015
 * Time: 10:30
 */
@Slf4j
class SiteZaycevNetDownloadProvider {

    public static final int MAX_TIMEOUT = 60 * 1000
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:41.0) Gecko/20100101 Firefox/41.0"

    static SongData fetchSong(String songName) {
        URIBuilder searchQueryUrlBuilder = new URIBuilder();
        searchQueryUrlBuilder.setScheme("http").setHost("zaycev.net").setPath("/search.html")
                .setParameter("query_search", songName.replaceAll("& ", ""));

        Document doc = Jsoup.connect(searchQueryUrlBuilder.build().toASCIIString()).timeout(MAX_TIMEOUT).userAgent(USER_AGENT).get();
        Elements searchResultDivs = doc.getElementsByAttributeValue("class", "musicset-track-list__items")

        if (!searchResultDivs.isEmpty()) {
            def children = searchResultDivs.get(0).children()

            Element searchResultsDiv = DistinctionEstimator.getSimilar(children, songName, { it.child(0).text() })
            Elements elements = searchResultsDiv.getElementsByAttribute("data-id")

            if (!elements.isEmpty()) {
                Element resultDiv = elements.get(0)

                String foundSongName = resultDiv.child(0).text()

                log.info("${children.size()} matches found... ")
                log.info("accepted '${foundSongName}'... ")

                String dataUrl = resultDiv.attr("data-url")

                URIBuilder getDownloadLinkUrlBuilder = new URIBuilder();
                getDownloadLinkUrlBuilder.setScheme("http").setHost("zaycev.net").setPath(dataUrl);

                String jsonResult = Jsoup.connect(getDownloadLinkUrlBuilder.build().toASCIIString()).ignoreContentType(true).execute().body()

                ObjectMapper mapper = new ObjectMapper();
                DownloadLink downloadLink = mapper.readValue(jsonResult, DownloadLink.class);

                String downloadUrl
                if (downloadLink.getUrl().indexOf("?") >= 0) {
                    downloadUrl = downloadLink.getUrl().substring(0, downloadLink.getUrl().indexOf("?"))
                } else {
                    downloadUrl = downloadLink.getUrl()
                }

                new SongData(foundSongName, songName, downloadUrl)
            } else {
                throw new Exception("nothing found")
            }
        } else {
            throw new Exception("nothing found")
        }
    }

    static class DownloadLink {
        String url
    }
}

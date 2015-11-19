package edu.fm.links

import edu.fm.Context
import groovy.util.logging.Slf4j
import org.apache.http.client.utils.URIBuilder
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
/**
 * User: artem.smirnov
 * Date: 06.11.2015
 * Time: 9:48
 */
@Slf4j
class Site7BxRuLinkProvider implements LinkProvider {

    public static final int MAX_TIMEOUT = 60 * 1000
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:41.0) Gecko/20100101 Firefox/41.0"

    LinkContainer fetchLink(String songName) {

        URIBuilder searchQueryUrlBuilder = new URIBuilder();
        searchQueryUrlBuilder.setScheme("http").setHost("7bx.ru").setPath("/mp3_search/")
                .setParameter("tag", songName.replaceAll("& ", ""));
    
        Document doc = Jsoup.connect(searchQueryUrlBuilder.build().toASCIIString()).timeout(MAX_TIMEOUT).userAgent(USER_AGENT).get();
    
        def contentDiv = doc.getElementById("content")
        def aTags = contentDiv.getElementsByTag("a")

        if (!aTags.isEmpty()) {
            Element resultATag = Context.get().distinctionEstimator.getSimilar(aTags, songName, { it.text() })

            String foundSongName = resultATag.text()

            log.info("${aTags.size()} matches found... ")
            log.info("accepted '${foundSongName}'... ")
    
            def resolveUrl = resultATag.attr("href")
    
            Document resolvedDoc = Jsoup.connect(resolveUrl).timeout(MAX_TIMEOUT).userAgent(USER_AGENT).get();
    
            def resolvedContentDiv = resolvedDoc.getElementById("content")
            def downloadLinkATags = resolvedContentDiv.getElementsByTag("a")
    
            if (!downloadLinkATags.isEmpty()) {
                def resultDownloadLinkATag = downloadLinkATags.get(0)
                def downloadUrl = resultDownloadLinkATag.attr("href")

                new LinkContainer(foundSongName, songName, downloadUrl)
            } else {
                throw new Exception("download link tags not found")
            }
        } else {
            throw new Exception("nothing found")
        }
    }
}

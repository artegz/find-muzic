package edu.fm.playlist
import edu.fm.Context
import edu.fm.SongDescriptor
import groovy.util.logging.Slf4j
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.conn.ClientConnectionManager
import org.apache.http.conn.scheme.PlainSocketFactory
import org.apache.http.conn.scheme.Scheme
import org.apache.http.conn.scheme.SchemeRegistry
import org.apache.http.conn.ssl.SSLSocketFactory
import org.apache.http.conn.ssl.TrustStrategy
import org.apache.http.impl.client.BasicResponseHandler
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.impl.conn.PoolingClientConnectionManager

import javax.swing.text.Element
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import javax.swing.text.html.HTML
import javax.swing.text.html.HTMLDocument
import javax.swing.text.html.HTMLEditorKit
import javax.swing.text.html.parser.ParserDelegator
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
/**
 * User: artem.smirnov
 * Date: 11.11.2015
 * Time: 10:55
 */
@Slf4j
class Site7bxRuPlaylistProvider implements PlaylistProvider {

    private HttpClient httpsClient

    Site7bxRuPlaylistProvider() {
        httpsClient = createSSLHttpClient()
    }

    public Set<SongDescriptor> fetchPlaylist(Date dateFrom, Date dateTo, String station) {
        def songs = new TreeSet<SongDescriptor>()

        List<Date> days = getDaysInInterval(dateFrom, dateTo)
        days.each {
            Set<String> daySongs = getSongs(station, it)
            songs.addAll(daySongs)
            sleep(100)
        }

        Context.get().songDescriptorMapper.parseList(songs)
    }

    /**
     * @deprecated to be reimplemented alike Site7bcRuProvider
     */
    @Deprecated
    private Set<String> getSongs(String station, Date dateOn) {
        String urlDay = new SimpleDateFormat("yyyyMMdd").format(dateOn)
        String urlOverHttps = "https://7bx.ru/radio_track/${station}/${urlDay}"

        log.info("fetching playlist from ${urlOverHttps}...")

        Set<String> foundSongNames
        try {
            HttpGet getMethod = new HttpGet(urlOverHttps)
            HttpResponse response = httpsClient.execute(getMethod)

            if (response.getStatusLine().getStatusCode() == 200) {
                log.info("request succeeded, now parsing...")

                String responseString = new BasicResponseHandler().handleResponse(response);
                HTMLDocument htmlDoc = asHtmlDocument(responseString)

                foundSongNames = parseSongs(htmlDoc)

                def date = new SimpleDateFormat("dd.MM.yyyy").format(dateOn)
                log.info("${foundSongNames.size()} found on ${date}...")
                log.info("succeeded")
            } else {
                throw new Exception("request failed with ${response.getStatusLine().getStatusCode()} status code...");
            }
        } catch (Throwable e) {
            log.error(e.getMessage())
            log.error("failed")
            foundSongNames = new TreeSet<>();
        }

        foundSongNames
    }


    private HTMLDocument asHtmlDocument(String responseString) {
        Reader stringReader = new StringReader(responseString);
        HTMLEditorKit htmlKit = new HTMLEditorKit();
        HTMLDocument htmlDoc = (HTMLDocument) htmlKit.createDefaultDocument();
        HTMLEditorKit.Parser parser = new ParserDelegator();
        parser.parse(stringReader, htmlDoc.getReader(0), true);
        htmlDoc
    }


    private DefaultHttpClient createSSLHttpClient() {
        TrustStrategy acceptingTrustStrategy = new TrustStrategy() {
            @Override
            public boolean isTrusted(X509Certificate[] certificate, String authType) {
                return true;
            }
        };
        SSLSocketFactory sf = new SSLSocketFactory(acceptingTrustStrategy,
                SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("https", 443, sf));
        registry.register(
                new Scheme("http", 90, PlainSocketFactory.getSocketFactory())
        );

        ClientConnectionManager ccm = new PoolingClientConnectionManager(registry);

        DefaultHttpClient httpClient = new DefaultHttpClient(ccm);
        httpClient
    }

    private Object getAttribute(Element element, HTML.Tag tag, HTML.Attribute attribute) {
        SimpleAttributeSet tagAtrs = (SimpleAttributeSet) element.getAttributes().getAttribute(tag)
        tagAtrs.getAttribute(attribute)
    }

    private boolean hasInnerText(Element element) {
        getInnerText(element) != null
    }

    private boolean isTag(Element element, HTML.Tag tag) {
        def attributeSet = element.getAttributes()
        attributeSet.getAttribute(tag) != null
    }

    private String getInnerText(Element element) {
        int startOffset = element.getStartOffset()
        int endOffset = element.getEndOffset()
        int length = endOffset - startOffset

        length >= 0 ? element.getDocument().getText(startOffset, length) : null
    }

    private Set<String> parseSongs(HTMLDocument htmlDoc) {
        Element content = htmlDoc.getElement("content")

        Map<String, String> songSearchQueries = new HashMap<>()

        content.getElement(2).each {
            it.getElement(0).each {
                if (it.getAttributes().getAttribute(StyleConstants.NameAttribute) == HTML.Tag.CONTENT) {
                    if (isTag(it, HTML.Tag.A)) {
                        if (hasInnerText(it)) {
                            songSearchQueries.put(getInnerText(it), (String) getAttribute(it, HTML.Tag.A, HTML.Attribute.HREF))
                        }
                    }
                }
            }
        }
        songSearchQueries.keySet()
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

package edu.fm.links

import org.jsoup.nodes.Element

/**
 * User: artem.smirnov
 * Date: 09.12.2015
 * Time: 17:41
 */
class SiteHotChartsRuTools {

    public static GString getSeparatedName(Element div) {
        "${div.child(0).text()} - ${div.child(1).text()}"
    }
}

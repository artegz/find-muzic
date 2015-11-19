package edu.fm

import edu.fm.dist.DistinctionEstimator
import edu.fm.links.LinkProvider
import edu.fm.links.Site7BxRuLinkProvider
import edu.fm.playlist.PlaylistProvider
import edu.fm.playlist.Site7bxRuPlaylistProvider

/**
 * User: artem.smirnov
 * Date: 19.11.2015
 * Time: 10:36
 */
class Context {

    private static final Context instance = new Context()

    DistinctionEstimator distinctionEstimator = new DistinctionEstimator()

    LinkProvider linkProvider = new Site7BxRuLinkProvider()

    PlaylistProvider playlistProvider = new Site7bxRuPlaylistProvider()

    public static Context get() {
        instance
    }

}

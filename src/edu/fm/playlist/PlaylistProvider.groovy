package edu.fm.playlist

import edu.fm.SongDescriptor

/**
 * User: artem.smirnov
 * Date: 19.11.2015
 * Time: 10:37
 */
interface PlaylistProvider {

    Set<SongDescriptor> fetchPlaylist(Date dateFrom, Date dateTo, String station)

}
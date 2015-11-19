package edu.fm.playlist

/**
 * User: artem.smirnov
 * Date: 19.11.2015
 * Time: 10:37
 */
interface PlaylistProvider {

    Set<String> fetchPlaylist(Date dateFrom, Date dateTo, String station)

}
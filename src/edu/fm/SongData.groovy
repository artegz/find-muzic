package edu.fm

/**
 * User: artem.smirnov
 * Date: 06.11.2015
 * Time: 9:51
 */
class SongData {

    String foundSongName

    String requestedSongName

    String downloadUrl

    SongData(String foundSongName, String requestedSongName, String downloadUrl) {
        this.foundSongName = foundSongName
        this.requestedSongName = requestedSongName
        this.downloadUrl = downloadUrl
    }

    String getFoundSongName() {
        return foundSongName
    }

    String getRequestedSongName() {
        return requestedSongName
    }

    String getDownloadUrl() {
        return downloadUrl
    }
}

package ru.asm.tools

import edu.fm.SongDescriptor

/**
 * User: artem.smirnov
 * Date: 13.03.2017
 * Time: 16:22
 */

// Step 1: download fresh torrents DB (TorrentsDbDownloadTool)
// Step 2: index downloaded torrents DB (IndexTorrentsDbTool)
// Step 3: fetch desired playlist (FetchPlaylistTool)
// Step 4: resolve torrents
//          - groups songs by an artist
//          - find artist torrents
//          - [mp3] put file names into DB/index; [flac] parse song names; put them into DB/index
//          - as output: artist+song - torrent mapping
//          - for each song choose torrent for downloading
//          - download torrent
//          - [mp3] copy required song into result dir; [flac] convert into mp3 and copy required song into result dir


// 2. index artists songs (separate elastic index)
// ... find files (mp3 + flac)

// 3. if mp3 => download;


List<SongDescriptor> songs = new ArrayList<>()
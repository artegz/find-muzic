import {Song} from "./song";
import {Mp3TorrentSongSource} from "./mp3-torrent-song-source";
import {FileDocument} from "./file-document";

export class SongInfo {

  song: Song;

  sources: Mp3TorrentSongSource[];

  files: FileDocument[];
}

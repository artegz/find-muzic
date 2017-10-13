import {Song} from "./dto/song";
import {Mp3TorrentSongSource} from "./dto/mp3-torrent-song-source";

export class LiteTorrentInfo {
  torrentId: string;
  title: string;
  format: string;

  containedSongs: Song[];
  containedSources: Mp3TorrentSongSource[];

  download = false;
}

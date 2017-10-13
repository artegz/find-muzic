import {Song} from "./dto/song";
import {Mp3TorrentSongSource} from "./dto/mp3-torrent-song-source";

export class LiteSourceInfo {

  song: Song;

  songSource: Mp3TorrentSongSource;

  downloadStatus: string;

  numFiles: number;
}

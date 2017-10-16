
import {Song} from "../song";
import {TorrentSongSource} from "../torrent-song-source";

export class TorrentDownloadInfoDto {
  torrentId: string;
  title: string;
  format: string;

  containedSongs: Song[];
  containedSources: TorrentSongSource[];

  download = false;
}

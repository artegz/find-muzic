
import {Song} from "../song";
import {TorrentSongSource} from "../torrent-song-source";
import {FileDocument} from "../file-document";

export class FileOutputInfoDto {

  song: Song;

  songSource: TorrentSongSource;

  file: FileDocument;

}

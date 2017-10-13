import {Song} from "./dto/song";
import {Mp3TorrentSongSource} from "./dto/mp3-torrent-song-source";
import {FileDocument} from "./dto/file-document";

export class LiteFileInfo {

  song: Song;

  songSource: Mp3TorrentSongSource;

  file: FileDocument;

}

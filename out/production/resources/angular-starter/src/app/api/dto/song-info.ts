import {Song} from "./song";
import {Mp3TorrentSongSource} from "./mp3-torrent-song-source";
import {FileDocument} from "./file-document";
import {SongResolveReport} from "../song-resolve-report";

export class SongInfo {

  song: Song;

  sources: Mp3TorrentSongSource[];

  files: FileDocument[];

  resolveReport: SongResolveReport;
}

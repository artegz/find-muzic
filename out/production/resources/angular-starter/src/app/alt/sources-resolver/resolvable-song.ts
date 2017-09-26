import {SongInfo} from "../../api/dto/song-info";
// import {DownloadableSource} from "./downloadable-source";
// import {DownloadedFile} from "./downloaded-file";

export class ResolvableSong {

  _song: SongInfo;

  resolve: boolean = false;

  constructor(song: SongInfo) {
    this._song = song;
  }

  get id() {
    return this._song.song.songId;
  }

  get artistName() {
    return this._song.song.artist.artistName;
  }

  get title() {
    return this._song.song.title;
  }

  get resolveFailed(): boolean {
    let resolveReport = this._song.resolveReport;
    if (!!resolveReport) {
      if (resolveReport.resolvePerformed && !resolveReport.searchPerformed) {
        return true;
      }
    }
    return false;
  }

  get numSources(): number {
    let resolveReport = this._song.resolveReport;
    if (!!resolveReport && resolveReport.foundSources) {
      return resolveReport.foundSources.length;
    }
    return null;
  }

}

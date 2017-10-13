import { LiteSourceInfo } from '../../api/lite-source-info';

export class DownloadableSource {

  _sourceInfo: LiteSourceInfo;

  download = false;

  constructor(sourceInfo: LiteSourceInfo) {
    this._sourceInfo = sourceInfo;
  }

  get sourceId() {
    return this._sourceInfo.songSource.sourceId;
  }

  get songId() {
    return this._sourceInfo.song.songId;
  }

  get torrentId() {
    return this._sourceInfo.songSource.indexSong.torrentId;
  }

  get downloadFailed(): boolean {
    return this._sourceInfo.downloadStatus === 'failed';
  }

  get artistName(): string {
    return this._sourceInfo.song.artist.artistName;
  }

  get songTitle(): string {
    return this._sourceInfo.song.title;
  }

  get numFiles(): number {
    return this._sourceInfo.numFiles;
  }
}

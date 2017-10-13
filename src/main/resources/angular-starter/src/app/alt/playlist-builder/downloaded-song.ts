import { LiteFileInfo } from '../../api/lite-file-info';

export class DownloadedSong {

  private _fileInfo: LiteFileInfo;

  output: boolean;

  constructor(fileInfo: LiteFileInfo) {
    this._fileInfo = fileInfo;
  }

  get fileId() {
    return this._fileInfo.file.id;
  }

  get outputFails(): boolean {
    return false;
  }

  get artistName(): string {
    return this._fileInfo.song.artist.artistName;
  }

  get songTitle(): string {
    return this._fileInfo.song.title;
  }

  get sourceId(): string {
    return this._fileInfo.songSource.sourceId;
  }

  get filePath(): string {
    return this._fileInfo.file.fsLocation;
  }

}

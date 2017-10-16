
import {FileOutputInfoDto} from "../../api/dto/output/file-output-info";

export class DownloadedSong {

  private _fileInfo: FileOutputInfoDto;

  output: boolean;

  constructor(fileInfo: FileOutputInfoDto) {
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

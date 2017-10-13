import { SongInfo } from '../../api/dto/song-info';
import { DownloadableSource } from './downloadable-source';
import { DownloadedFile } from './downloaded-file';

export class ResolvableSong {

  _song: SongInfo;

  sources: DownloadableSource[];
  files: DownloadedFile[];

  resolve: boolean = false;
  inProgress: boolean = false;

  constructor(song: SongInfo) {
    this._song = song;
    if (!!this._song.sources) {
      this.sources = this._song.sources.map(s => new DownloadableSource(s));
    }
    if (!!this._song.files) {
      this.files = this._song.files.map(f => new DownloadedFile(f));
    }
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

}

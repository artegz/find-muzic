import { LiteSongInfo } from '../../api/lite-song-info';

export class ResolvableSong {

  _song: LiteSongInfo;

  resolve: boolean = false;

  constructor(song: LiteSongInfo) {
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
    return this._song.resolveStatus === 'failed';
  }

  get numSources(): number {
    return this._song.numSources;
  }

  get numFiles(): number {
    return this._song.numFiles;
  }

}

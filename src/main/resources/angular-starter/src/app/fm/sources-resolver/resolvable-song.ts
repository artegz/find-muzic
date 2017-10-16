
import {SongResolveInfoDto} from "../../api/dto/resolve/song-resolve-info";

export class ResolvableSong {

  _song: SongResolveInfoDto;

  resolve: boolean = false;

  constructor(song: SongResolveInfoDto) {
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

import {Song} from "./dto/song";

export class LiteSongInfo {
  song: Song;
  numSources: number;
  numFiles: number;

  resolveStatus: string
}

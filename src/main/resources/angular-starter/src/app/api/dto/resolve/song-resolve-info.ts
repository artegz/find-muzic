
import {Song} from "../song";

export class SongResolveInfoDto {
  song: Song;
  numSources: number;
  numFiles: number;

  resolveStatus: string
}

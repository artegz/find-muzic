import {SongEntity} from "./song-entity";
import {Task} from "./task";

export class ProgressInfo {
  songProgresses: { [key: number]: Task };
  songs: SongEntity[];
}

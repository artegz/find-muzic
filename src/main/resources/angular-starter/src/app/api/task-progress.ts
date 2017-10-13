import {Artist} from "./dto/artist";
import {Song} from "./dto/song";
import {SubTaskProgress} from "./sub-task-progress";

export class TaskProgress {

  taskId: string;
  taskName: string;
  taskType: string;

  artist: Artist;
  artistSongs: Song[];

  subTasks: { [key: string]: SubTaskProgress };

  lastMessage: string;
  progress: number;

  getSubTaskIds(): string[] {
    return Object.keys(this.subTasks);
  }

}

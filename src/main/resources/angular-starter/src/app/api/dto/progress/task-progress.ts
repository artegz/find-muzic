import { SubTaskProgressDto } from "./sub-task-progress";

export class TaskProgressDto {

  taskId: string;
  taskName: string;
  taskType: string;

  subTasks: { [key: string]: SubTaskProgressDto };

  lastMessage: string;
  messages: string[];

  progress: number;

  getSubTaskIds(): string[] {
    return Object.keys(this.subTasks);
  }

}


import { Component, OnDestroy, OnInit } from '@angular/core';
import { RestService } from '../../api/rest.service';
import {ProgressInfoDto} from "../../api/dto/progress/progress-info";
import {TaskProgressDto} from "../../api/dto/progress/task-progress";

@Component({
  selector: 'app-progress-info',
  templateUrl: './progress-info.component.html'
})
export class ProgressInfoComponent implements OnInit, OnDestroy {

  progressInfo: ProgressInfoDto;
  timerId: any;

  constructor(private rest: RestService) {}

  ngOnInit(): void {
    this.timerId = setInterval(() => {
      this.rest.getProgressInfo2()
        .subscribe(res => {
          this.progressInfo = res;
        });
    }, 3000);
  }


  ngOnDestroy(): void {
    clearInterval(this.timerId);
  }

  getSubTaskIds(taskProgress: TaskProgressDto) {
    return Object.keys(taskProgress.subTasks);
  }

}

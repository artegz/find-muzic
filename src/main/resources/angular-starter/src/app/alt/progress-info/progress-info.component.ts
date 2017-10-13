
import { Component, OnDestroy, OnInit } from '@angular/core';
import { RestService } from '../../api/rest.service';
import { SimpleProgressInfo } from '../../api/simple-progress-info';
import {TaskProgress} from "../../api/task-progress";

@Component({
  selector: 'app-progress-info',
  templateUrl: './progress-info.component.html'
})
export class ProgressInfoComponent implements OnInit, OnDestroy {

  progressInfo: SimpleProgressInfo;
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

  getSubTaskIds(taskProgress: TaskProgress) {
    return Object.keys(taskProgress.subTasks);
  }

}

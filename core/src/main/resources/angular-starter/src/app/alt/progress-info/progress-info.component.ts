
import {Component, OnDestroy, OnInit} from '@angular/core';
import {RestService} from "../../api/rest.service";
import {ProgressInfo} from "../../api/progress-info";

@Component({
  selector: 'app-progress-info',
  templateUrl: './progress-info.component.html'
})
export class ProgressInfoComponent implements OnInit, OnDestroy {

  progressInfo: ProgressInfo;
  timerId: number;

  constructor(private rest: RestService) {}

  ngOnInit(): void {
    this.timerId = setInterval(() => {
      this.rest.getProgressInfo()
        .subscribe(res => {
          this.progressInfo = res;
        });
    }, 1000);
  }


  ngOnDestroy(): void {
    clearInterval(this.timerId)
  }
}

import { Component, OnInit } from '@angular/core';
import {RestService} from "../api/rest.service";
import {StatusEntity} from "../api/status-entity";

@Component({
  selector: 'my-statuses',
  templateUrl: './statuses.component.html'
})
export class MyStatusesComponent implements OnInit {

  statuses: StatusEntity[];
  p: number = 1;

  constructor(
    private rest: RestService
  ) { }

  ngOnInit() {
    this.rest.getStatuses()
      .subscribe(result => {
        this.statuses = result;
      });
  }

}

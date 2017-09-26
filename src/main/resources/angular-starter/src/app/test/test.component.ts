import { Component, OnInit } from '@angular/core';
import {RestService} from "../api/rest.service";

@Component({
  selector: 'my-test',
  templateUrl: './test.component.html'
})
export class MyTestComponent implements OnInit {

  artists: string[];
  p: number = 1;

  constructor(
    private rest: RestService
  ) { }

  ngOnInit() {
    this.rest.getArtists()
      .subscribe(result => {
        this.artists = result;
      });
  }

}

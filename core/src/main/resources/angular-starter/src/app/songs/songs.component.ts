import { Component, OnInit } from '@angular/core';
import {RestService} from "../api/rest.service";
import {SingleSongResult} from "../api/single-song-result";

@Component({
  selector: 'my-found-songs',
  templateUrl: './songs.component.html'
})
export class MyFoundSongsComponent implements OnInit {

  found: SingleSongResult[];
  notFound: SingleSongResult[];
  p: number = 1;

  constructor(
    private rest: RestService
  ) { }

  ngOnInit() {
    this.rest.getFoundSongs()
      .subscribe(result => {
        this.found = result.found;
        this.notFound = result.notFound;
      });
  }

}

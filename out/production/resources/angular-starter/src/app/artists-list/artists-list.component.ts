import { Component, OnInit } from '@angular/core';
import { RestService } from "../api";

@Component({
  selector: 'app-artists-list',
  templateUrl: './artists-list.component.html'
  // template: `
  //   <ul *ngIf="!!artists">
  //     <li *ngFor="let artist of artists">
  //       {{artist}}
  //     </li>
  //   </ul>
  // `
  /*,
  templateUrl: './artists-list.component.html'*/
    // , styleUrls: ['./artists-list.component.css']
})
export class ArtistsListComponent implements OnInit {

  artists: string[];

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

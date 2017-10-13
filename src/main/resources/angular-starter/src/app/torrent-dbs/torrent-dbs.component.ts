import { Component, OnInit } from '@angular/core';
import { RestService } from '../api/rest.service';

@Component({
  selector: 'my-torrent-dbs',
  templateUrl: './torrent-dbs.component.html'
})
export class TorrentDbsComponent implements OnInit {

  torrentDbs: string[];
  p: number = 1;

  magnet: string;

  constructor(
    private rest: RestService
  ) { }

  ngOnInit(): void {
    this.rest.listTorrentDbs()
      .subscribe(fns => {
        this.torrentDbs = fns;
      });
  }


  download(): void {
    if (!!this.magnet) {
      this.rest.downloadDb(this.magnet)
        .subscribe();
    }
  }

}

import { Component, OnInit } from '@angular/core';
import { RestService } from '../../api/rest.service';
import { DownloadedSong } from './downloaded-song';
import {Router} from "@angular/router";

@Component({
  selector: 'app-playlist-builder',
  templateUrl: './playlist-builder.component.html'
})
export class PlaylistBuilderComponent implements OnInit{

  downloadedSongs: DownloadedSong[];
  p: number = 1;

  constructor(private rest: RestService
              , private router: Router
  ) {}

  ngOnInit(): void {
    this.rest.getDownloadedFiles()
      .subscribe(res => {
        this.downloadedSongs = res.map(info => new DownloadedSong(info));
      });
  }

  select(rs: DownloadedSong): boolean {
    rs.output = !rs.output;
    return false;
  }

  selectAllForOutput(): boolean {
    this.downloadedSongs.forEach(value => value.output = true);
    return false;
  }

  unselectAllForOutput(): boolean {
    this.downloadedSongs.forEach(value => value.output = false);
    return false;
  }

  preparePlaylist(): boolean {
    let ids = this.downloadedSongs.filter(s => s.output)
      .map(s => s.fileId);
    this.rest.buildPlaylist(ids)
      .subscribe(() => {
        this.router.navigate(["/progress"]);
      });
    return false;
  }

}

import { Component, OnInit } from '@angular/core';
import {RestService} from "../../api/rest.service";
import {ResolvableSong} from "./resolvable-song";
import {DownloadableSource} from "./downloadable-source";
import {DownloadedFile} from "./downloaded-file";

@Component({
  selector: 'app-playlist-manager',
  templateUrl: './playlist-manager.component.html'
})
export class PlaylistManagerComponent implements OnInit {

  resolvableSongs: ResolvableSong[];
  selectedSong: ResolvableSong;

  // downloadableSources: DownloadableSource[];
  // downloadedFiles: DownloadedFile[];

  p: number = 1;

  constructor(private rest: RestService) {}

  ngOnInit(): void {
    this.rest.getSongs("nashe-test")
      .subscribe(res => {
        this.resolvableSongs = res.map(info => new ResolvableSong(info));
      });
  }

  get downloadableSources(): DownloadableSource[] {
    return !!this.selectedSong ? this.selectedSong.sources : [];
  }
  get downloadedFiles(): DownloadedFile[] {
    return !!this.selectedSong ? this.selectedSong.files : [];
  }

  onSongSelected(song: ResolvableSong): boolean {
    this.selectedSong = song;
    return false;
  }

  selectAllForResolve() {
    this.resolvableSongs.forEach(value => value.resolve = true);
  }

  unselectAllForResolve() {
    this.resolvableSongs.forEach(value => value.resolve = false);
  }

  selectAllForFetch() {
    this.downloadableSources.forEach(value => value.download = true);
  }

  unselectAllForFetch() {
    this.downloadableSources.forEach(value => value.download = false);
  }

  resolveSources() {
    let ids = this.resolvableSongs.filter(s => s.resolve && !s.inProgress)
      .map(s => {
        s.inProgress = true;
        return s;
      })
      .map(s => s.id);
    this.rest.resolveSongs(ids)
      .switchMap(() => {
        return this.rest.getSongs("nashe-test");
      }).subscribe(res => {
      this.resolvableSongs = res.map(info => new ResolvableSong(info));
    });
  }

  fetchFiles() {
    let sources: {[key:number]:string[]} = {};

    this.resolvableSongs.forEach(s => {
      s.sources.forEach(ss => {
        if (!sources[s.id]) {
          sources[s.id] = [];
        }
        sources[s.id].push(ss.sourceId);
      })
    })

  }
}

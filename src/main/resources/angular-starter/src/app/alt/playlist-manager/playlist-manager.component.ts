import { Component, OnInit } from '@angular/core';
import { RestService } from '../../api/rest.service';
import { ResolvableSong } from './resolvable-song';
import { DownloadableSource } from './downloadable-source';
import { DownloadedFile } from './downloaded-file';
import { Router } from '@angular/router';

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
  sp: number = 1;
  fp: number = 1;

  constructor(private rest: RestService,
              // private route: ActivatedRoute,
              private router: Router) {}

  ngOnInit(): void {
    this.rest.getSongs("nashe", true, true, true, true)
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

  selectAllForResolve(): boolean {
    this.resolvableSongs.forEach(value => value.resolve = true);
    return false;
  }

  unselectAllForResolve(): boolean {
    this.resolvableSongs.forEach(value => value.resolve = false);
    return false;
  }

  selectAllForFetch(): boolean {
    this.downloadableSources.forEach(value => value.download = true);
    return false;
  }

  unselectAllForFetch(): boolean {
    this.downloadableSources.forEach(value => value.download = false);
    return false;
  }

  resolveSources(): boolean {
    let ids = this.resolvableSongs.filter(s => s.resolve && !s.inProgress)
      .map(s => {
        s.inProgress = true;
        return s;
      })
      .map(s => s.id);
    // this.rest.resolveSongs(ids)
    //   .switchMap(() => {
    //     return this.rest.getSongs("nashe");
    //   }).subscribe(res => {
    //   this.resolvableSongs = res.map(info => new ResolvableSong(info));
    // });
    this.rest.resolveSongs(ids)
      .subscribe(() => {
        this.router.navigate(["/progress"]);
      });
    return false;
  }

  fetchFiles(): boolean {
    let sources: {[key:number]:string[]} = {};

    this.resolvableSongs.forEach(s => {
      s.sources.forEach(ss => {
        if (ss.download) {
          if (!sources[s.id]) {
            sources[s.id] = [];
          }
          sources[s.id].push(ss.sourceId);
        }
      })
    });

    this.rest.fetchSongs(sources)
      .switchMap(() => {
        return this.rest.getSongs("nashe");
      }).subscribe(res => {
      this.resolvableSongs = res.map(info => new ResolvableSong(info));
    });

    return false;
  }
}

import { Component, OnInit } from '@angular/core';
import { RestService } from '../../api/rest.service';
import { ResolvableSong } from './resolvable-song';
import { Router } from '@angular/router';

@Component({
  selector: 'app-sources-resolver',
  templateUrl: './sources-resolver.component.html'
})
export class SourcesResolverComponent implements OnInit {

  resolvableSongs: ResolvableSong[];
  p: number = 1;

  constructor(private rest: RestService,
              private router: Router) {}

  ngOnInit(): void {
    this.rest.getSongsForSearch()
      .subscribe(res => {
        this.resolvableSongs = res.map(info => new ResolvableSong(info));
      });
  }

  select(rs: ResolvableSong): boolean {
    rs.resolve = !rs.resolve;
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

  resolveSources(): boolean {
    let ids = this.resolvableSongs.filter(s => s.resolve)
      .map(s => {
        return s;
      })
      .map(s => s.id);
    this.rest.searchSongs(ids)
      .subscribe(() => {
        this.router.navigate(["/progress"]);
      });
    return false;
  }
}

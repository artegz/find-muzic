import { Component, OnInit } from '@angular/core';
import { RestService } from '../../api/rest.service';
import { Router } from '@angular/router';
import {IndexableArtist} from "./indexable-artist";

@Component({
  selector: 'app-artist-indexer',
  templateUrl: './artist-indexer.component.html',
  styleUrls: ['./artist-indexer.component.css']
})
export class ArtistIndexerComponent implements OnInit {

  indexableArtists: IndexableArtist[];
  p1: number = 1;
  p2: number = 1;

  constructor(private rest: RestService,
              private router: Router) {}

  ngOnInit(): void {
    this.rest.getArtistsForIndex()
      .subscribe(res => {
        this.indexableArtists = res.map(info => new IndexableArtist(info));
      });
  }

  includedArtists(): IndexableArtist[] {
    return this.indexableArtists.filter(value => value.index);
  }

  excludedArtists(): IndexableArtist[] {
    return this.indexableArtists.filter(value => !value.index);
  }

  select(rs: IndexableArtist): boolean {
    rs.index = !rs.index;
    return false;
  }

  selectAllForIndex(): boolean {
    this.indexableArtists.forEach(value => value.index = true);
    return false;
  }

  unselectAllForIndex(): boolean {
    this.indexableArtists.forEach(value => value.index = false);
    return false;
  }

  indexArtists(): boolean {
    let ids = this.indexableArtists.filter(s => s.index)
      .map(s => {
        return s.artistInfo.artist;
      })
      .map(s => s.artistId);
    this.rest.indexArtists(ids)
      .subscribe(() => {
        this.router.navigate(["/progress"]);
      });
    return false;
  }

}

import { Component, OnInit } from '@angular/core';
import { RestService } from '../../api/rest.service';
import { Router } from '@angular/router';
import {TorrentSongSource} from "../../api/dto/torrent-song-source";
import {TorrentDownloadInfoDto} from "../../api/dto/download/torrent-download-info";

@Component({
  selector: 'app-songs-downloader',
  templateUrl: './songs-downloader.component.html'
})
export class SongsDownloaderComponent implements OnInit{

  downloadableTorrents: TorrentDownloadInfoDto[];
  p: number = 1;

  constructor(private rest: RestService,
              private router: Router) {}

  ngOnInit(): void {
    this.rest.getTorrentsForDownload()
      .subscribe(res => {
        this.downloadableTorrents = res;
      });
  }

  selectTorrent(rs: TorrentDownloadInfoDto): boolean {
    rs.download = !rs.download;
    return false;
  }
  selectSource(rs: TorrentSongSource): boolean {
    rs.download = !rs.download;
    return false;
  }

  selectAllForDownload(): boolean {
    this.downloadableTorrents.forEach(value => {
      value.download = true;
      value.containedSources.forEach(value2 => value2.download = true);
    });
    return false;
  }
  unselectAllForDownload(): boolean {
    this.downloadableTorrents.forEach(value => {
      value.download = false;
      value.containedSources.forEach(value2 => value2.download = false);
    });
    return false;
  }

  fetchFiles(): boolean {
    let sources: {[key:string]:string[]} = {};

    this.downloadableTorrents.forEach(s => {
      if (s.download || this.isDownloadSources(s)) {
        if (!sources[s.torrentId]) {
          sources[s.torrentId] = [];
        }
        for (let src of s.containedSources) {
          if (src.download || s.download) {
            sources[s.torrentId].push(src.sourceId);
          }
        }
      }
    });

    this.rest.downloadTorrents(sources)
      .subscribe(() => {
        this.router.navigate(["/progress"]);
      });

    return false;
  }

  private isDownloadSources(s) {
    let d = false;
    for (let src of s.containedSources) {
      if (src.download) {
        d = true;
        break;
      }
    }
    return d;
  }
}

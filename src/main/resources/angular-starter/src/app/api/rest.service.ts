import { Injectable } from '@angular/core';
import { Headers, URLSearchParams, Http } from '@angular/http';
import { Observable } from 'rxjs';
import 'rxjs/add/operator/map';
import {ProgressInfoDto} from "./dto/progress/progress-info";
import {ArtistIndexInfoDto} from "./dto/index/artist-index-info";
import {SongResolveInfoDto} from "./dto/resolve/song-resolve-info";
import {TorrentDownloadInfoDto} from "./dto/download/torrent-download-info";
import {FileOutputInfoDto} from "./dto/output/file-output-info";

@Injectable()
export class RestService {

  constructor(private http: Http) {}

  /* ******************* Progress *********************** */

  getProgressInfo2(): Observable<ProgressInfoDto> {
    let url = 'rest/' + 'progresses2';
    let params = new URLSearchParams();
    let headers = new Headers();
    let res = this.http.get(
      url,
      {search: params, headers: headers}
    );
    return res.map(response => response.json());
  }


  /* ******************* Index artists *********************** */

  getArtistsForIndex(): Observable<ArtistIndexInfoDto[]> {
    let url = 'rest/' + 'alt/index/artists';

    let params = new URLSearchParams();
    let headers = new Headers();

    let res = this.http.get(
      url,
      {search: params, headers: headers}
    );
    return res.map(response => response.json());
  }
  indexArtists(artistIds: number[]): Observable<void> {
    let url = 'rest/' + 'alt/index/artists/start';

    let params = new URLSearchParams();
    let headers = new Headers();

    let res = this.http.post(
      url,
      artistIds,
      {search: params, headers: headers}
    );
    return res.map(response => response.json());
  }


  /* ******************* Search songs *********************** */

  getSongsForSearch(): Observable<SongResolveInfoDto[]> {
    let url = 'rest/' + 'alt/search/songs';

    let params = new URLSearchParams();
    let headers = new Headers();

    let res = this.http.get(
      url,
      {search: params, headers: headers}
    );
    return res.map(response => response.json());
  }
  searchSongs(songIds: number[]): Observable<void> {
    let url = 'rest/' + 'alt/search/songs/start';

    let params = new URLSearchParams();
    let headers = new Headers();

    let res = this.http.post(
      url,
      songIds,
      {search: params, headers: headers}
    );
    return res.map(response => response.json());
  }


  /* ******************* Download torrents *********************** */

  getTorrentsForDownload(): Observable<TorrentDownloadInfoDto[]> {
    let url = 'rest/' + 'alt/download/torrents';

    let params = new URLSearchParams();
    let headers = new Headers();

    let res = this.http.get(
      url,
      {search: params, headers: headers}
    );
    return res.map(response => response.json());
  }
  downloadTorrents(torrentSources: {[key:string]:string[]}): Observable<void> {
    let url = 'rest/' + 'alt/download/torrents/start';

    let params = new URLSearchParams();
    let headers = new Headers();

    let res = this.http.post(
      url,
      torrentSources,
      {search: params, headers: headers}
    );
    return res.map(response => response.json());
  }


  /* ******************* Prepare output *********************** */

  getDownloadedFiles(): Observable<FileOutputInfoDto[]> {
    let url = 'rest/' + 'alt/build/files';

    let params = new URLSearchParams();
    let headers = new Headers();

    let res = this.http.get(
      url,
      {search: params, headers: headers}
    );
    return res.map(response => response.json());
  }
  buildPlaylist(fileIds: number[]): Observable<void> {
    let url = 'rest/' + 'alt/build/files/start';

    let params = new URLSearchParams();
    let headers = new Headers();

    let res = this.http.post(
      url,
      fileIds,
      {search: params, headers: headers}
    );
    return res.map(response => response.json());
  }


}

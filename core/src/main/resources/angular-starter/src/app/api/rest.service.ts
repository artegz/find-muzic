import { Injectable } from '@angular/core';
import {Headers, URLSearchParams, Http} from '@angular/http';
import { Observable } from 'rxjs';
import 'rxjs/add/operator/map';
import {StatusEntity} from "./status-entity";
import {SongsSearchResult} from "./songs-search-result";
import {SongInfo} from "./dto/song-info";

@Injectable()
export class RestService {

  constructor(private http: Http) {}



  getSongs(playlistId: string): Observable<SongInfo[]> {
    let url = 'rest/' + 'alt/playlists/' + playlistId + '/songs';
    let params = new URLSearchParams();
    let headers = new Headers();
    let res = this.http.get(
      url,
      {search: params, headers: headers}
    );
    return res.map(response => response.json());
  }
  resolveSongs(songIds: number[]): Observable<void> {
    let url = 'rest/' + 'alt/songs/sources/resolve';

    let params = new URLSearchParams();
    let headers = new Headers();

    let res = this.http.post(
      url,
      songIds,
      {search: params, headers: headers}
    );
    return res.map(response => response.json());
  }
  fetchSongs(songSources: {[key: number]: string[]}): Observable<void> {
    let url = 'rest/' + 'alt/songs/sources/download';

    let params = new URLSearchParams();
    let headers = new Headers();

    let res = this.http.post(
      url,
      songSources,
      {search: params, headers: headers}
    );
    return res.map(response => response.json());
  }





  downloadDb(magnet: string): Observable<void>{
    let url = 'rest/' + 'torrentDbs/download';

    let params = new URLSearchParams();
    let headers = new Headers();

    let res = this.http.post(
      url,
      magnet,
      {search: params, headers: headers}
    );
    return res.map(response => response.json());
  }

  listTorrentDbs(): Observable<string[]> {
    let url = 'rest/' + 'torrentDbs';

    let params = new URLSearchParams();
    let headers = new Headers();

    let res = this.http.get(
      url,
      {search: params, headers: headers}
    );
    return res.map(response => response.json());
  }

  getArtists(): Observable<string[]> {
    let url = 'rest/' + 'artists';

    let params = new URLSearchParams();
    let headers = new Headers();

    let res = this.http.get(
      url,
      {search: params, headers: headers}
    );
    return res.map(response => response.json());
  }

  getFoundSongs(): Observable<SongsSearchResult> {
    let url = 'rest/' + 'results/songs';

    let params = new URLSearchParams();
    let headers = new Headers();

    let res = this.http.get(
      url,
      {search: params, headers: headers}
    );
    return res.map(response => response.json());
  }

  getStatuses(): Observable<StatusEntity[]> {
      let url = 'rest/' + 'results/statuses';

      let params = new URLSearchParams();
      let headers = new Headers();

      let res = this.http.get(
          url,
          {search: params, headers: headers}
      );
      return res.map(response => response.json());
  }

  // getProcessInstances(): Observable<ProcessInstance[]> {
  //   return this.getProcessInstancesImpl();
  // }
  //
  // private getProcessInstancesImpl(): Observable<ProcessInstance[]> {
  //   let url = 'http://kodkod:8080/glibs/api/engine/engine/default/process-instance';
  //
  //   let params = new URLSearchParams();
  //   // params.set('search', term); // the user's search value
  //   // params.set('action', 'opensearch');
  //   // params.set('format', 'json');
  //   // params.set('callback', 'JSONP_CALLBACK');
  //   params.set('maxResults', '10');
  //   params.set('firstResult', '0');
  //
  //   let headers = new Headers();
  //   // headers.set('tralalal', '12341234');
  //   // headers.set('PreAuthToken', 'TOKEN:asmirnov:707FA777ABAEDFBA49A044FBDA6701E5');
  //
  //   let res = this.http.get(
  //     url,
  //     {search: params, headers: headers}
  //   );
  //   return res.map(response => response.json());
  // }

}

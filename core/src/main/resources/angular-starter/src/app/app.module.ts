import { NgModule, ApplicationRef } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HttpModule } from '@angular/http';
import { FormsModule } from '@angular/forms';
import { DataTableModule } from "angular2-datatable";

import { AppComponent } from './app.component';
import { HomeComponent } from './home/home.component';
import { AboutComponent } from './about/about.component';
// import { ArtistsListComponent } from "./artists-list/artists-list.component";
import { MyTestComponent } from './test/test.component';
import { ApiService } from './shared';
import { routing } from './app.routing';

import { RestService } from "./api";

import { removeNgStyles, createNewHosts } from '@angularclass/hmr';
import {NgxPaginationModule} from "ngx-pagination/dist/ngx-pagination";
import {TorrentDbsComponent} from "./torrent-dbs/torrent-dbs.component";
import {MyStatusesComponent} from "./statuses/statuses.component";
import {MyFoundSongsComponent} from './songs/songs.component';
import {PlaylistManagerComponent} from "./alt/playlist-manager/playlist-manager.component";
import {ProgressInfoComponent} from "./alt/progress-info/progress-info.component";

@NgModule({
  imports: [
    BrowserModule,
    HttpModule,
    FormsModule,
    routing,
    DataTableModule, NgxPaginationModule
  ],
  declarations: [
    AppComponent,
    HomeComponent,
    AboutComponent,
    // ArtistsListComponent,
    MyTestComponent,
    TorrentDbsComponent,
    MyStatusesComponent,
    MyFoundSongsComponent,
    PlaylistManagerComponent,
    ProgressInfoComponent
  ],
  providers: [
    ApiService,
    RestService
  ],
  bootstrap: [AppComponent]
})
export class AppModule {
  constructor(public appRef: ApplicationRef) {}
  hmrOnInit(store) {
    console.log('HMR store', store);
  }
  hmrOnDestroy(store) {
    let cmpLocation = this.appRef.components.map(cmp => cmp.location.nativeElement);
    // recreate elements
    store.disposeOldHosts = createNewHosts(cmpLocation);
    // remove styles
    removeNgStyles();
  }
  hmrAfterDestroy(store) {
    // display new elements
    store.disposeOldHosts();
    delete store.disposeOldHosts;
  }
}

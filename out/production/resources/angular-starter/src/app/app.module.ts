import { NgModule, ApplicationRef } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HttpModule } from '@angular/http';
import { FormsModule } from '@angular/forms';
import { DataTableModule } from 'angular2-datatable';

import { AppComponent } from './app.component';
import { HomeComponent } from './home/home.component';
import { routing } from './app.routing';

import { RestService } from './api';

import { removeNgStyles, createNewHosts } from '@angularclass/hmr';
import { NgxPaginationModule } from 'ngx-pagination/dist/ngx-pagination';
import { ProgressInfoComponent } from './fm/progress-info/progress-info.component';
import { SourcesResolverComponent } from './fm/sources-resolver/sources-resolver.component';
import { SongsDownloaderComponent } from './fm/songs-downloader/songs-downloader.component';
import { PlaylistBuilderComponent } from './fm/playlist-builder/playlist-builder.component';
import { ArtistIndexerComponent } from './fm/artist-indexer/artist-indexer.component';

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

    ProgressInfoComponent,
    ArtistIndexerComponent,
    SourcesResolverComponent,
    SongsDownloaderComponent,
    PlaylistBuilderComponent
  ],
  providers: [
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

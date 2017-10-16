import { RouterModule, Routes } from '@angular/router';

import { HomeComponent } from './home/home.component';
import { ProgressInfoComponent  } from './fm/progress-info/progress-info.component';
import { SourcesResolverComponent } from './fm/sources-resolver/sources-resolver.component';
import { SongsDownloaderComponent } from './fm/songs-downloader/songs-downloader.component';
import { PlaylistBuilderComponent } from './fm/playlist-builder/playlist-builder.component';
import { ArtistIndexerComponent } from './fm/artist-indexer/artist-indexer.component';
// import { ArtistsListComponent } from './artists-list/artists-list.component';

const routes: Routes = [
  { path: '', component: HomeComponent },

  { path: 'progress', component: ProgressInfoComponent},
  { path: 'artist-indexer', component: ArtistIndexerComponent},
  { path: 'sources-resolver', component: SourcesResolverComponent},
  { path: 'songs-downloader', component: SongsDownloaderComponent},
  { path: 'playlist-builder', component: PlaylistBuilderComponent}
];

export const routing = RouterModule.forRoot(routes);

import { RouterModule, Routes } from '@angular/router';

import { HomeComponent } from './home/home.component';
import { AboutComponent } from './about/about.component';
import { MyTestComponent  } from './test/test.component';
import { TorrentDbsComponent  } from './torrent-dbs/torrent-dbs.component';
import { MyStatusesComponent  } from './statuses/statuses.component';
import { MyFoundSongsComponent  } from './songs/songs.component';
import { PlaylistManagerComponent } from './alt/playlist-manager/playlist-manager.component';
import { ProgressInfoComponent  } from './alt/progress-info/progress-info.component';
import { SourcesResolverComponent } from './alt/sources-resolver/sources-resolver.component';
import { SongsDownloaderComponent } from './alt/songs-downloader/songs-downloader.component';
import { PlaylistBuilderComponent } from './alt/playlist-builder/playlist-builder.component';
import { ArtistIndexerComponent } from './alt/artist-indexer/artist-indexer.component';
// import { ArtistsListComponent } from './artists-list/artists-list.component';

const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'about', component: AboutComponent},
  // { path: 'artists', component: ArtistsListComponent},
  { path: 'test', component: MyTestComponent},
  { path: 'torrentDbs', component: TorrentDbsComponent},
  { path: 'statuses', component: MyStatusesComponent},
  { path: 'foundSongs', component: MyFoundSongsComponent},
  { path: 'playlistManager', component: PlaylistManagerComponent},

  { path: 'progress', component: ProgressInfoComponent},
  { path: 'artist-indexer', component: ArtistIndexerComponent},
  { path: 'sources-resolver', component: SourcesResolverComponent},
  { path: 'songs-downloader', component: SongsDownloaderComponent},
  { path: 'playlist-builder', component: PlaylistBuilderComponent}
];

export const routing = RouterModule.forRoot(routes);

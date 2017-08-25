import { RouterModule, Routes } from '@angular/router';

import { HomeComponent } from './home/home.component';
import { AboutComponent } from './about/about.component';
import {MyTestComponent} from "./test/test.component";
import {TorrentDbsComponent} from "./torrent-dbs/torrent-dbs.component";
import {MyStatusesComponent} from "./statuses/statuses.component";
// import { ArtistsListComponent } from "./artists-list/artists-list.component";

const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'about', component: AboutComponent},
  // { path: 'artists', component: ArtistsListComponent},
  { path: 'test', component: MyTestComponent},
  { path: 'torrentDbs', component: TorrentDbsComponent},
  { path: 'statuses', component: MyStatusesComponent}
];

export const routing = RouterModule.forRoot(routes);

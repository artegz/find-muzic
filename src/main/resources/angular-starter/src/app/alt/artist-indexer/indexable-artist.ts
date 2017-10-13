import {LiteArtistInfo} from "../../api/lite-artist-info";

export class IndexableArtist {
  artistInfo: LiteArtistInfo;
  index = false;

  constructor(artistInfo: LiteArtistInfo) {
    this.artistInfo = artistInfo;
  }

  getArtistName() {
    return this.artistInfo.artist.artistName;
  }
}

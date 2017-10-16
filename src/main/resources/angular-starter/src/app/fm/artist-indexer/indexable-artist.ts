
import {ArtistIndexInfoDto} from "../../api/dto/index/artist-index-info";

export class IndexableArtist {
  artistInfo: ArtistIndexInfoDto;
  index = false;

  constructor(artistInfo: ArtistIndexInfoDto) {
    this.artistInfo = artistInfo;
  }

  getArtistName() {
    return this.artistInfo.artist.artistName;
  }
}

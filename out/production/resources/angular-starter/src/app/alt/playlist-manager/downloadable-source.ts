import { Mp3TorrentSongSource } from '../../api/dto/mp3-torrent-song-source';

export class DownloadableSource {

  source: Mp3TorrentSongSource;

  download = false;

  constructor(source: Mp3TorrentSongSource) {
    this.source = source;
  }

  get sourceId() {
    return this.source.sourceId;
  }

}

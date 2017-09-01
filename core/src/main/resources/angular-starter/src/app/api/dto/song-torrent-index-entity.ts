export class SongTorrentIndexEntity {

  id: string;
  torrentId: string;
  artistName: string;
  songName: string;
  type: string; //flac or mp3
  mp3FileName: string;
  mp3FilePath: string;
  cueFileName: string;
  cueFilePath: string;
  trackNum: string;
  indexTime: string;

}

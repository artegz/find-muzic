export class SongResolveReport {
  songId: number;
  artistId: number;

  startTime: Date;
  endTime: Date;

  resolvePerformed: boolean = false;
  resolveStatus: string;
  resolveFailureReason: string = null;
  resolvedTorrentIds: string[];

  indexingPerformed: boolean = false;
  torrentsIndexingStatuses: {[key: string]: string} = null;

  searchPerformed: boolean;
  foundSources: string[];
}

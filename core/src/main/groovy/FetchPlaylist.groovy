import edu.fm.Context
import edu.fm.FetchPlaylistRunner
import edu.fm.playlist.SiteMoreradioOrgPlaylistProvider

String station = "nashe"
def runner = new FetchPlaylistRunner()

Context.get().playlistProvider = new SiteMoreradioOrgPlaylistProvider()

runner.argDateFrom = "01.01.2016"
runner.argDateTo = "30.09.2016"

runner.argPath = "C:/TEMP/find-music/"
runner.station = station
runner.outputFilename = "nashe-01012016-30092016.${station}.json"

runner.run()


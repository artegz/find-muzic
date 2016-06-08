import edu.fm.Context
import edu.fm.FetchPlaylistRunner
import edu.fm.playlist.SiteHotChartsRuProvider

String station = "nashe"
def runner = new FetchPlaylistRunner()

Context.get().playlistProvider = new SiteHotChartsRuProvider(SiteHotChartsRuProvider.ListType.top)

runner.argDateFrom = "01.10.2015"
runner.argDateTo = "31.10.2015"
runner.argPath = "C:/TEMP/find-music/"
runner.station = station
runner.outputFilename = "chart-2015.${station}.txt"

runner.run()


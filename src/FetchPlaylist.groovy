import edu.fm.FetchPlaylistRunner

String station = "nashe"
def runner = new FetchPlaylistRunner()

runner.argDateFrom = "01.10.2015"
runner.argDateTo = "31.10.2015"
runner.argPath = "C:/TEMP/mdl-7bx/"
runner.station = station
runner.outputFilename = "october-playlist.${station}.txt"

runner.run()


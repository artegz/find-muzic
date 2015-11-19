import edu.fm.Context
import edu.fm.FindMuzicRunner

/**
 * User: artem.smirnov
 * Date: 23.09.2015
 * Time: 18:06
 */

String station = "nashe"
def runner = new FindMuzicRunner()

runner.argPath = "C:/TEMP/mdl-7bx/"

runner.loadListFilename = "playlist.${station}.txt"
runner.failedListFilename = "failed.${station}.txt"
runner.succeededListFilename = "succeeded.${station}.txt"
runner.mappingListFilename = "mapping.${station}.txt"
runner.resultsSubDir = "songs.${station}"

runner.excludeAlreadyExist = true
runner.excludeFailed = false
runner.excludeSucceeded = true

runner.songsOffset = 0
runner.songsCount = 1000

Context.get().distinctionEstimator.ignoreParentheses = true
Context.get().distinctionEstimator.maxDiffFactor = 5

//DistinctionEstimator.maxDiffFactor = 99
// todo: see http://hotcharts.ru


runner.run()
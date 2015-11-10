package edu.fm

@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.7')
@Grab(group = 'org.apache.httpcomponents', module = 'httpclient', version = '4.5')
@Grab(group='org.jsoup', module='jsoup', version='1.8.3')
@Grab(group='com.fasterxml.jackson.core', module='jackson-databind', version='2.6.2')
@Grab(group = 'commons-io', module = 'commons-io', version = '2.4')

/**
 * User: artem.smirnov
 * Date: 23.09.2015
 * Time: 18:06
 */

def argPath = "C:/TEMP/mdl-7bx/"

def loadListFilename = 'playlist.rock.txt'
def failedListFilename = 'failed.rock.txt'
def succeededListFilename = 'succeeded.rock.txt'
def mappingListFilename = 'mapping.rock.txt'
def resultsSubDir = 'songs.rock'

Integer songsOffset = 200
Integer songsCount = 100

//DistinctionEstimator.maxDiffFactor = 99

File workDir = FileTools.getDir(argPath)
File resultFolder = FileTools.getSubDir(workDir, resultsSubDir)

List<String> songNames = FileTools.readSongs(workDir, loadListFilename)
List<String> loadedSongs = FileTools.getLoadedSongs(resultFolder)

List<String> failedSongNames = FileTools.readSongs(workDir, failedListFilename)
List<String> succeededSongNames = FileTools.readSongs(workDir, succeededListFilename)

List<String> subList = getSubList(songNames, songsOffset, songsCount)
List<String> songsToLoad = subList
songsToLoad = filterAlreadyLoaded(songsToLoad, loadedSongs)
songsToLoad = filterSongs(songsToLoad, failedSongNames)
songsToLoad = filterSongs(songsToLoad, succeededSongNames)

if (subList.size() != songsToLoad.size()) {
    println "${subList.size() - songsToLoad.size()}/${subList.size()} already exist and would not be loaded"
}

int counter = songsOffset
int failureCounter = 0


def failed = new ArrayList<String>()
def succeeded = new ArrayList<String>()

def mapping = new HashMap<String, String>()

try {
    songsToLoad.each {

        print "[${counter++}] '${it}'... "
        try {
            def song = Site7bxRuProvider.fetchSong(it)
            DownloadTools.downloadFile(song.foundSongName, song.downloadUrl, resultFolder)

            mapping.put(it, song.foundSongName)
            succeeded.add(it)
            print "SUCCEED"
            failureCounter = 0
            sleep(1000)
        } catch (Throwable e) {
            failed.add(it)
            print "${e.message}... FAILED"
            failureCounter++
            if (failureCounter > 40) {
                failed.addAll(songsToLoad.subList(songsToLoad.indexOf(it), songsToLoad.size()))
                throw e
            }
        }
        println()

    }
} finally {
    println("")
    println("SUMMARY ")
    println("SUCCEEDED (${succeeded.size()}): ")
    succeeded.each { s ->
        println(s)
    }
    println("FAILED (${failed.size()}): ")
    failed.each { f ->
        println(f)
    }
}

FileTools.writeSongs(workDir, failed, failedListFilename, false)
FileTools.writeSongs(workDir, succeeded, succeededListFilename, false)
FileTools.writeMapping(workDir, mapping, mappingListFilename, false)



private ArrayList<String> filterSongs(List<String> subList, List<String> songs) {
    List<String> list = new ArrayList<>()
    for (String songName : subList) {
        if (!songs.contains(songName)) {
            list.add(songName)
        }
    }
    list
}

private ArrayList<String> filterAlreadyLoaded(List<String> subList, List<String> loadedSongs) {
    List<String> list = new ArrayList<>()
    for (String songName : subList) {
        if (!DistinctionEstimator.containsExact(loadedSongs, songName, { it })) {
            list.add(songName)
        }
    }
    list
}

private List<String> getSubList(Collection<String> songNames, int songsOffset, int songsCount) {
    List<String> songsToLoad
    if (songsOffset != null) {
        def count = (songsCount != null) ? songsCount : songNames.size() - songsOffset
        songsToLoad = new ArrayList<String>(songNames).subList(songsOffset, songsOffset + count)
    } else {
        songsToLoad = new ArrayList<String>(songNames)
    }
    songsToLoad
}

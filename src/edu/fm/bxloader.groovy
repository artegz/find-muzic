package edu.fm

@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.7')
@Grab(group = 'org.apache.httpcomponents', module = 'httpclient', version = '4.5')
@Grab(group='org.jsoup', module='jsoup', version='1.8.3')
@Grab(group='com.fasterxml.jackson.core', module='jackson-databind', version='2.6.2')

/**
 * User: artem.smirnov
 * Date: 23.09.2015
 * Time: 18:06
 */

def argPath = "C:/TEMP/mdl-7bx/"

def loadListFilename = 'playlist.rock.txt'
def failedListFilename = 'failed.rock.txt'

Integer songsOffset = 0
Integer songsCount = 10

File workDir = FileTools.getDir(argPath)
File resultFolder = FileTools.getSubDir(workDir, 'songs')

List<String> songNames = FileTools.readSongs(workDir, loadListFilename)

List<String> songsToLoad = getSubList(songNames, songsOffset, songsCount)


int counter = songsOffset
int failureCounter = 0


def failed = new ArrayList<String>()
def succeeded = new ArrayList<String>()

try {
    songsToLoad.each {

        print "[${counter++}] '${it}'... "
        try {
            def song = Site7bxRuProvider.fetchSong(it)
            DownloadTools.downloadFile(song.foundSongName, song.downloadUrl, resultFolder)

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

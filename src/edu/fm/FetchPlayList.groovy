package edu.fm

import java.text.SimpleDateFormat

@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.7')
@Grab(group = 'org.apache.httpcomponents', module = 'httpclient', version = '4.5')

/**
 * User: artem.smirnov
 * Date: 23.09.2015
 * Time: 18:06
 */

def argDateFrom = "01.10.2015"
def argDateTo = "31.10.2015"
def argPath = "C:/TEMP/mdl-7bx/"
String station = "nashe"

Date dateFrom = new SimpleDateFormat("dd.MM.yyyy").parse(argDateFrom)
Date dateTo = new SimpleDateFormat("dd.MM.yyyy").parse(argDateTo)

File workDir = FileTools.getDir(argPath)

//def fetchedSongs = new SiteMoreradioRuPlaylistFetch().fetchSongs(dateFrom, dateTo, station)
def fetchedSongs = new Site7bxRuPlaylistProvider().fetchSongs(dateFrom, dateTo, station)

FileTools.writeSongs(workDir, fetchedSongs, "september-1.nashe.playlist.txt", true)


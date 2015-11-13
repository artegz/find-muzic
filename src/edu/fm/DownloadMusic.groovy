package edu.fm

@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.7')
@Grab(group = 'org.apache.httpcomponents', module = 'httpclient', version = '4.5')
@Grab(group='org.jsoup', module='jsoup', version='1.8.3')
@Grab(group='com.fasterxml.jackson.core', module='jackson-databind', version='2.6.2')
@Grab(group = 'commons-io', module = 'commons-io', version = '2.4')
@Grab(group='ch.qos.logback', module='logback-classic', version='1.0.13')

/**
 * User: artem.smirnov
 * Date: 23.09.2015
 * Time: 18:06
 */

//DistinctionEstimator.maxDiffFactor = 99

def runner = new FindMuzicRunner()

runner.argPath = "C:/TEMP/mdl-7bx/"

runner.loadListFilename = 'playlist.rock.txt'
runner.failedListFilename = 'failed.rock.txt'
runner.succeededListFilename = 'succeeded.rock.txt'
runner.mappingListFilename = 'mapping.rock.txt'
runner.resultsSubDir = 'songs.rock'

runner.excludeAlreadyExist = true
runner.excludeFailed = false
runner.excludeSucceeded = true

runner.songsOffset = 1000
runner.songsCount = 10

DistinctionEstimator.ignoreParentheses = false
DistinctionEstimator.maxDiffFactor = 10

runner.run()
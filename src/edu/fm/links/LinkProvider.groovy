package edu.fm.links

/**
 * User: artem.smirnov
 * Date: 19.11.2015
 * Time: 10:33
 */
interface LinkProvider {

    LinkContainer fetchLink(String songName)
}
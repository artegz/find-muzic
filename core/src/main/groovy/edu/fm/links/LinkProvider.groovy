package edu.fm.links

import edu.fm.SongDescriptor

/**
 * User: artem.smirnov
 * Date: 19.11.2015
 * Time: 10:33
 */
interface LinkProvider {

    LinkContainer fetchLink(SongDescriptor song)
}
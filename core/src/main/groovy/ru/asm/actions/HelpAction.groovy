package ru.asm.actions

/**
 * User: artem.smirnov
 * Date: 16.06.2016
 * Time: 10:16
 */
class HelpAction implements UserAction {

    @Override
    DispatchResult proceedAction(String[] params) {
        println """Commands:
    help - show this help
    exit - exit program
    countall - total number of records in repo
    find <mainCategory> <subCategory> <folders> <title> <page> - find torrent by name and category
    findr <mainCategory> <subCategory> <folders> <title> <page> - find torrent by name and category
    csvdb <file> <playlist> - import songs from csv to db
    dbselect <command>
                    """
        DispatchResult.SUCCESSFUL
    }
}

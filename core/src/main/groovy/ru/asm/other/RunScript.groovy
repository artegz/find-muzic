package ru.asm.other

/**
 * User: artem.smirnov
 * Date: 22.06.2016
 * Time: 9:58
 */

final ConsoleModeApp app = new ConsoleModeApp()
app.init()

List<String> list = [
    "db sql 'select * FROM PLAYLIST_SONGS'",
    "exit"
]

list.each { line ->
    app.proceedCommand(line)
}
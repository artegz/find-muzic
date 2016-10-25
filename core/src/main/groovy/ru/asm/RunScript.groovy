package ru.asm

/**
 * User: artem.smirnov
 * Date: 22.06.2016
 * Time: 9:58
 */

final ConsoleModeApp app = new ConsoleModeApp()
app.init()

List<String> list = [
    "db sql 'select * from songs'",
    "exit"
]

list.each { line ->
    app.proceedCommand(line)
}
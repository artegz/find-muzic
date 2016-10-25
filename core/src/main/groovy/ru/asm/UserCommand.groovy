package ru.asm

/**
 * User: artem.smirnov
 * Date: 16.06.2016
 * Time: 10:12
 */
class UserCommand {

    private String command;

    private String[] params;

    UserCommand(String command, String[] params) {
        this.command = command
        this.params = params
    }

    String getCommand() {
        return command
    }

    String[] getParams() {
        return params
    }
}

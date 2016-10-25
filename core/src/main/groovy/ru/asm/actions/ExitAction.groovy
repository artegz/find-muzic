package ru.asm.actions

/**
 * User: artem.smirnov
 * Date: 16.06.2016
 * Time: 10:15
 */
class ExitAction implements UserAction {

    @Override
    DispatchResult proceedAction(String[] params) {
        return DispatchResult.EXIT
    }
}

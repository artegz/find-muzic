package ru.asm.actions

import org.springframework.context.ApplicationContext
/**
 * User: artem.smirnov
 * Date: 16.06.2016
 * Time: 16:50
 */
class FindAction implements UserAction {

    private ApplicationContext applicationContext

    private boolean regexp = false

    FindAction(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext
    }

    void setRegexp(boolean regexp) {
        this.regexp = regexp
    }

    @Override
    DispatchResult proceedAction(String[] params) {


        return DispatchResult.SUCCESSFUL
    }
}

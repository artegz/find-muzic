package ru.asm.actions

import org.springframework.context.ApplicationContext
/**
 * User: artem.smirnov
 * Date: 16.06.2016
 * Time: 10:18
 */
class DbSelectAction implements UserAction {

    private ApplicationContext applicationContext

    DbSelectAction(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext
    }

    @Override
    DispatchResult proceedAction(String[] params) {

    }
}

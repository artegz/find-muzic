package ru.asm.tools.dev;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.asm.core.persistence.DataStorageService;

/**
 * User: artem.smirnov
 * Date: 16.03.2017
 * Time: 16:30
 */
public class ExecuteSelectSqlTool {

    public static void main(String[] args) {
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext("ru.asm.core.persistence");
        final DataStorageService dataStorageService = applicationContext.getBean("dataStorageService", DataStorageService.class);

        dataStorageService.executeSql("select * FROM PLAYLIST_SONGS");
    }
}

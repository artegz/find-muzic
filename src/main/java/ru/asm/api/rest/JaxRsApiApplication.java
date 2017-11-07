package ru.asm.api.rest;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/**
 * User: artem.smirnov
 * Date: 15.07.2016
 * Time: 17:22
 */
@ApplicationPath("/")
public class JaxRsApiApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        final HashSet<Class<?>> classes = new HashSet<>(super.getClasses());
        classes.add(CORSFilter.class);
        return (classes);
    }

}

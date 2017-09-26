package ru.asm.api.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * User: artem.smirnov
 * Date: 20.07.2016
 * Time: 11:35
 */
public class JsonProviderFactoryBean implements FactoryBean<JacksonJaxbJsonProvider> {
    @Override
    public JacksonJaxbJsonProvider getObject() throws Exception {
        return getJsonProvider();
    }

    @Override
    public Class<?> getObjectType() {
        return JacksonJaxbJsonProvider.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    private JacksonJaxbJsonProvider getJsonProvider() {
        return new JacksonJaxbJsonProvider(objectMapper, JacksonJaxbJsonProvider.DEFAULT_ANNOTATIONS);
    }

    @Autowired
    private ObjectMapper objectMapper;
}

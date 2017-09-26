package ru.asm.api;



import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.springframework.context.annotation.*;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import ru.asm.api.rest.AppRestService;
import ru.asm.api.rest.JaxRsApiApplication;

import javax.ws.rs.ext.RuntimeDelegate;
import java.util.Arrays;

/**
 * User: artem.smirnov
 * Date: 15.07.2016
 * Time: 17:15
 */
@Configuration
@ComponentScan(
        basePackages = "ru.asm.core"
)
@EnableTransactionManagement
public class AppConfig {

    @Bean(destroyMethod = "shutdown")
    public SpringBus cxf() {
        return new SpringBus();
    }

    @Bean
    @DependsOn("cxf")
    public Server jaxRsServer() {
        JAXRSServerFactoryBean factory = RuntimeDelegate.getInstance().createEndpoint(jaxRsApiApplication(), JAXRSServerFactoryBean.class);
        factory.setServiceBeans(Arrays.<Object>asList(appRestService()));
        factory.setAddress(factory.getAddress());
        factory.setProviders(Arrays.asList(jsonProvider()));

        return factory.create();
    }

    @Bean
    public JaxRsApiApplication jaxRsApiApplication() {
        return new JaxRsApiApplication();
    }

    @Bean
    public AppRestService appRestService() {
        return new AppRestService();
    }

    @Bean
    public JacksonJsonProvider jsonProvider() {
        return new JacksonJsonProvider();
    }

}

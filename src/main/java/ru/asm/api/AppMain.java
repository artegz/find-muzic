package ru.asm.api;

import org.apache.cxf.transport.servlet.CXFServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

/**
 * User: artem.smirnov
 * Date: 22.08.2017
 * Time: 17:44
 */
public class AppMain {

    public static void main(String[] args) throws Exception {
        Server server = new Server(8081);

        // Register and map the dispatcher servlet
        final ServletContextHandler servletContextHandler = new ServletContextHandler();
        servletContextHandler.setContextPath("/rest");
        servletContextHandler.addServlet(new ServletHolder(new CXFServlet()), "/*");
        servletContextHandler.addEventListener(new ContextLoaderListener());
        servletContextHandler.setInitParameter("contextClass", AnnotationConfigWebApplicationContext.class.getName());
        servletContextHandler.setInitParameter("contextConfigLocation", AppConfig.class.getName());

        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setDirectoriesListed(true);
        resourceHandler.setWelcomeFiles(new String[] { "index.html" });
//        resourceHandler.setResourceBase("./out/production/core/angular-starter/dist");
        resourceHandler.setResourceBase("C:\\IdeaProjects\\find-muzic\\src\\main\\resources\\angular-starter\\dist");


        ContextHandler staticContextHandler = new ContextHandler();
        staticContextHandler.setContextPath("/*");
        staticContextHandler.setHandler(resourceHandler);

        final HandlerCollection handlerCollection = new HandlerCollection();
        handlerCollection.addHandler(staticContextHandler);
        handlerCollection.addHandler(servletContextHandler);

        server.setHandler(handlerCollection);
//        server.setHandler(staticContextHandler);
        try {
            server.start();
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

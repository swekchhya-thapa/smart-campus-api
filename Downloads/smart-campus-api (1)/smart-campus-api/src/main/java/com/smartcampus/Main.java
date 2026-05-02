package com.smartcampus;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.net.URI;
import java.util.logging.Logger;

public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    // The address the server will run on
    public static final String BASE_URI = "http://0.0.0.0:8080/api/v1/";

    public static void main(String[] args) throws Exception {

        // Tell Jersey to scan the com.smartcampus package for all
        // resource classes, filters, and exception mappers
        ResourceConfig config = new ResourceConfig().packages("com.smartcampus");

        // Start the Grizzly server
        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), config);

        LOGGER.info("====================================");
        LOGGER.info("Smart Campus API is running!");
        LOGGER.info("Visit: http://localhost:8080/api/v1");
        LOGGER.info("Press ENTER to stop the server.");
        LOGGER.info("====================================");

        System.in.read(); // wait for Enter key

        server.shutdownNow();
        LOGGER.info("Server stopped.");
    }
}
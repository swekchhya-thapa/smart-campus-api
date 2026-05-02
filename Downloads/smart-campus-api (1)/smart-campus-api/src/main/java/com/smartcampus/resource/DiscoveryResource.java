package com.smartcampus.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.HashMap;
import java.util.Map;

// This class handles requests to /api/v1
// (the /api/v1 part comes from @ApplicationPath in SmartCampusApplication)
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Response discover() {

        // Build the response as a Map - Jackson will convert it to JSON automatically
        Map<String, Object> response = new HashMap<>();
        response.put("api", "Smart Campus Sensor & Room Management API");
        response.put("version", "1.0");
        response.put("contact", "admin@smartcampus.ac.uk");
        response.put("description", "RESTful API for managing campus rooms, sensors and readings.");

        // Links to the main resources (this is the HATEOAS part)
        Map<String, String> links = new HashMap<>();
        links.put("rooms", "/api/v1/rooms");
        links.put("sensors", "/api/v1/sensors");
        response.put("resources", links);

        return Response.ok(response).build();
    }
}
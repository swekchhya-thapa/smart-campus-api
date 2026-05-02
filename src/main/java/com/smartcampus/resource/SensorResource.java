package com.smartcampus.resource;

import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Sensor;
import com.smartcampus.storage.DataStore;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.*;
import java.util.stream.Collectors;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    // ----------------------------------------------------------------
    // GET /api/v1/sensors
    // GET /api/v1/sensors?type=CO2
    // Returns all sensors, or filtered by type if ?type= is provided
    // ----------------------------------------------------------------
    @GET
    public Response getAllSensors(@QueryParam("type") String type) {

        Collection<Sensor> all = DataStore.sensors.values();
        List<Sensor> result;

        if (type != null && !type.isBlank()) {
            // Filter the list to only sensors matching the type
            result = all.stream()
                    .filter(s -> s.getType().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
        } else {
            // No filter - return everything
            result = new ArrayList<>(all);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("count", result.size());
        response.put("sensors", result);

        // If a filter was used, include it in the response
        if (type != null) {
            response.put("filteredByType", type);
        }

        return Response.ok(response).build();
    }

    // ----------------------------------------------------------------
    // POST /api/v1/sensors
    // Registers a new sensor
    // IMPORTANT: validates that the roomId actually exists
    // ----------------------------------------------------------------
    @POST
    public Response createSensor(Sensor sensor) {

        // Check sensor ID was provided
        if (sensor == null || sensor.getId() == null || sensor.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorBody("Sensor ID is required."))
                    .build();
        }

        // Check roomId was provided
        if (sensor.getRoomId() == null || sensor.getRoomId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorBody("roomId is required."))
                    .build();
        }

        // Check the room actually exists - if not throw 422
        if (!DataStore.rooms.containsKey(sensor.getRoomId())) {
            throw new LinkedResourceNotFoundException(
                "Cannot register sensor: room '" + sensor.getRoomId() +
                "' does not exist. Please create the room first."
            );
        }

        // Check sensor ID isn't already taken
        if (DataStore.sensors.containsKey(sensor.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(errorBody("Sensor '" + sensor.getId() + "' already exists."))
                    .build();
        }

        // Default status to ACTIVE if not provided
        if (sensor.getStatus() == null || sensor.getStatus().isBlank()) {
            sensor.setStatus("ACTIVE");
        }

        // Save the sensor
        DataStore.sensors.put(sensor.getId(), sensor);

        // Create an empty readings list for this sensor
        DataStore.readings.put(sensor.getId(), new ArrayList<>());

        // Link the sensor to its room
        DataStore.rooms.get(sensor.getRoomId()).getSensorIds().add(sensor.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Sensor registered successfully.");
        response.put("sensor", sensor);

        return Response.status(Response.Status.CREATED)
                .entity(response)
                .build();
    }

    // ----------------------------------------------------------------
    // GET /api/v1/sensors/{sensorId}
    // Returns one specific sensor
    // ----------------------------------------------------------------
    @GET
    @Path("/{sensorId}")
    public Response getSensorById(@PathParam("sensorId") String sensorId) {

        Sensor sensor = DataStore.sensors.get(sensorId);

        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorBody("Sensor '" + sensorId + "' not found."))
                    .build();
        }

        return Response.ok(sensor).build();
    }

    // ----------------------------------------------------------------
    // DELETE /api/v1/sensors/{sensorId}
    // Removes a sensor and unlinks it from its room
    // ----------------------------------------------------------------
    @DELETE
    @Path("/{sensorId}")
    public Response deleteSensor(@PathParam("sensorId") String sensorId) {

        Sensor sensor = DataStore.sensors.get(sensorId);

        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorBody("Sensor '" + sensorId + "' not found."))
                    .build();
        }

        // Remove the sensor from its room's sensorIds list
        if (sensor.getRoomId() != null && DataStore.rooms.containsKey(sensor.getRoomId())) {
            DataStore.rooms.get(sensor.getRoomId()).getSensorIds().remove(sensorId);
        }

        // Remove the sensor and its readings
        DataStore.sensors.remove(sensorId);
        DataStore.readings.remove(sensorId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Sensor '" + sensorId + "' has been removed.");

        return Response.ok(response).build();
    }

    // ----------------------------------------------------------------
    // Sub-Resource Locator for /api/v1/sensors/{sensorId}/readings
    // No HTTP verb annotation - JAX-RS knows this is a locator
    // It delegates to SensorReadingResource for anything after /readings
    // ----------------------------------------------------------------
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingsResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }

    // ----------------------------------------------------------------
    // Helper - builds a standard error body
    // ----------------------------------------------------------------
    private Map<String, Object> errorBody(String message) {
        Map<String, Object> err = new HashMap<>();
        err.put("error", message);
        err.put("timestamp", System.currentTimeMillis());
        return err;
    }
}
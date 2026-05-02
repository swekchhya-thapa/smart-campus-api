package com.smartcampus.resource;

import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import com.smartcampus.storage.DataStore;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.*;

/**
 * Sub-resource for sensor readings — handles /api/v1/sensors/{sensorId}/readings
 *
 * Instantiated by SensorResource's sub-resource locator (not by JAX-RS directly),
 * so it does NOT carry a class-level @Path annotation.
 *
 * Responsibilities:
 *   GET /  — retrieve all historical readings for the sensor
 *   POST / — record a new reading; updates parent sensor's currentValue as a side effect
 *            Blocked if sensor status is "MAINTENANCE" (throws SensorUnavailableException → 403)
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    /**
     * GET /api/v1/sensors/{sensorId}/readings
     * Returns all recorded readings for the sensor, ordered newest-first.
     */
    @GET
    public Response getReadings() {
        Sensor sensor = DataStore.sensors.get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorBody("Sensor '" + sensorId + "' not found."))
                    .build();
        }
        List<SensorReading> history = DataStore.readings.getOrDefault(sensorId, new ArrayList<>());
        // Return newest first
        List<SensorReading> sorted = new ArrayList<>(history);
        sorted.sort(Comparator.comparingLong(SensorReading::getTimestamp).reversed());

        Map<String, Object> response = new HashMap<>();
        response.put("sensorId", sensorId);
        response.put("count", sorted.size());
        response.put("readings", sorted);
        return Response.ok(response).build();
    }

    /**
     * POST /api/v1/sensors/{sensorId}/readings
     *
     * Appends a new reading. Throws SensorUnavailableException (→ 403) if the
     * sensor is in MAINTENANCE status — it cannot physically accept readings.
     *
     * Side effect: Updates the parent Sensor's currentValue to the new reading's value,
     * ensuring data consistency across the API.
     */
    @POST
    public Response addReading(SensorReading reading) {
        Sensor sensor = DataStore.sensors.get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorBody("Sensor '" + sensorId + "' not found."))
                    .build();
        }
        // State constraint: block MAINTENANCE sensors
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(
                    "Sensor '" + sensorId + "' is currently in MAINTENANCE mode and cannot accept new readings."
            );
        }
        if (reading == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorBody("Reading body is required."))
                    .build();
        }
        // Ensure ID and timestamp are set
        if (reading.getId() == null || reading.getId().isBlank()) {
            reading.setId(java.util.UUID.randomUUID().toString());
        }
        if (reading.getTimestamp() == 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        // Persist the reading
        DataStore.readings.computeIfAbsent(sensorId, k -> new ArrayList<>()).add(reading);

        // Side effect: keep parent Sensor.currentValue in sync
        sensor.setCurrentValue(reading.getValue());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Reading recorded successfully.");
        response.put("reading", reading);
        response.put("sensorCurrentValue", sensor.getCurrentValue());
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    private Map<String, Object> errorBody(String message) {
        Map<String, Object> err = new HashMap<>();
        err.put("error", message);
        err.put("timestamp", System.currentTimeMillis());
        return err;
    }
}

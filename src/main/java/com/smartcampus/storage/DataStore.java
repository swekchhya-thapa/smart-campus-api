package com.smartcampus.storage;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataStore {

    // All rooms stored by their ID
    // e.g. "LIB-301" -> Room object
    public static final Map<String, Room> rooms = new ConcurrentHashMap<>();

    // All sensors stored by their ID
    // e.g. "TEMP-001" -> Sensor object
    public static final Map<String, Sensor> sensors = new ConcurrentHashMap<>();

    // All readings stored by sensor ID
    // e.g. "TEMP-001" -> [reading1, reading2, reading3]
    public static final Map<String, List<SensorReading>> readings = new ConcurrentHashMap<>();

    // This block runs ONCE when the app starts - seeds some sample data
    static {
        // Create two sample rooms
        Room r1 = new Room("LIB-301", "Library Quiet Study", 50);
        Room r2 = new Room("LAB-101", "Computer Science Lab", 30);
        rooms.put(r1.getId(), r1);
        rooms.put(r2.getId(), r2);

        // Create three sample sensors
        Sensor s1 = new Sensor("TEMP-001", "Temperature", "ACTIVE", 21.5, "LIB-301");
        Sensor s2 = new Sensor("CO2-001", "CO2", "ACTIVE", 410.0, "LAB-101");
        Sensor s3 = new Sensor("OCC-001", "Occupancy", "MAINTENANCE", 0.0, "LIB-301");
        sensors.put(s1.getId(), s1);
        sensors.put(s2.getId(), s2);
        sensors.put(s3.getId(), s3);

        // Link sensors to their rooms
        r1.getSensorIds().add("TEMP-001");
        r1.getSensorIds().add("OCC-001");
        r2.getSensorIds().add("CO2-001");

        // Create empty reading lists for each sensor
        readings.put("TEMP-001", new ArrayList<>());
        readings.put("CO2-001", new ArrayList<>());
        readings.put("OCC-001", new ArrayList<>());
    }
}
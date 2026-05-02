package com.smartcampus.model;

import java.util.ArrayList;
import java.util.List;

public class Room {

    private String id;        // e.g. "LIB-301"
    private String name;      // e.g. "Library Quiet Study"
    private int capacity;     // e.g. 50
    private List<String> sensorIds = new ArrayList<>(); // IDs of sensors in this room

    // Empty constructor - JAX-RS needs this to create objects from JSON
    public Room() {}

    // Constructor for creating rooms with data
    public Room(String id, String name, int capacity) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
    }

    // Getters and Setters - these let other classes read/write the fields
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public List<String> getSensorIds() { return sensorIds; }
    public void setSensorIds(List<String> sensorIds) { this.sensorIds = sensorIds; }
}
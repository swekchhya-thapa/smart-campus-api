package com.smartcampus.model;

import java.util.UUID;

public class SensorReading {

    private String id;        // unique ID for this reading
    private long timestamp;   // when was it recorded (milliseconds since 1970)
    private double value;     // the actual measurement e.g. 22.5

    // Empty constructor - required by JAX-RS
    public SensorReading() {}

    public SensorReading(double value) {
        this.id = UUID.randomUUID().toString(); // auto-generate unique ID
        this.timestamp = System.currentTimeMillis(); // current time
        this.value = value;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }
}
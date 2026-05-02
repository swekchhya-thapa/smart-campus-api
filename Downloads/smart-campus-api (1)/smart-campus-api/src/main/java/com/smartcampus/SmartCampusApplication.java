package com.smartcampus;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

// This tells JAX-RS that all your API endpoints start with /api/v1
// So a room endpoint becomes: http://localhost:8080/api/v1/rooms
@ApplicationPath("/api/v1")
public class SmartCampusApplication extends Application {
    // Nothing needed here - JAX-RS handles everything automatically
}
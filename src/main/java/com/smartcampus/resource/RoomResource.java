package com.smartcampus.resource;

import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;
import com.smartcampus.storage.DataStore;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    // ----------------------------------------------------------------
    // GET /api/v1/rooms
    // Returns all rooms
    // ----------------------------------------------------------------
    @GET
    public Response getAllRooms() {
        Collection<Room> allRooms = DataStore.rooms.values();

        Map<String, Object> response = new HashMap<>();
        response.put("count", allRooms.size());
        response.put("rooms", new ArrayList<>(allRooms));

        return Response.ok(response).build();
    }

    // ----------------------------------------------------------------
    // POST /api/v1/rooms
    // Creates a new room
    // ----------------------------------------------------------------
    @POST
    public Response createRoom(Room room) {

        // Check that ID was provided
        if (room == null || room.getId() == null || room.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorBody("Room ID is required."))
                    .build();
        }

        // Check that name was provided
        if (room.getName() == null || room.getName().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorBody("Room name is required."))
                    .build();
        }

        // Check a room with this ID doesn't already exist
        if (DataStore.rooms.containsKey(room.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(errorBody("A room with ID '" + room.getId() + "' already exists."))
                    .build();
        }

        // Make sure sensorIds list is never null
        if (room.getSensorIds() == null) {
            room.setSensorIds(new ArrayList<>());
        }

        // Save the room
        DataStore.rooms.put(room.getId(), room);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Room created successfully.");
        response.put("room", room);

        // 201 Created is the correct status for a successful POST
        return Response.status(Response.Status.CREATED)
                .entity(response)
                .build();
    }

    // ----------------------------------------------------------------
    // GET /api/v1/rooms/{roomId}
    // Returns one specific room by ID
    // ----------------------------------------------------------------
    @GET
    @Path("/{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {

        Room room = DataStore.rooms.get(roomId);

        // If not found return 404
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorBody("Room '" + roomId + "' not found."))
                    .build();
        }

        return Response.ok(room).build();
    }

    // ----------------------------------------------------------------
    // DELETE /api/v1/rooms/{roomId}
    // Deletes a room - but ONLY if it has no sensors
    // ----------------------------------------------------------------
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {

        Room room = DataStore.rooms.get(roomId);

        // If room doesn't exist return 404
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorBody("Room '" + roomId + "' not found."))
                    .build();
        }

        // SAFETY CHECK - cannot delete if sensors are still in the room
        if (room.getSensorIds() != null && !room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(
                "Cannot delete room '" + roomId + "'. It still has " +
                room.getSensorIds().size() + " sensor(s) assigned. " +
                "Remove all sensors first."
            );
        }

        // Safe to delete
        DataStore.rooms.remove(roomId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Room '" + roomId + "' has been deleted.");

        return Response.ok(response).build();
    }

    // ----------------------------------------------------------------
    // Helper method - builds a standard error response body
    // ----------------------------------------------------------------
    private Map<String, Object> errorBody(String message) {
        Map<String, Object> err = new HashMap<>();
        err.put("error", message);
        err.put("timestamp", System.currentTimeMillis());
        return err;
    }
}
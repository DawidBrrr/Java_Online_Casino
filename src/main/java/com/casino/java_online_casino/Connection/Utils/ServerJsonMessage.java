package com.casino.java_online_casino.Connection.Utils;

import com.google.gson.JsonObject;

public class ServerJsonMessage {

    private static JsonObject create(String status, int code, String message) {
        JsonObject json = new JsonObject();
        json.addProperty("status", status);
        json.addProperty("code", code);
        json.addProperty("message", message);
        return json;
    }

    // --- Sukces ---
    public static JsonObject ok(String message) {
        return create("ok", 200, message);
    }

    // --- Błędy autoryzacji / tokenu ---
    public static JsonObject invalidToken() {
        return create("error", 401, "Token is invalid or malformed.");
    }

    public static JsonObject expiredToken() {
        return create("error", 401, "Token has expired.");
    }

    public static JsonObject missingToken() {
        return create("error", 401, "Authorization token is missing.");
    }

    // --- Błędy HTTP / żądania ---
    public static JsonObject methodNotAllowed() {
        return create("error", 405, "HTTP method not allowed for this endpoint.");
    }

    public static JsonObject badRequest(String details) {
        return create("error", 400, details);
    }

    public static JsonObject unsupportedMediaType() {
        return create("error", 415, "Content-Type is not supported. Use application/json.");
    }

    // --- Błędy systemowe / serwera ---
    public static JsonObject internalServerError() {
        return create("error", 500, "An unexpected error occurred on the server.");
    }

    public static JsonObject databaseError() {
        return create("error", 503, "A database error occurred. Please try again later.");
    }

    // --- Błędy aplikacyjne / użytkownika ---
    public static JsonObject userNotFound() {
        return create("error", 404, "User does not exist.");
    }

    public static JsonObject invalidCredentials() {
        return create("error", 401, "Username or password is incorrect.");
    }

    public static JsonObject accessDenied() {
        return create("error", 403, "You do not have permission to access this resource.");
    }
}

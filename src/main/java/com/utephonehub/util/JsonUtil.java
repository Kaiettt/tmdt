package com.utephonehub.util;

import com.google.gson.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Type;

/**
 * JSON Utility
 * Xử lý parse và serialize JSON
 */
public class JsonUtil {
    
    private static final Logger logger = LogManager.getLogger(JsonUtil.class);
    private final Gson gson;
    
    public JsonUtil() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
    }
    
    /**
     * Custom LocalDateTime adapter for Gson
     * Supports multiple datetime formats:
     * - "yyyy-MM-dd HH:mm:ss" (database format)
     * - "yyyy-MM-dd'T'HH:mm:ss" (ISO-8601 with seconds)
     * - "yyyy-MM-dd'T'HH:mm" (HTML5 datetime-local format)
     */
    private static class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
        private static final DateTimeFormatter OUTPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        private static final DateTimeFormatter[] INPUT_FORMATTERS = {
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,           // yyyy-MM-dd'T'HH:mm:ss
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"), // HTML5 datetime-local (no seconds)
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"), // Database format
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")   // Database format (no seconds)
        };
        
        @Override
        public JsonElement serialize(LocalDateTime dateTime, java.lang.reflect.Type type, JsonSerializationContext context) {
            return new JsonPrimitive(dateTime.format(OUTPUT_FORMATTER));
        }
        
        @Override
        public LocalDateTime deserialize(JsonElement json, java.lang.reflect.Type type, JsonDeserializationContext context) {
            String dateTimeString = json.getAsString();
            
            // Try parsing with each formatter until one succeeds
            for (DateTimeFormatter formatter : INPUT_FORMATTERS) {
                try {
                    return LocalDateTime.parse(dateTimeString, formatter);
                } catch (Exception e) {
                    // Try next formatter
                }
            }
            
            // If all formatters fail, throw exception with helpful message
            throw new JsonParseException(
                "Unable to parse datetime: " + dateTimeString + 
                ". Expected formats: yyyy-MM-dd'T'HH:mm:ss, yyyy-MM-dd'T'HH:mm, yyyy-MM-dd HH:mm:ss, or yyyy-MM-dd HH:mm"
            );
        }
    }
    
    /**
     * Parse JSON từ HttpServletRequest
     * @param request HttpServletRequest
     * @param clazz Class type
     * @return Parsed object
     */
    public <T> T parseJson(HttpServletRequest request, Class<T> clazz) throws IOException {
        try {
            StringBuilder jsonBuilder = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }
            
            String jsonString = jsonBuilder.toString();
            logger.debug("Parsing JSON: {}", jsonString);
            
            return gson.fromJson(jsonString, clazz);
        } catch (JsonSyntaxException e) {
            logger.error("Invalid JSON format", e);
            throw new IllegalArgumentException("Invalid JSON format");
        }
    }
    
    /**
     * Parse JSON từ HttpServletRequest với Type
     * @param request HttpServletRequest
     * @param type Type
     * @return Parsed object
     */
    public <T> T parseJson(HttpServletRequest request, Type type) throws IOException {
        try {
            StringBuilder jsonBuilder = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }
            
            String jsonString = jsonBuilder.toString();
            logger.debug("Parsing JSON: {}", jsonString);
            
            return gson.fromJson(jsonString, type);
        } catch (JsonSyntaxException e) {
            logger.error("Invalid JSON format", e);
            throw new IllegalArgumentException("Invalid JSON format");
        }
    }
    
    /**
     * Convert object to JSON string
     * @param object Object to convert
     * @return JSON string
     */
    public String toJson(Object object) {
        try {
            return gson.toJson(object);
        } catch (Exception e) {
            logger.error("Error converting object to JSON", e);
            throw new RuntimeException("Failed to convert object to JSON", e);
        }
    }
    
    /**
     * Convert object to pretty JSON string
     * @param object Object to convert
     * @return Pretty JSON string
     */
    public String toPrettyJson(Object object) {
        try {
            return gson.toJson(object);
        } catch (Exception e) {
            logger.error("Error converting object to pretty JSON", e);
            throw new RuntimeException("Failed to convert object to pretty JSON", e);
        }
    }
    
    /**
     * Parse JSON string to object
     * @param json JSON string
     * @param clazz Class type
     * @return Parsed object
     */
    public <T> T fromJson(String json, Class<T> clazz) {
        try {
            return gson.fromJson(json, clazz);
        } catch (JsonSyntaxException e) {
            logger.error("Invalid JSON format: {}", json, e);
            throw new IllegalArgumentException("Invalid JSON format");
        }
    }
    
    /**
     * Parse JSON string to object với Type
     * @param json JSON string
     * @param type Type
     * @return Parsed object
     */
    public <T> T fromJson(String json, Type type) {
        try {
            return gson.fromJson(json, type);
        } catch (JsonSyntaxException e) {
            logger.error("Invalid JSON format: {}", json, e);
            throw new IllegalArgumentException("Invalid JSON format");
        }
    }
    
    /**
     * Check if string is valid JSON
     * @param json JSON string
     * @return true if valid JSON
     */
    public boolean isValidJson(String json) {
        try {
            gson.fromJson(json, Object.class);
            return true;
        } catch (JsonSyntaxException e) {
            return false;
        }
    }
}

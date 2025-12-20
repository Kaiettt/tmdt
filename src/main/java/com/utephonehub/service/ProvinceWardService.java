package com.utephonehub.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Vietnam Province and Ward API Service
 * Integrate with https://provinces.open-api.vn/api/v2/
 */
public class ProvinceWardService {
    
    private static final Logger logger = LogManager.getLogger(ProvinceWardService.class);
    private final Gson gson = new Gson();
    
    // Use API v2 with depth parameter (max depth=2)
    private static final String BASE_URL = "https://provinces.open-api.vn/api/v2";
    private static final String PROVINCES_URL = BASE_URL + "/p";
    private static final String WARDS_URL = BASE_URL + "/w";
    
    /**
     * Get all provinces
     */
    public List<ProvinceDto> getAllProvinces() {
        try {
            String jsonResponse = makeHttpRequest(PROVINCES_URL);
            if (jsonResponse == null) return new ArrayList<>();
            
            Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
            List<Map<String, Object>> provinces = gson.fromJson(jsonResponse, listType);
            
            List<ProvinceDto> result = new ArrayList<>();
            for (Map<String, Object> province : provinces) {
                ProvinceDto dto = new ProvinceDto();
                dto.setCode(String.valueOf(((Double) province.get("code")).intValue()));
                dto.setName((String) province.get("name"));
                result.add(dto);
            }
            
            logger.info("Loaded {} provinces from API", result.size());
            return result;
            
        } catch (Exception e) {
            logger.error("Error loading provinces", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get province by code
     */
    public ProvinceDto getProvinceByCode(String code) {
        try {
            String url = PROVINCES_URL + "/" + code;
            String jsonResponse = makeHttpRequest(url);
            if (jsonResponse == null) return null;
            
            Map<String, Object> province = gson.fromJson(jsonResponse, Map.class);
            
            ProvinceDto dto = new ProvinceDto();
            dto.setCode(String.valueOf(((Double) province.get("code")).intValue()));
            dto.setName((String) province.get("name"));
            
            return dto;
            
        } catch (Exception e) {
            logger.error("Error loading province by code: {}", code, e);
            return null;
        }
    }
    
    /**
     * Get all wards
     */
    public List<WardDto> getAllWards() {
        try {
            String jsonResponse = makeHttpRequest(WARDS_URL);
            if (jsonResponse == null) return new ArrayList<>();
            
            Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
            List<Map<String, Object>> wards = gson.fromJson(jsonResponse, listType);
            
            List<WardDto> result = new ArrayList<>();
            for (Map<String, Object> ward : wards) {
                WardDto dto = new WardDto();
                dto.setCode(String.valueOf(((Double) ward.get("code")).intValue()));
                dto.setName((String) ward.get("name"));
                // Province info not available in basic ward list API
                result.add(dto);
            }
            
            logger.info("Loaded {} wards from API", result.size());
            return result;
            
        } catch (Exception e) {
            logger.error("Error loading wards", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get ward by code
     */
    public WardDto getWardByCode(String code) {
        try {
            String url = WARDS_URL + "/" + code;
            String jsonResponse = makeHttpRequest(url);
            if (jsonResponse == null) return null;
            
            Map<String, Object> ward = gson.fromJson(jsonResponse, Map.class);
            
            WardDto dto = new WardDto();
            dto.setCode(String.valueOf(((Double) ward.get("code")).intValue()));
            dto.setName((String) ward.get("name"));
            // Province info not available in single ward API
            return dto;
            
        } catch (Exception e) {
            logger.error("Error loading ward by code: {}", code, e);
            return null;
        }
    }
    
    /**
     * Get wards by province code
     * Uses API v2 with depth=2 - returns wards directly at province level
     */
    public List<WardDto> getWardsByProvinceCode(String provinceCode) {
        try {
            // API v2 with depth=2 returns wards array directly in province object
            String url = PROVINCES_URL + "/" + provinceCode + "?depth=2";
            logger.info("Fetching wards for province {} from: {}", provinceCode, url);
            
            String jsonResponse = makeHttpRequest(url);
            if (jsonResponse == null) {
                logger.warn("No response from API for province: {}", provinceCode);
                return new ArrayList<>();
            }
            
            Map<String, Object> province = gson.fromJson(jsonResponse, Map.class);
            List<WardDto> result = new ArrayList<>();
            
            // Get wards array directly from province (depth=2 response structure)
            Object wardsObj = province.get("wards");
            if (wardsObj instanceof List) {
                List<Map<String, Object>> wards = (List<Map<String, Object>>) wardsObj;
                String provinceName = (String) province.get("name");
                
                logger.info("Found {} wards for province: {}", wards.size(), provinceName);
                
                for (Map<String, Object> ward : wards) {
                    WardDto dto = new WardDto();
                    dto.setCode(String.valueOf(((Double) ward.get("code")).intValue()));
                    dto.setName((String) ward.get("name"));
                    dto.setProvinceCode(provinceCode);
                    dto.setProvinceName(provinceName);
                    result.add(dto);
                }
            } else {
                logger.warn("No wards array found in province response for: {}", provinceCode);
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error loading wards for province: {}", provinceCode, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Make HTTP request to API
     */
    private String makeHttpRequest(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(10000); // 10 seconds
            connection.setReadTimeout(10000); // 10 seconds
            
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                return response.toString();
            } else {
                logger.warn("HTTP request failed with code: {} for URL: {}", responseCode, urlString);
                return null;
            }
            
        } catch (Exception e) {
            logger.error("Error making HTTP request to: {}", urlString, e);
            return null;
        }
    }
    
    /**
     * Province DTO
     */
    public static class ProvinceDto {
        private String code;
        private String name;
        
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
    
    /**
     * Ward DTO
     */
    public static class WardDto {
        private String code;
        private String name;
        private String provinceCode;
        private String provinceName;
        
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getProvinceCode() { return provinceCode; }
        public void setProvinceCode(String provinceCode) { this.provinceCode = provinceCode; }
        public String getProvinceName() { return provinceName; }
        public void setProvinceName(String provinceName) { this.provinceName = provinceName; }
    }
}
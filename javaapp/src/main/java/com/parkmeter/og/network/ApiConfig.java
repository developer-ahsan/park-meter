package com.parkmeter.og.network;

/**
 * Configuration class for API settings
 */
public class ApiConfig {
    
    // API Base URL
    public static final String BASE_URL = "https://parkapp.ca/api/";
    
    // API Endpoints
    public static final String GET_ZONES_ENDPOINT = "getZones"; // POST method
    
    // Network timeouts
    public static final int CONNECT_TIMEOUT_SECONDS = 30;
    public static final int READ_TIMEOUT_SECONDS = 30;
    public static final int WRITE_TIMEOUT_SECONDS = 30;
} 

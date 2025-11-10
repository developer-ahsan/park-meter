package com.parkmeter.og.service;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.parkmeter.og.model.Literal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LiteralsDownloadService {
    private static final String TAG = "LiteralsDownloadService";
    private static final String LITERALS_FILE = "literals.json";
    private static final String GOOGLE_SHEETS_URL = "https://docs.google.com/spreadsheets/d/e/2PACX-1vTrC_EcFDpu0YM722BhD2x-nsvTiMNMx6AoMXS4LAtjkWA289GfUlrctrQM56AG0ULgYolkRJIk83-h/pub?gid=1687686550&single=true&output=csv";
    
    private final Context context;
    private final OkHttpClient httpClient;
    private final Gson gson;

    public LiteralsDownloadService(Context context) {
        this.context = context;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }

    /**
     * Download literals from Google Sheets and save locally
     */
    public void downloadAndCacheLiterals(LiteralsDownloadCallback callback) {
        new Thread(() -> {
            try {
                Log.d(TAG, "Starting literals download...");
                
                // Download CSV data from Google Sheets
                String csvData = downloadCsvData();
                if (csvData == null || csvData.isEmpty()) {
                    callback.onFailure("Failed to download CSV data");
                    return;
                }

                // Parse CSV to literals
                List<Literal> literals = parseCsvToLiterals(csvData);
                if (literals.isEmpty()) {
                    callback.onFailure("No literals found in CSV data");
                    return;
                }

                // Save to local file
                boolean saved = saveLiteralsToFile(literals);
                if (!saved) {
                    callback.onFailure("Failed to save literals to local file");
                    return;
                }

                Log.d(TAG, "Successfully downloaded and cached " + literals.size() + " literals");
                callback.onSuccess(literals);

            } catch (Exception e) {
                Log.e(TAG, "Error downloading literals: " + e.getMessage(), e);
                callback.onFailure("Error: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Load literals from local cache
     */
    public List<Literal> loadCachedLiterals() {
        try {
            File file = new File(context.getFilesDir(), LITERALS_FILE);
            if (!file.exists()) {
                Log.d(TAG, "No cached literals file found");
                return new ArrayList<>();
            }

            FileInputStream fis = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder jsonString = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonString.append(line);
            }
            reader.close();
            fis.close();

            Literal[] literals = gson.fromJson(jsonString.toString(), Literal[].class);
            List<Literal> literalList = new ArrayList<>();
            for (Literal literal : literals) {
                literalList.add(literal);
            }

            Log.d(TAG, "Loaded " + literalList.size() + " cached literals");
            return literalList;

        } catch (Exception e) {
            Log.e(TAG, "Error loading cached literals: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Get literals as a map for quick lookup
     */
    public Map<String, Literal> getLiteralsMap() {
        List<Literal> literals = loadCachedLiterals();
        Map<String, Literal> map = new HashMap<>();
        for (Literal literal : literals) {
            map.put(literal.getKey(), literal);
        }
        return map;
    }

    private String downloadCsvData() throws IOException {
        Request request = new Request.Builder()
                .url(GOOGLE_SHEETS_URL)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response.code());
            }

            if (response.body() == null) {
                throw new IOException("Response body is null");
            }

            return response.body().string();
        }
    }

    private List<Literal> parseCsvToLiterals(String csvData) {
        List<Literal> literals = new ArrayList<>();
        String[] lines = csvData.split("\n");
        
        // Skip header row
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            // Parse CSV line (handle commas within quotes)
            String[] parts = parseCsvLine(line);
            if (parts.length >= 3) {
                String key = parts[0].trim();
                String english = parts[1].trim();
                String french = parts[2].trim();
                
                // Remove quotes if present
                key = removeQuotes(key);
                english = removeQuotes(english);
                french = removeQuotes(french);
                
                if (!key.isEmpty()) {
                    literals.add(new Literal(key, english, french));
                }
            }
        }
        
        return literals;
    }

    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        
        return result.toArray(new String[0]);
    }

    private String removeQuotes(String str) {
        if (str.startsWith("\"") && str.endsWith("\"")) {
            return str.substring(1, str.length() - 1);
        }
        return str;
    }

    private boolean saveLiteralsToFile(List<Literal> literals) {
        try {
            String json = gson.toJson(literals);
            File file = new File(context.getFilesDir(), LITERALS_FILE);
            
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(json.getBytes());
            fos.close();
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error saving literals to file: " + e.getMessage(), e);
            return false;
        }
    }

    public interface LiteralsDownloadCallback {
        void onSuccess(List<Literal> literals);
        void onFailure(String error);
    }
}

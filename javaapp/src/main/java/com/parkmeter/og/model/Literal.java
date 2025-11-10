package com.parkmeter.og.model;

import com.google.gson.annotations.SerializedName;

public class Literal {
    @SerializedName("key")
    private String key;
    
    @SerializedName("english")
    private String english;
    
    @SerializedName("french")
    private String french;

    public Literal() {}

    public Literal(String key, String english, String french) {
        this.key = key;
        this.english = english;
        this.french = french;
    }

    // Getters
    public String getKey() { return key; }
    public String getEnglish() { return english; }
    public String getFrench() { return french; }

    // Setters
    public void setKey(String key) { this.key = key; }
    public void setEnglish(String english) { this.english = english; }
    public void setFrench(String french) { this.french = french; }
    
    /**
     * Get the text for the specified language
     * @param languageCode "en" for English, "fr" for French
     * @return The text in the specified language, or English as fallback
     */
    public String getText(String languageCode) {
        if ("fr".equals(languageCode) && french != null && !french.trim().isEmpty()) {
            return french;
        }
        return english != null ? english : "";
    }
}

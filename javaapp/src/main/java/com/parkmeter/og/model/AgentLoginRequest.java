package com.parkmeter.og.model;

import com.google.gson.annotations.SerializedName;

public class AgentLoginRequest {
    @SerializedName("language")
    private String language;

    @SerializedName("rememberMe")
    private boolean rememberMe;

    @SerializedName("email")
    private String email;

    @SerializedName("password")
    private String password;

    public AgentLoginRequest(String language, boolean rememberMe, String email, String password) {
        this.language = language;
        this.rememberMe = rememberMe;
        this.email = email;
        this.password = password;
    }

    public String getLanguage() { return language; }
    public boolean isRememberMe() { return rememberMe; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
}

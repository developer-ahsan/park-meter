package com.parkmeter.og.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class AgentLoginResponse {
    @SerializedName("auth")
    private boolean auth;

    @SerializedName("result")
    private AgentUser result;

    @SerializedName("token")
    private String token;

    public boolean isAuth() { return auth; }
    public AgentUser getResult() { return result; }
    public String getToken() { return token; }

    public static class AgentUser implements Serializable {
        @SerializedName("_id")
        private String id;

        @SerializedName("fname")
        private String fname;

        @SerializedName("lname")
        private String lname;

        @SerializedName("email")
        private String email;

        @SerializedName("role")
        private String role;

        @SerializedName("token")
        private String userToken;

        @SerializedName("org")
        private Zone.Organization org;

        @SerializedName("organizations")
        private List<Zone.Organization> organizations;

        @SerializedName("cities")
        private List<Zone.City> cities;

        public String getId() { return id; }
        public String getFname() { return fname; }
        public String getLname() { return lname; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
        public String getUserToken() { return userToken; }
        public Zone.Organization getOrg() { return org; }
        public List<Zone.Organization> getOrganizations() { return organizations; }
        public List<Zone.City> getCities() { return cities; }

        public String getFullName() {
            String first = fname != null ? fname : "";
            String last = lname != null ? lname : "";
            return (first + " " + last).trim();
        }
    }
}

package com.parkmeter.og.model;

public class Language {
    private String code;
    private String name;
    private int flagResourceId;
    
    public Language(String code, String name, int flagResourceId) {
        this.code = code;
        this.name = name;
        this.flagResourceId = flagResourceId;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getName() {
        return name;
    }
    
    public int getFlagResourceId() {
        return flagResourceId;
    }
    
    @Override
    public String toString() {
        return name;
    }
}

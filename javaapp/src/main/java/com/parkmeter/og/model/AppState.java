package com.parkmeter.og.model;

public class AppState {
    private Zone selectedZone;
    private Zone.Organization selectedOrganization;
    private boolean isZoneSelected;
    private String vehicleNumber;
    private String selectedLanguageCode = "en"; // Default to English

    public AppState() {
        this.isZoneSelected = false;
    }

    public Zone getSelectedZone() {
        return selectedZone;
    }

    public void setSelectedZone(Zone selectedZone) {
        this.selectedZone = selectedZone;
        this.selectedOrganization = selectedZone != null ? selectedZone.getOrganization() : null;
        this.isZoneSelected = selectedZone != null;
    }

    public Zone.Organization getSelectedOrganization() {
        return selectedOrganization;
    }

    public void setSelectedOrganization(Zone.Organization selectedOrganization) {
        this.selectedOrganization = selectedOrganization;
    }

    public boolean isZoneSelected() {
        return isZoneSelected;
    }

    public void setZoneSelected(boolean zoneSelected) {
        isZoneSelected = zoneSelected;
    }

    public String getOrganizationColor() {
        return selectedOrganization != null ? selectedOrganization.getColor() : "#f7951c";
    }

    public String getOrganizationLogoUrl() {
        if (selectedOrganization != null) {
            String logo = selectedOrganization.getLogo();
            if (logo == null) {
                return null;
            }
            logo = logo.trim();
            while (logo.startsWith("@")) {
                logo = logo.substring(1);
            }
            if (logo.isEmpty()) {
                return null;
            }
            if (logo.startsWith("http://") || logo.startsWith("https://")) {
                return logo;
            }
            if (logo.startsWith("/")) {
                return "https://parkapp.ca/cwp_api" + logo;
            }
            return "https://parkapp.ca/cwp_api/" + logo;
        }
        return null;
    }

    public String getOrganizationName() {
        return selectedOrganization != null ? selectedOrganization.getOrgName() : "Parkapp Meter";
    }
    
    // Get Zone ID for use in next screens
    public String getSelectedZoneId() {
        return selectedZone != null ? selectedZone.getId() : null;
    }
    
    // Get Zone Name for use in next screens
    public String getSelectedZoneName() {
        return selectedZone != null ? selectedZone.getZoneName() : null;
    }
    
    // Get Zone Code for use in next screens
    public String getSelectedZoneCode() {
        return selectedZone != null ? selectedZone.getZoneCode() : null;
    }
    
    // Get Organization ID for use in next screens
    public String getSelectedOrganizationId() {
        return selectedOrganization != null ? selectedOrganization.getId() : null;
    }
    
    // Get Organization Name for use in next screens
    public String getSelectedOrganizationName() {
        return selectedOrganization != null ? selectedOrganization.getOrgName() : null;
    }
    
    // Get City ID for use in next screens
    public String getSelectedCityId() {
        return selectedZone != null && selectedZone.getCity() != null ? selectedZone.getCity().getId() : null;
    }
    
    // Language selection methods
    public String getSelectedLanguageCode() {
        return selectedLanguageCode;
    }
    
    public void setSelectedLanguageCode(String languageCode) {
        this.selectedLanguageCode = languageCode;
    }
    
    // Get City Name for use in next screens
    public String getSelectedCityName() {
        return selectedZone != null && selectedZone.getCity() != null ? selectedZone.getCity().getCityName() : null;
    }
    
    // Get complete zone data for logging
    public String getZoneSelectionLog() {
        if (selectedZone == null) {
            return "No zone selected";
        }
        
        StringBuilder log = new StringBuilder();
        log.append("Zone Selected: ").append(selectedZone.getZoneName())
           .append(" (ID: ").append(selectedZone.getId()).append(")")
           .append(" (Code: ").append(selectedZone.getZoneCode()).append(")");
        
        if (selectedZone.getCity() != null) {
            log.append(" | City: ").append(selectedZone.getCity().getCityName())
               .append(" (ID: ").append(selectedZone.getCity().getId()).append(")");
        }
        
        if (selectedOrganization != null) {
            log.append(" | Organization: ").append(selectedOrganization.getOrgName())
               .append(" (ID: ").append(selectedOrganization.getId()).append(")")
               .append(" | Color: ").append(selectedOrganization.getColor())
               .append(" | Logo: ").append(selectedOrganization.getLogo());
        }
        
        return log.toString();
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }
    
    public String getVehicleNumber() {
        return vehicleNumber;
    }
    
    public void clearSelection() {
        this.selectedZone = null;
        this.selectedOrganization = null;
        this.isZoneSelected = false;
        this.vehicleNumber = null;
    }
} 

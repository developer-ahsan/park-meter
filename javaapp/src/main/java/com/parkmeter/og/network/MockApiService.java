package com.parkmeter.og.network;

import com.parkmeter.og.model.Zone;

import java.util.ArrayList;
import java.util.List;

public class MockApiService {
    
    public static List<Zone> getMockZones() {
        List<Zone> zones = new ArrayList<>();
        
        // Create mock zone 1
        Zone zone1 = new Zone();
        zone1.setId("66c3b5070ddbb8504f2910b6");
        zone1.setZoneName("1213 Wellington");
        zone1.setZoneCode("0001");
        
        Zone.City city1 = new Zone.City();
        city1.setId("66c3b47e0ddbb8504f2910a7");
        city1.setCityName("Ottawa");
        city1.setTimeZone("America/New_York");
        zone1.setCity(city1);
        
        Zone.Organization org1 = new Zone.Organization();
        org1.setId("66c12693a0cd2e395d34af8e");
        org1.setOrgName("Park45");
        org1.setSubDomain("root");
        org1.setServiceFee(35);
        org1.setColor("#f7941c");
        org1.setPaymentGateway("stripe");
        org1.setLogo("uploads/logo/logo-1750204187208.png");
        org1.setSslInstalled(true);
        zone1.setOrganization(org1);
        
        zone1.setZoneType(0);
        zone1.setVisitorPassTime(0);
        zone1.setBusinessPass(false);
        zone1.setCanUserKickOut(false);
        zone1.setEnableExtension(false);
        zone1.setPlateEditable(true);
        zone1.setNoOfTimesPlateCanEdit(3);
        
        zones.add(zone1);
        
        // Create mock zone 2
        Zone zone2 = new Zone();
        zone2.setId("66c3b5070ddbb8504f2910b7");
        zone2.setZoneName("Downtown Core");
        zone2.setZoneCode("0002");
        
        Zone.City city2 = new Zone.City();
        city2.setId("66c3b47e0ddbb8504f2910a8");
        city2.setCityName("Ottawa");
        city2.setTimeZone("America/New_York");
        zone2.setCity(city2);
        
        Zone.Organization org2 = new Zone.Organization();
        org2.setId("66c12693a0cd2e395d34af8f");
        org2.setOrgName("Park45");
        org2.setSubDomain("root");
        org2.setServiceFee(40);
        org2.setColor("#3c91FF");
        org2.setPaymentGateway("stripe");
        org2.setLogo("uploads/logo/logo-1750204187209.png");
        org2.setSslInstalled(true);
        zone2.setOrganization(org2);
        
        zone2.setZoneType(1);
        zone2.setVisitorPassTime(120);
        zone2.setBusinessPass(true);
        zone2.setCanUserKickOut(true);
        zone2.setEnableExtension(true);
        zone2.setPlateEditable(true);
        zone2.setNoOfTimesPlateCanEdit(5);
        
        zones.add(zone2);
        
        // Create mock zone 3
        Zone zone3 = new Zone();
        zone3.setId("66c3b5070ddbb8504f2910b8");
        zone3.setZoneName("University District");
        zone3.setZoneCode("0003");
        
        Zone.City city3 = new Zone.City();
        city3.setId("66c3b47e0ddbb8504f2910a9");
        city3.setCityName("Ottawa");
        city3.setTimeZone("America/New_York");
        zone3.setCity(city3);
        
        Zone.Organization org3 = new Zone.Organization();
        org3.setId("66c12693a0cd2e395d34af90");
        org3.setOrgName("Park45");
        org3.setSubDomain("root");
        org3.setServiceFee(25);
        org3.setColor("#28a745");
        org3.setPaymentGateway("stripe");
        org3.setLogo("uploads/logo/logo-1750204187210.png");
        org3.setSslInstalled(true);
        zone3.setOrganization(org3);
        
        zone3.setZoneType(2);
        zone3.setVisitorPassTime(60);
        zone3.setBusinessPass(false);
        zone3.setCanUserKickOut(false);
        zone3.setEnableExtension(false);
        zone3.setPlateEditable(true);
        zone3.setNoOfTimesPlateCanEdit(2);
        
        zones.add(zone3);
        
        // Create mock zone 4
        Zone zone4 = new Zone();
        zone4.setId("66c3b5070ddbb8504f2910b9");
        zone4.setZoneName("Westboro Village");
        zone4.setZoneCode("0004");
        
        Zone.City city4 = new Zone.City();
        city4.setId("66c3b47e0ddbb8504f2910aa");
        city4.setCityName("Ottawa");
        city4.setTimeZone("America/New_York");
        zone4.setCity(city4);
        
        Zone.Organization org4 = new Zone.Organization();
        org4.setId("66c12693a0cd2e395d34af91");
        org4.setOrgName("Park45");
        org4.setSubDomain("root");
        org4.setServiceFee(30);
        org4.setColor("#FF6B35");
        org4.setPaymentGateway("stripe");
        org4.setLogo("uploads/logo/logo-1750204187211.png");
        org4.setSslInstalled(true);
        zone4.setOrganization(org4);
        
        zone4.setZoneType(0);
        zone4.setVisitorPassTime(90);
        zone4.setBusinessPass(true);
        zone4.setCanUserKickOut(false);
        zone4.setEnableExtension(true);
        zone4.setPlateEditable(true);
        zone4.setNoOfTimesPlateCanEdit(4);
        
        zones.add(zone4);
        
        // Create mock zone 5
        Zone zone5 = new Zone();
        zone5.setId("66c3b5070ddbb8504f2910ba");
        zone5.setZoneName("Hintonburg");
        zone5.setZoneCode("0005");
        
        Zone.City city5 = new Zone.City();
        city5.setId("66c3b47e0ddbb8504f2910ab");
        city5.setCityName("Ottawa");
        city5.setTimeZone("America/New_York");
        zone5.setCity(city5);
        
        Zone.Organization org5 = new Zone.Organization();
        org5.setId("66c12693a0cd2e395d34af92");
        org5.setOrgName("Park45");
        org5.setSubDomain("root");
        org5.setServiceFee(35);
        org5.setColor("#9C27B0");
        org5.setPaymentGateway("stripe");
        org5.setLogo("uploads/logo/logo-1750204187212.png");
        org5.setSslInstalled(true);
        zone5.setOrganization(org5);
        
        zone5.setZoneType(1);
        zone5.setVisitorPassTime(120);
        zone5.setBusinessPass(true);
        zone5.setCanUserKickOut(true);
        zone5.setEnableExtension(true);
        zone5.setPlateEditable(true);
        zone5.setNoOfTimesPlateCanEdit(3);
        
        zones.add(zone5);
        
        // Create mock zone 6
        Zone zone6 = new Zone();
        zone6.setId("66c3b5070ddbb8504f2910bb");
        zone6.setZoneName("Centretown");
        zone6.setZoneCode("0006");
        
        Zone.City city6 = new Zone.City();
        city6.setId("66c3b47e0ddbb8504f2910ac");
        city6.setCityName("Ottawa");
        city6.setTimeZone("America/New_York");
        zone6.setCity(city6);
        
        Zone.Organization org6 = new Zone.Organization();
        org6.setId("66c12693a0cd2e395d34af93");
        org6.setOrgName("Park45");
        org6.setSubDomain("root");
        org6.setServiceFee(45);
        org6.setColor("#FF5722");
        org6.setPaymentGateway("stripe");
        org6.setLogo("uploads/logo/logo-1750204187213.png");
        org6.setSslInstalled(true);
        zone6.setOrganization(org6);
        
        zone6.setZoneType(2);
        zone6.setVisitorPassTime(180);
        zone6.setBusinessPass(true);
        zone6.setCanUserKickOut(true);
        zone6.setEnableExtension(true);
        zone6.setPlateEditable(true);
        zone6.setNoOfTimesPlateCanEdit(5);
        
        zones.add(zone6);
        
        // Create mock zone 7
        Zone zone7 = new Zone();
        zone7.setId("66c3b5070ddbb8504f2910bc");
        zone7.setZoneName("Sandy Hill");
        zone7.setZoneCode("0007");
        
        Zone.City city7 = new Zone.City();
        city7.setId("66c3b47e0ddbb8504f2910ad");
        city7.setCityName("Ottawa");
        city7.setTimeZone("America/New_York");
        zone7.setCity(city7);
        
        Zone.Organization org7 = new Zone.Organization();
        org7.setId("66c12693a0cd2e395d34af94");
        org7.setOrgName("Park45");
        org7.setSubDomain("root");
        org7.setServiceFee(25);
        org7.setColor("#607D8B");
        org7.setPaymentGateway("stripe");
        org7.setLogo("uploads/logo/logo-1750204187214.png");
        org7.setSslInstalled(true);
        zone7.setOrganization(org7);
        
        zone7.setZoneType(0);
        zone7.setVisitorPassTime(60);
        zone7.setBusinessPass(false);
        zone7.setCanUserKickOut(false);
        zone7.setEnableExtension(false);
        zone7.setPlateEditable(true);
        zone7.setNoOfTimesPlateCanEdit(2);
        
        zones.add(zone7);
        
        // Create mock zone 8
        Zone zone8 = new Zone();
        zone8.setId("66c3b5070ddbb8504f2910bd");
        zone8.setZoneName("New Edinburgh");
        zone8.setZoneCode("0008");
        
        Zone.City city8 = new Zone.City();
        city8.setId("66c3b47e0ddbb8504f2910ae");
        city8.setCityName("Ottawa");
        city8.setTimeZone("America/New_York");
        zone8.setCity(city8);
        
        Zone.Organization org8 = new Zone.Organization();
        org8.setId("66c12693a0cd2e395d34af95");
        org8.setOrgName("Park45");
        org8.setSubDomain("root");
        org8.setServiceFee(40);
        org8.setColor("#795548");
        org8.setPaymentGateway("stripe");
        org8.setLogo("uploads/logo/logo-1750204187215.png");
        org8.setSslInstalled(true);
        zone8.setOrganization(org8);
        
        zone8.setZoneType(1);
        zone8.setVisitorPassTime(150);
        zone8.setBusinessPass(true);
        zone8.setCanUserKickOut(true);
        zone8.setEnableExtension(true);
        zone8.setPlateEditable(true);
        zone8.setNoOfTimesPlateCanEdit(4);
        
        zones.add(zone8);
        
        return zones;
    }
} 

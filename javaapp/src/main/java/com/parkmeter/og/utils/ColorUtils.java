package com.parkmeter.og.utils;

import android.graphics.Color;

public class ColorUtils {
    
    /**
     * Convert hex color string to Android color integer
     * @param hexColor Hex color string (e.g., "#f7941c" or "f7941c")
     * @return Android color integer
     */
    public static int hexToColor(String hexColor) {
        if (hexColor == null || hexColor.isEmpty()) {
            return Color.parseColor("#f7951c"); // Default color
        }
        
        try {
            // Remove # if present
            if (hexColor.startsWith("#")) {
                hexColor = hexColor.substring(1);
            }
            
            return Color.parseColor("#" + hexColor);
        } catch (Exception e) {
            return Color.parseColor("#3c91FF"); // Default color on error
        }
    }
    
    /**
     * Check if a color is dark (for determining text color)
     * @param color Android color integer
     * @return true if color is dark, false if light
     */
    public static boolean isColorDark(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness >= 0.5;
    }
    
    /**
     * Get appropriate text color for a background color
     * @param backgroundColor Background color integer
     * @return Text color integer (white for dark backgrounds, black for light backgrounds)
     */
    public static int getTextColorForBackground(int backgroundColor) {
        return isColorDark(backgroundColor) ? Color.WHITE : Color.BLACK;
    }
    
    /**
     * Get a darker version of a hex color
     * @param hexColor Hex color string
     * @return Darker hex color string
     */
    public static String getDarkerColor(String hexColor) {
        if (hexColor == null || hexColor.isEmpty()) {
            return "#e6850a"; // Default darker orange
        }
        
        try {
            int color = hexToColor(hexColor);
            float[] hsv = new float[3];
            Color.colorToHSV(color, hsv);
            hsv[2] = Math.max(0.0f, hsv[2] - 0.2f); // Make darker by reducing value
            return String.format("#%06X", (0xFFFFFF & Color.HSVToColor(hsv)));
        } catch (Exception e) {
            return "#e6850a"; // Default darker orange
        }
    }
} 

package com.parkmeter.og.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.parkmeter.og.R;
import com.parkmeter.og.StripeTerminalApplication;

/**
 * Utility class to manage app-wide theming based on organization colors
 */
public class AppThemeManager {
    
    private static AppThemeManager instance;
    private String currentOrgColor = "#f7941c"; // Default orange color
    
    private AppThemeManager() {}
    
    public static AppThemeManager getInstance() {
        if (instance == null) {
            instance = new AppThemeManager();
        }
        return instance;
    }
    
    /**
     * Update the current organization color
     */
    public void updateOrganizationColor(String orgColor) {
        this.currentOrgColor = orgColor != null ? orgColor : "#f7941c";
    }
    
    /**
     * Get the current organization color
     */
    public String getCurrentOrgColor() {
        return currentOrgColor;
    }
    
    /**
     * Get the current organization color as integer
     */
    public int getCurrentOrgColorInt() {
        return ColorUtils.hexToColor(currentOrgColor);
    }
    
    /**
     * Apply organization color to a TextView
     */
    public void applyTextColor(TextView textView) {
        if (textView != null) {
            textView.setTextColor(getCurrentOrgColorInt());
        }
    }
    
    /**
     * Apply organization color to a Button
     */
    public void applyButtonColor(Button button) {
        if (button != null) {
            button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getCurrentOrgColorInt()));
            // Set text color to white for better contrast on colored background
            button.setTextColor(android.graphics.Color.WHITE);
        }
    }
    
    /**
     * Apply organization color to a MaterialButton
     */
    public void applyMaterialButtonColor(MaterialButton button) {
        if (button != null) {
            button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getCurrentOrgColorInt()));
            // Set text color to white for better contrast on colored background
            button.setTextColor(android.graphics.Color.WHITE);
        }
    }
    
    /**
     * Apply organization color to an ImageView tint
     */
    public void applyImageViewTint(ImageView imageView) {
        if (imageView != null) {
            imageView.setColorFilter(getCurrentOrgColorInt());
        }
    }
    
    /**
     * Apply organization color to TextInputLayout
     */
    public void applyTextInputLayoutColor(TextInputLayout textInputLayout) {
        if (textInputLayout != null) {
            textInputLayout.setBoxStrokeColor(getCurrentOrgColorInt());
            textInputLayout.setHintTextColor(android.content.res.ColorStateList.valueOf(getCurrentOrgColorInt()));
            // Also tint any start icon (e.g., search icon) to the org color
            textInputLayout.setStartIconTintList(android.content.res.ColorStateList.valueOf(getCurrentOrgColorInt()));
        }
    }
    
    /**
     * Apply organization color to EditText
     */
    public void applyEditTextColor(EditText editText) {
        if (editText != null) {
            editText.setTextColor(getCurrentOrgColorInt());
        }
    }
    
    /**
     * Apply organization color to a View background
     */
    public void applyViewBackgroundColor(View view) {
        if (view != null) {
            view.setBackgroundColor(getCurrentOrgColorInt());
        }
    }
    
    /**
     * Apply organization color to a View background with gradient
     */
    public void applyViewBackgroundGradient(View view) {
        if (view != null) {
            android.graphics.drawable.GradientDrawable gradient = new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                new int[]{getCurrentOrgColorInt(), ColorUtils.hexToColor(ColorUtils.getDarkerColor(currentOrgColor))}
            );
            gradient.setCornerRadius(32); // 8dp in pixels
            view.setBackground(gradient);
        }
    }
    
    /**
     * Apply organization color to header/title text
     */
    public void applyHeaderColor(TextView headerText) {
        if (headerText != null) {
            headerText.setTextColor(getCurrentOrgColorInt());
        }
    }
    
    /**
     * Apply organization color to input fields
     */
    public void applyInputFieldColor(EditText inputField) {
        if (inputField != null) {
            inputField.setTextColor(getCurrentOrgColorInt());
            inputField.setHintTextColor(android.content.res.ColorStateList.valueOf(getCurrentOrgColorInt()));
        }
    }
    
    /**
     * Apply organization color to search fields
     */
    public void applySearchFieldColor(TextInputLayout searchLayout) {
        if (searchLayout != null) {
            searchLayout.setBoxStrokeColor(getCurrentOrgColorInt());
            searchLayout.setHintTextColor(android.content.res.ColorStateList.valueOf(getCurrentOrgColorInt()));
            searchLayout.setStartIconTintList(android.content.res.ColorStateList.valueOf(getCurrentOrgColorInt()));
        }
    }
    
    /**
     * Apply organization color to all UI elements in a fragment
     */
    public void applyThemeToFragment(View rootView) {
        if (rootView == null) return;
        
        // Apply to all TextViews
        applyThemeToTextViews(rootView);
        
        // Apply to all Buttons
        applyThemeToButtons(rootView);
        
        // Apply to all ImageViews
        applyThemeToImageViews(rootView);
        
        // Apply to all TextInputLayouts
        applyThemeToTextInputLayouts(rootView);
        
        // Apply to all EditTexts
        applyThemeToEditTexts(rootView);
        
        // Apply to all ProgressBars
        applyThemeToProgressBars(rootView);
    }
    
    private void applyThemeToTextViews(View rootView) {
        if (rootView instanceof TextView) {
            TextView textView = (TextView) rootView;
            // Apply to explicit title TextViews
            if (textView.getId() == R.id.tv_title) {
                applyHeaderColor(textView);
            } else {
                // Only apply to headers and important text, not all text
                if (textView.getId() == R.id.tv_app_name || 
                    textView.getText().toString().contains("Select Zone") ||
                    textView.getText().toString().contains("Enter License Plate")) {
                    applyTextColor(textView);
                }
            }
        }
        
        if (rootView instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) rootView;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                applyThemeToTextViews(viewGroup.getChildAt(i));
            }
        }
    }
    
    private void applyThemeToButtons(View rootView) {
        if (rootView instanceof Button || rootView instanceof MaterialButton) {
            if (rootView instanceof MaterialButton) {
                applyMaterialButtonColor((MaterialButton) rootView);
            } else {
                applyButtonColor((Button) rootView);
            }
        }
        
        if (rootView instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) rootView;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                applyThemeToButtons(viewGroup.getChildAt(i));
            }
        }
    }
    
    private void applyThemeToImageViews(View rootView) {
        if (rootView instanceof ImageView) {
            ImageView imageView = (ImageView) rootView;
            // Apply tint to icons
            if (imageView.getId() == R.id.btn_back ||
                imageView.getId() == R.id.settings_icon ||
                imageView.getId() == R.id.back_icon ||
                imageView.getId() == R.id.vehicle_icon) {
                applyImageViewTint(imageView);
            }
        }
        
        if (rootView instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) rootView;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                applyThemeToImageViews(viewGroup.getChildAt(i));
            }
        }
    }
    
    private void applyThemeToTextInputLayouts(View rootView) {
        if (rootView instanceof TextInputLayout) {
            applyTextInputLayoutColor((TextInputLayout) rootView);
        }
        
        if (rootView instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) rootView;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                applyThemeToTextInputLayouts(viewGroup.getChildAt(i));
            }
        }
    }
    
    private void applyThemeToEditTexts(View rootView) {
        if (rootView instanceof EditText) {
            applyEditTextColor((EditText) rootView);
        }
        
        if (rootView instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) rootView;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                applyThemeToEditTexts(viewGroup.getChildAt(i));
            }
        }
    }
    
    /**
     * Apply organization color to a ProgressBar's indeterminate tint
     */
    public void applyProgressBarTint(android.widget.ProgressBar progressBar) {
        if (progressBar != null) {
            progressBar.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(getCurrentOrgColorInt()));
        }
    }
    
    private void applyThemeToProgressBars(View rootView) {
        if (rootView instanceof android.widget.ProgressBar) {
            applyProgressBarTint((android.widget.ProgressBar) rootView);
        }
        
        if (rootView instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) rootView;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                applyThemeToProgressBars(viewGroup.getChildAt(i));
            }
        }
    }
    
    /**
     * Reset the theme to default colors
     */
    public void resetToDefaultTheme() {
        this.currentOrgColor = "#2196F3"; // Default blue color
    }
} 

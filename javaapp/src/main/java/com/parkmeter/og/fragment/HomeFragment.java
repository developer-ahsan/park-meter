package com.parkmeter.og.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.button.MaterialButton;
import com.parkmeter.og.NavigationListener;
import com.parkmeter.og.R;
import com.parkmeter.og.StripeTerminalApplication;
import com.parkmeter.og.model.AppState;
import com.parkmeter.og.model.Zone;
import com.parkmeter.og.model.Campaign;
import com.parkmeter.og.model.CampaignRequest;
import com.parkmeter.og.model.Language;
import com.parkmeter.og.network.Park45ApiClient;
import com.parkmeter.og.network.Park45ApiService;
import com.parkmeter.og.utils.AppThemeManager;
import com.parkmeter.og.utils.LiteralsHelper;
import com.parkmeter.og.utils.LanguageManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.card.MaterialCardView;

public class HomeFragment extends Fragment {

	public static final String TAG = "HomeFragment";

	private NavigationListener navigationListener;
	private AppState appState;

	private LinearLayout bannerLayout;
	private ImageView appLogo;
	private com.google.android.material.progressindicator.CircularProgressIndicator appLogoLoader;
	private TextView tvAppName;
	private TextView tvSelectedZoneDisplay;
	private MaterialButton enterVehicleButton;
	private ImageView settingsIcon;
	private com.google.android.material.card.MaterialCardView cardPrimaryAction;
	private com.google.android.material.card.MaterialCardView cardAppLogo;
	private com.google.android.material.card.MaterialCardView campaignCard;
	private com.google.android.material.progressindicator.CircularProgressIndicator campaignLoader;
	private android.webkit.WebView campaignWebView;
	
	// Language dropdown views
	private LinearLayout languageDropdown;
	private ImageView languageFlag;
	private TextView languageText;
	private java.util.List<Language> availableLanguages;

	public static HomeFragment newInstance() {
		return new HomeFragment();
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getActivity() instanceof NavigationListener) {
			navigationListener = (NavigationListener) getActivity();
		}
		appState = StripeTerminalApplication.getInstance().getAppState();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_home, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		initViews(view);
		setupClickListeners();
		initializeLanguages();
		updateUI();
	}

	private void initViews(View view) {
		bannerLayout = view.findViewById(R.id.banner_layout);
		appLogo = view.findViewById(R.id.app_logo);
		appLogoLoader = view.findViewById(R.id.app_logo_loader);
		tvAppName = view.findViewById(R.id.tv_app_name);
		tvSelectedZoneDisplay = view.findViewById(R.id.tv_selected_zone_display);
		enterVehicleButton = view.findViewById(R.id.btn_enter_vehicle_number);
		settingsIcon = view.findViewById(R.id.settings_icon);
		cardPrimaryAction = view.findViewById(R.id.card_primary_action);
		cardAppLogo = view.findViewById(R.id.card_app_logo);
		campaignCard = view.findViewById(R.id.campaign_card);
		campaignLoader = view.findViewById(R.id.campaign_loader);
		campaignWebView = view.findViewById(R.id.campaign_webview);
		
		// Initialize language dropdown views
		languageDropdown = view.findViewById(R.id.language_dropdown);
		languageFlag = view.findViewById(R.id.language_flag);
		languageText = view.findViewById(R.id.language_text);
	}

	private void setupClickListeners() {
		// Add null checks to prevent crashes
		if (enterVehicleButton != null) {
			enterVehicleButton.setOnClickListener(v -> {
				if (appState != null && appState.isZoneSelected()) {
					if (navigationListener != null) {
						navigationListener.onRequestVehicleNumber();
					}
				} else {
					if (navigationListener != null) {
						navigationListener.onRequestZonesSelection();
					}
				}
			});
		}

		if (settingsIcon != null) {
			settingsIcon.setOnClickListener(v -> {
				if (navigationListener != null) {
					navigationListener.onRequestSettings();
				}
			});
		}
		
		// Language dropdown click listener
		if (languageDropdown != null) {
			languageDropdown.setOnClickListener(v -> showLanguageDialog());
		}
	}
	
	private void loadCampaign(String zoneId) {
		if (campaignCard == null || campaignLoader == null || campaignWebView == null) {
			return;
		}
		
		// Show campaign card with loader immediately when API call starts
		campaignCard.setVisibility(View.VISIBLE);
		campaignLoader.setVisibility(View.VISIBLE);
		campaignWebView.setVisibility(View.GONE);
		
		// Set loader color dynamically and ensure it's spinning
		campaignLoader.setIndeterminate(true);
		campaignLoader.setIndicatorColor(AppThemeManager.getInstance().getCurrentOrgColorInt());
		campaignLoader.setProgress(0); // Reset progress to ensure spinning animation
		
		// Create campaign request
		CampaignRequest request = new CampaignRequest(zoneId);
		
		// Make API call
		Park45ApiService apiService = Park45ApiClient.getInstance().getApiService();
		apiService.getCurrentCampaign(request).enqueue(new Callback<java.util.List<Campaign>>() {
			@Override
			public void onResponse(Call<java.util.List<Campaign>> call, Response<java.util.List<Campaign>> response) {
				if (campaignCard == null || campaignLoader == null || campaignWebView == null) {
					return; // Fragment destroyed
				}
				
				if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
					Campaign campaign = response.body().get(0);
					String campaignHtml = campaign.getCampaign();
					
					// Only show content if we have actual content
					if (campaignHtml != null && !campaignHtml.trim().isEmpty()) {
						// Hide loader and show WebView with content
						displayCampaign(campaignHtml);
					} else {
						// No content - hide the card
						campaignCard.setVisibility(View.GONE);
					}
				} else {
					// No campaign data - hide the card
					campaignCard.setVisibility(View.GONE);
				}
			}
			
			@Override
			public void onFailure(Call<java.util.List<Campaign>> call, Throwable t) {
				if (campaignCard == null || campaignLoader == null) {
					return; // Fragment destroyed
				}
				
				// Hide campaign card on failure
				campaignCard.setVisibility(View.GONE);
			}
		});
	}
	
	private void displayCampaign(String campaignHtml) {
		if (campaignCard == null || campaignLoader == null || campaignWebView == null) {
			return;
		}
		
		// Hide loader and show WebView
		campaignLoader.setVisibility(View.GONE);
		campaignWebView.setVisibility(View.VISIBLE);
		
		// Configure WebView
		campaignWebView.getSettings().setJavaScriptEnabled(false);
		campaignWebView.getSettings().setDomStorageEnabled(false);
		campaignWebView.getSettings().setLoadWithOverviewMode(true);
		campaignWebView.getSettings().setUseWideViewPort(true);
		campaignWebView.getSettings().setBuiltInZoomControls(false);
		campaignWebView.getSettings().setDisplayZoomControls(false);
		campaignWebView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
		
		// Completely disable scrolling in WebView
		campaignWebView.setVerticalScrollBarEnabled(false);
		campaignWebView.setHorizontalScrollBarEnabled(false);
		campaignWebView.setScrollContainer(false);
		campaignWebView.setOverScrollMode(View.OVER_SCROLL_NEVER);
		campaignWebView.setScrollbarFadingEnabled(false);
		campaignWebView.setVerticalFadingEdgeEnabled(false);
		campaignWebView.setHorizontalFadingEdgeEnabled(false);
		
		// Disable touch scrolling
		campaignWebView.setOnTouchListener((v, event) -> true);
		
		// Set WebViewClient to handle loading
		campaignWebView.setWebViewClient(new android.webkit.WebViewClient() {
			@Override
			public void onPageFinished(android.webkit.WebView view, String url) {
				// Ensure no scrolling after page loads
				view.scrollTo(0, 0);
			}
		});
		
		// Create HTML content with responsive design and no scrolling
		String htmlContent = "<!DOCTYPE html><html><head>" +
				"<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, user-scalable=no\">" +
				"<style>" +
				"html, body { margin: 0; padding: 0; background: transparent; overflow: hidden; height: auto; max-height: none; }" +
				"img { max-width: 100%; height: auto; display: block; }" +
				"p { margin: 8px 0; }" +
				"* { -webkit-user-select: none; -moz-user-select: none; -ms-user-select: none; user-select: none; }" +
				"div { overflow: visible !important; }" +
				"</style></head><body>" +
				campaignHtml +
				"</body></html>";
		
		// Load HTML content
		campaignWebView.loadDataWithBaseURL("https://parkapp.ca", htmlContent, "text/html", "UTF-8", null);
	}

	private void updateUI() {
		if (appState.isZoneSelected()) {
			Zone selectedZone = appState.getSelectedZone();
			enterVehicleButton.setText(LiteralsHelper.getText(getContext(), "enter_license_plate_button"));
			tvSelectedZoneDisplay.setText(LiteralsHelper.getText(getContext(), "selected_zone") + " " + selectedZone.getZoneName());
			settingsIcon.setVisibility(View.VISIBLE);
			
			// Load campaign for selected zone
			loadCampaign(selectedZone.getId());
		} else {
			enterVehicleButton.setText(LiteralsHelper.getText(getContext(), "select_zone"));
			tvSelectedZoneDisplay.setText(LiteralsHelper.getText(getContext(), "no_zone_selected"));
			settingsIcon.setVisibility(View.GONE);
			
			// Hide campaign card when no zone is selected
			if (campaignCard != null) {
				campaignCard.setVisibility(View.GONE);
			}
		}
		
		// Ensure button text color is properly set after text change
		if (enterVehicleButton != null) {
			enterVehicleButton.setTextColor(android.graphics.Color.WHITE);
		}

		// Always show "Parkapp Meter" in the banner
		tvAppName.setText(LiteralsHelper.getText(getContext(), "park45_meter"));
		
		// Update organization color in theme manager
		String orgColor = appState.getOrganizationColor();
		AppThemeManager.getInstance().updateOrganizationColor(orgColor);
		
		// Apply app-wide theming
		if (getView() != null) {
			AppThemeManager.getInstance().applyThemeToFragment(getView());
		}
		
		// Update banner background color with gradient
		if (bannerLayout != null) {
			AppThemeManager.getInstance().applyViewBackgroundGradient(bannerLayout);
			// Always use white text for "Parkapp Meter" in banner
			tvAppName.setTextColor(android.graphics.Color.WHITE);
		}
		
		// Update card stroke color
		if (cardPrimaryAction != null) {
			cardPrimaryAction.setStrokeColor(AppThemeManager.getInstance().getCurrentOrgColorInt());
		}
		if (cardAppLogo != null) {
			cardAppLogo.setStrokeColor(AppThemeManager.getInstance().getCurrentOrgColorInt());
		}
		if (campaignCard != null) {
			campaignCard.setStrokeColor(AppThemeManager.getInstance().getCurrentOrgColorInt());
		}
		
		// Update language dropdown colors with dynamic organization color
		if (languageText != null) {
			languageText.setTextColor(AppThemeManager.getInstance().getCurrentOrgColorInt());
		}
		if (languageFlag != null) {
			// The flag drawable will maintain its colors, but we can tint if needed
			// For now, just ensure the text color matches the organization theme
		}
		
		// Load organization logo
		loadOrganizationLogo();
	}

	@Override
	public void onResume() {
		super.onResume();
		
		// Re-establish navigation listener in case it was cleared
		if (navigationListener == null && getActivity() instanceof NavigationListener) {
			navigationListener = (NavigationListener) getActivity();
		}
		
		// Re-setup click listeners in case views were recreated
		if (getView() != null) {
			setupClickListeners();
		}
		
		updateUI();
		
		// Force refresh the logo when returning from zone selection
		loadOrganizationLogo();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		// Clear view references to prevent memory leaks
		bannerLayout = null;
		appLogo = null;
		appLogoLoader = null;
		tvAppName = null;
		tvSelectedZoneDisplay = null;
		enterVehicleButton = null;
		settingsIcon = null;
		cardPrimaryAction = null;
		cardAppLogo = null;
		campaignCard = null;
		campaignLoader = null;
		if (campaignWebView != null) {
			campaignWebView.destroy();
			campaignWebView = null;
		}
		
		// Clear language dropdown references
		languageDropdown = null;
		languageFlag = null;
		languageText = null;
		availableLanguages = null;
		
		// Clear navigation listener to prevent memory leaks
		navigationListener = null;
	}
	
	private void loadOrganizationLogo() {
		// Load organization logo if available
		String logoUrl = appState.getOrganizationLogoUrl();
		
		if (appLogo != null) {
			if (logoUrl != null && !logoUrl.isEmpty()) {
				// Show loader with dynamic color
				if (appLogoLoader != null) {
					appLogoLoader.bringToFront();
					appLogoLoader.setIndeterminate(true);
					appLogoLoader.setIndicatorColor(AppThemeManager.getInstance().getCurrentOrgColorInt());
					appLogoLoader.setVisibility(View.VISIBLE);
				}
				
				Glide.with(this)
						.load(logoUrl)
						.diskCacheStrategy(DiskCacheStrategy.NONE)
						.skipMemoryCache(true)
						.into(new com.bumptech.glide.request.target.CustomTarget<android.graphics.drawable.Drawable>() {
							@Override
							public void onResourceReady(@NonNull android.graphics.drawable.Drawable resource, @Nullable com.bumptech.glide.request.transition.Transition<? super android.graphics.drawable.Drawable> transition) {
								appLogo.setImageDrawable(resource);
								if (appLogoLoader != null) appLogoLoader.setVisibility(View.GONE);
							}

							@Override
							public void onLoadCleared(@Nullable android.graphics.drawable.Drawable placeholder) {
								if (appLogoLoader != null) appLogoLoader.setVisibility(View.GONE);
							}

							@Override
							public void onLoadFailed(@Nullable android.graphics.drawable.Drawable errorDrawable) {
								if (appLogoLoader != null) appLogoLoader.setVisibility(View.GONE);
								// show placeholder only when failed
								int color = AppThemeManager.getInstance().getCurrentOrgColorInt();
								appLogo.setImageDrawable(new android.graphics.drawable.ColorDrawable(color));
							}
						});
			} else {
				if (appLogoLoader != null) appLogoLoader.setVisibility(View.GONE);
				appLogo.setImageResource(R.drawable.ic_app_logo);
			}
		}
	}
	
	private void initializeLanguages() {
		availableLanguages = new java.util.ArrayList<>();
		availableLanguages.add(new Language("en", LiteralsHelper.getText(getContext(), "english"), R.drawable.ic_flag_english));
		availableLanguages.add(new Language("fr", LiteralsHelper.getText(getContext(), "french"), R.drawable.ic_flag_french));
		
		// Update language display based on saved selection
		updateLanguageDisplay();
	}
	
	private void updateLanguageDisplay() {
		String currentLanguageCode = LanguageManager.getCurrentLanguage(requireContext());
		Language currentLanguage = getLanguageByCode(currentLanguageCode);
		
		if (currentLanguage != null && languageFlag != null && languageText != null) {
			languageFlag.setImageResource(currentLanguage.getFlagResourceId());
			languageText.setText(currentLanguage.getName());
			
			// Apply dynamic organization color to the text
			languageText.setTextColor(AppThemeManager.getInstance().getCurrentOrgColorInt());
		}
	}
	
	private Language getLanguageByCode(String code) {
		for (Language language : availableLanguages) {
			if (language.getCode().equals(code)) {
				return language;
			}
		}
		return availableLanguages.get(0); // Default to English
	}
	
	private void showLanguageDialog() {
		// Use WeakReference to prevent memory leaks
		android.content.Context context = requireContext();
		android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
		builder.setTitle(LiteralsHelper.getText(getContext(), "select_language"));
		
		String[] languageNames = new String[availableLanguages.size()];
		for (int i = 0; i < availableLanguages.size(); i++) {
			languageNames[i] = availableLanguages.get(i).getName();
		}
		
		String currentLanguageCode = LanguageManager.getCurrentLanguage(context);
		int currentIndex = 0;
		for (int i = 0; i < availableLanguages.size(); i++) {
			if (availableLanguages.get(i).getCode().equals(currentLanguageCode)) {
				currentIndex = i;
				break;
			}
		}
		
		// Use WeakReference for the fragment to prevent memory leaks
		java.lang.ref.WeakReference<HomeFragment> fragmentRef = new java.lang.ref.WeakReference<>(this);
		
		builder.setSingleChoiceItems(languageNames, currentIndex, (dialog, which) -> {
			HomeFragment fragment = fragmentRef.get();
			if (fragment == null || !fragment.isAdded()) {
				dialog.dismiss();
				return;
			}
			
			Language selectedLanguage = availableLanguages.get(which);
			String newLanguageCode = selectedLanguage.getCode();
			
			// Check if language actually changed
			if (LanguageManager.isLanguageChanged(context, newLanguageCode)) {
				// Set the new language
				LanguageManager.setLanguage(context, newLanguageCode);
				
				// Update app state
				fragment.appState.setSelectedLanguageCode(newLanguageCode);
				
				// Update the display immediately
				fragment.updateLanguageDisplay();
				
				// Recreate the activity to apply language changes immediately
				android.app.Activity activity = fragment.getActivity();
				if (activity != null && !activity.isFinishing()) {
					activity.recreate();
				}
			}
			
			dialog.dismiss();
		});
		
		builder.setNegativeButton(LiteralsHelper.getText(getContext(), "cancel"), (dialog, which) -> dialog.dismiss());
		
		android.app.AlertDialog dialog = builder.create();
		dialog.show();
	}
} 

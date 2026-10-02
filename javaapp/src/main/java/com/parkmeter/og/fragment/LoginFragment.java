package com.parkmeter.og.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.parkmeter.og.NavigationListener;
import com.parkmeter.og.R;
import com.parkmeter.og.StripeTerminalApplication;
import com.parkmeter.og.model.AgentLoginRequest;
import com.parkmeter.og.model.AgentLoginResponse;
import com.parkmeter.og.model.AppState;
import com.parkmeter.og.network.Park45ApiClient;
import com.parkmeter.og.utils.AppThemeManager;
import com.parkmeter.og.utils.LanguageManager;
import com.parkmeter.og.utils.SharedPreferencesManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginFragment extends Fragment {

    public static final String TAG = "LoginFragment";

    private NavigationListener navigationListener;
    private SharedPreferencesManager prefsManager;
    private AppState appState;

    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private CheckBox cbRememberMe;
    private MaterialButton btnSignIn;
    private FrameLayout loadingOverlay;

    private Call<AgentLoginResponse> loginCall;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity() instanceof NavigationListener) {
            navigationListener = (NavigationListener) getActivity();
        }
        prefsManager = new SharedPreferencesManager(requireContext());
        appState = StripeTerminalApplication.getInstance().getAppState();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etEmail = view.findViewById(R.id.et_email);
        etPassword = view.findViewById(R.id.et_password);
        cbRememberMe = view.findViewById(R.id.cb_remember_me);
        btnSignIn = view.findViewById(R.id.btn_sign_in);
        loadingOverlay = view.findViewById(R.id.login_loading_overlay);

        // Pre-fill email if remember-me was set
        if (prefsManager.isRememberMe()) {
            String lastEmail = prefsManager.getLastEmail();
            if (lastEmail != null) {
                etEmail.setText(lastEmail);
                cbRememberMe.setChecked(true);
            }
        }

        btnSignIn.setOnClickListener(v -> attemptLogin());

        // Allow done key on password to trigger login
        etPassword.setOnEditorActionListener((v, actionId, event) -> {
            attemptLogin();
            return true;
        });
    }

    private void attemptLogin() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";

        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        boolean rememberMe = cbRememberMe.isChecked();
        String language = appState.getSelectedLanguageCode();
        if (language == null) language = "en";

        setLoading(true);

        AgentLoginRequest request = new AgentLoginRequest(language, rememberMe, email, password);

        loginCall = Park45ApiClient.getInstance().getApiService().agentLogin(request);
        loginCall.enqueue(new Callback<AgentLoginResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<AgentLoginResponse> call,
                                           @NonNull Response<AgentLoginResponse> response) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            setLoading(false);
                            if (response.isSuccessful() && response.body() != null
                                    && response.body().isAuth()) {
                                AgentLoginResponse body = response.body();
                                AgentLoginResponse.AgentUser user = body.getResult();
                                String token = body.getToken();

                                prefsManager.saveLoginSession(user, token, rememberMe, email);
                                appState.setLoggedInUser(user);
                                appState.setAuthToken(token);

                                if (user.getOrg() != null && user.getOrg().getColor() != null
                                        && !user.getOrg().getColor().isEmpty()) {
                                    AppThemeManager.getInstance().updateOrganizationColor(user.getOrg().getColor());
                                }

                                if (navigationListener != null) {
                                    navigationListener.onLoginSuccess();
                                }
                            } else {
                                Toast.makeText(getContext(),
                                        "Invalid email or password. Please try again.",
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                    @Override
                    public void onFailure(@NonNull Call<AgentLoginResponse> call, @NonNull Throwable t) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            setLoading(false);
                            Toast.makeText(getContext(),
                                    "Network error: " + t.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    private void setLoading(boolean loading) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (btnSignIn != null) {
            btnSignIn.setEnabled(!loading);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (loginCall != null) {
            loginCall.cancel();
            loginCall = null;
        }
        etEmail = null;
        etPassword = null;
        cbRememberMe = null;
        btnSignIn = null;
        loadingOverlay = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        navigationListener = null;
    }
}

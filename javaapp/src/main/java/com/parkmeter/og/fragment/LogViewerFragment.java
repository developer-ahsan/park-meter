package com.parkmeter.og.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.parkmeter.og.R;
import com.parkmeter.og.NavigationListener;
import com.parkmeter.og.utils.LiteralsHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LogViewerFragment extends Fragment {

    public static final String TAG = "com.parkmeter.og.fragment.LogViewerFragment";
    private TextView logTextView;
    private Button refreshButton;
    private Button clearButton;
    private Button filterButton;
    private Button searchButton;
    private EditText searchEditText;
    private ScrollView scrollView;
    private boolean showFilteredLogs = false;
    private String searchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_log_viewer, container, false);
        
        logTextView = view.findViewById(R.id.log_text_view);
        refreshButton = view.findViewById(R.id.refresh_button);
        clearButton = view.findViewById(R.id.clear_button);
        filterButton = view.findViewById(R.id.filter_button);
        searchButton = view.findViewById(R.id.search_button);
        searchEditText = view.findViewById(R.id.search_edit_text);
        scrollView = view.findViewById(R.id.log_scroll_view);
        
        // Setup home button
        view.findViewById(R.id.home_button).setOnClickListener(v -> {
            if (getActivity() instanceof NavigationListener) {
                ((NavigationListener) getActivity()).onRequestExitWorkflow();
            }
        });

        setupButtons();
        loadLogs();
        
        return view;
    }

    private void setupButtons() {
        refreshButton.setOnClickListener(v -> {
            loadLogs();
            scrollToBottom();
        });

        clearButton.setOnClickListener(v -> {
            logTextView.setText("");
            scrollToBottom();
        });

        filterButton.setOnClickListener(v -> {
            showFilteredLogs = !showFilteredLogs;
            filterButton.setText(showFilteredLogs ? LiteralsHelper.getText(getContext(), "show_all") : LiteralsHelper.getText(getContext(), "filter_discovery_button"));
            loadLogs();
            scrollToBottom();
        });

        // Setup search functionality
        searchButton.setOnClickListener(v -> {
            searchQuery = searchEditText.getText().toString().trim();
            loadLogs();
            scrollToBottom();
        });

        // Search on Enter key
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                searchQuery = searchEditText.getText().toString().trim();
                loadLogs();
                scrollToBottom();
                return true;
            }
            return false;
        });
    }

    private void loadLogs() {
        try {
            Process process = Runtime.getRuntime().exec("logcat -d");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            
            List<String> logs = new ArrayList<>();
            String line;
            
            while ((line = bufferedReader.readLine()) != null) {
                boolean shouldAdd = false;
                
                // Apply discovery filter
                if (showFilteredLogs) {
                    if (line.contains("DiscoveryFragment") || 
                        line.contains("TAP_TO_PAY") || 
                        line.contains("discovery") ||
                        line.contains("Terminal") ||
                        line.contains("MainActivity") ||
                        line.contains("error") ||
                        line.contains("exception")) {
                        shouldAdd = true;
                    }
                } else {
                    shouldAdd = true;
                }
                
                // Apply search filter
                if (shouldAdd && !searchQuery.isEmpty()) {
                    shouldAdd = line.toLowerCase().contains(searchQuery.toLowerCase());
                }
                
                if (shouldAdd) {
                    logs.add(line);
                }
            }
            
            bufferedReader.close();
            
            // Take last 1000 lines to avoid memory issues
            int startIndex = Math.max(0, logs.size() - 1000);
            List<String> recentLogs = logs.subList(startIndex, logs.size());
            
            StringBuilder logBuilder = new StringBuilder();
            logBuilder.append(LiteralsHelper.getText(getContext(), "log_viewer_header")).append("\n");
            logBuilder.append(LiteralsHelper.getText(getContext(), "timestamp_label")).append(" ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n");
            logBuilder.append(LiteralsHelper.getText(getContext(), "total_logs_label")).append(" ").append(logs.size()).append(" (").append(LiteralsHelper.getText(getContext(), "showing_last_label")).append(" ").append(recentLogs.size()).append(")\n");
            logBuilder.append(LiteralsHelper.getText(getContext(), "filter_label")).append(" ").append(showFilteredLogs ? LiteralsHelper.getText(getContext(), "discovery_only") : LiteralsHelper.getText(getContext(), "all_logs")).append("\n");
            if (!searchQuery.isEmpty()) {
                logBuilder.append(LiteralsHelper.getText(getContext(), "search_label")).append(" '").append(searchQuery).append("'\n");
            }
            logBuilder.append(LiteralsHelper.getText(getContext(), "log_separator")).append("\n\n");
            
            for (String logLine : recentLogs) {
                logBuilder.append(logLine).append("\n");
            }
            
            logTextView.setText(logBuilder.toString());
            
        } catch (IOException e) {
            logTextView.setText(LiteralsHelper.getText(getContext(), "error_loading_logs").replace("%1$s", e.getMessage()));
        }
    }

    private void scrollToBottom() {
        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }

    @Override
    public void onResume() {
        super.onResume();
        loadLogs();
    }
} 

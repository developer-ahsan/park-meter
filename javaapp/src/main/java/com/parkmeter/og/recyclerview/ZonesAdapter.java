package com.parkmeter.og.recyclerview;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.parkmeter.og.R;
import com.parkmeter.og.model.Zone;
import com.parkmeter.og.utils.AppThemeManager;
import com.parkmeter.og.utils.LiteralsHelper;

import java.util.ArrayList;
import java.util.List;

public class ZonesAdapter extends RecyclerView.Adapter<ZonesAdapter.ZoneViewHolder> {
    
    private List<Zone> allZones;
    private List<Zone> filteredZones;
    private OnZoneClickListener listener;
    private String selectedZoneId;

    public interface OnZoneClickListener {
        void onZoneClick(Zone zone);
    }

    public ZonesAdapter(OnZoneClickListener listener) {
        this.listener = listener;
        this.allZones = new ArrayList<>();
        this.filteredZones = new ArrayList<>();
    }

    @NonNull
    @Override
    public ZoneViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_zone, parent, false);
        return new ZoneViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ZoneViewHolder holder, int position) {
        Zone zone = filteredZones.get(position);
        holder.bind(zone);
    }

    @Override
    public int getItemCount() {
        return filteredZones.size();
    }

    public void setZones(List<Zone> zones) {
        this.allZones = new ArrayList<>(zones);
        this.filteredZones = new ArrayList<>(zones);
        notifyDataSetChanged();
    }

    public void setSelectedZoneId(String zoneId) {
        this.selectedZoneId = zoneId;
        // Reorder to bring selected zone to the front if present
        reorderSelectedToFront();
        notifyDataSetChanged();
    }

    public void filterZones(String query) {
        filteredZones.clear();
        if (query == null || query.trim().isEmpty()) {
            filteredZones.addAll(allZones);
        } else {
            String lowerCaseQuery = query.toLowerCase().trim();
            for (Zone zone : allZones) {
                try {
                    // Check zone name (null-safe)
                    boolean matchesZoneName = zone.getZoneName() != null && 
                        zone.getZoneName().toLowerCase().contains(lowerCaseQuery);
                    
                    // Check city name (null-safe)
                    boolean matchesCityName = zone.getCity() != null && 
                        zone.getCity().getCityName() != null && 
                        zone.getCity().getCityName().toLowerCase().contains(lowerCaseQuery);
                    
                    // Check zone code (null-safe)
                    boolean matchesZoneCode = zone.getZoneCode() != null && 
                        zone.getZoneCode().toLowerCase().contains(lowerCaseQuery);
                    
                    if (matchesZoneName || matchesCityName || matchesZoneCode) {
                        filteredZones.add(zone);
                    }
                } catch (Exception e) {
                    // Error processing zone
                }
            }
        }
        // Reorder to bring selected zone to the top in filtered results
        reorderSelectedToFront();
        notifyDataSetChanged();
    }

    private void reorderSelectedToFront() {
        if (selectedZoneId == null || filteredZones.isEmpty()) return;
        for (int i = 0; i < filteredZones.size(); i++) {
            Zone z = filteredZones.get(i);
            if (selectedZoneId.equals(z.getId())) {
                if (i != 0) {
                    filteredZones.remove(i);
                    filteredZones.add(0, z);
                }
                break;
            }
        }
    }

    class ZoneViewHolder extends RecyclerView.ViewHolder {
        private TextView tvZoneName;
        private TextView tvCityName;
        private TextView tvZoneCode;
        private ImageView ivSelected;
        private ImageView ivZoneIcon;

        public ZoneViewHolder(@NonNull View itemView) {
            super(itemView);
            tvZoneName = itemView.findViewById(R.id.tv_zone_name);
            tvCityName = itemView.findViewById(R.id.tv_city_name);
            tvZoneCode = itemView.findViewById(R.id.tv_zone_code);
            ivSelected = itemView.findViewById(R.id.iv_selected);
            ivZoneIcon = itemView.findViewById(R.id.iv_zone_icon);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    Zone selectedZone = filteredZones.get(position);
                    
                    if (selectedZone.getCity() != null && selectedZone.getCity().getCityName() != null) {
                    } else {
                    }
                    
                    if (selectedZone.getOrganization() != null) {
                    } else {
                    }
                    
                    listener.onZoneClick(selectedZone);
                }
            });
        }

        public void bind(Zone zone) {
            // Null-safe zone name
            tvZoneName.setText(zone.getZoneName() != null ? zone.getZoneName() : LiteralsHelper.getText(itemView.getContext(), "unknown_city"));
            
            // Null-safe city name
            if (zone.getCity() != null && zone.getCity().getCityName() != null) {
                tvCityName.setText(zone.getCity().getCityName());
            } else {
                tvCityName.setText(LiteralsHelper.getText(itemView.getContext(), "unknown_city"));
            }
            
            // Null-safe zone code
            tvZoneCode.setText(LiteralsHelper.getText(itemView.getContext(), "code_label").replace("%1$s", zone.getZoneCode() != null ? zone.getZoneCode() : LiteralsHelper.getText(itemView.getContext(), "na")));

            // Apply dynamic org color to icons and code text
            int orgColorInt = AppThemeManager.getInstance().getCurrentOrgColorInt();
            if (ivSelected != null) {
                ivSelected.setColorFilter(orgColorInt);
            }
            if (ivZoneIcon != null) {
                ivZoneIcon.setColorFilter(orgColorInt);
            }
            if (tvZoneCode != null) {
                tvZoneCode.setTextColor(orgColorInt);
            }

            // Also apply card stroke color
            if (itemView instanceof com.google.android.material.card.MaterialCardView) {
                ((com.google.android.material.card.MaterialCardView) itemView).setStrokeColor(orgColorInt);
            }

            // Show selection indicator if this zone is selected
            if (selectedZoneId != null && selectedZoneId.equals(zone.getId())) {
                ivSelected.setVisibility(View.VISIBLE);
                
                // Keep simple: do not change background/text colors
                itemView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                ivSelected.setColorFilter(orgColorInt);
            } else {
                ivSelected.setVisibility(View.GONE);
                
                // Reset to default appearance
                itemView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            }
        }
    }
} 

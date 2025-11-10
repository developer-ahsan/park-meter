package com.parkmeter.og.fragment.offline;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.parkmeter.og.R;
import com.parkmeter.og.model.Event;
import com.parkmeter.og.model.OfflineLog;

import org.jetbrains.annotations.NotNull;

/**
 * A simple [RecyclerView.ViewHolder] that displays various offline payments logs
 */
public class OfflinePaymentsLogHolder extends RecyclerView.ViewHolder {

    public OfflinePaymentsLogHolder(@NotNull View itemView) {
        super(itemView);
    }

    public void bind(@NotNull OfflineLog log) {
        ((TextView) itemView.findViewById(R.id.message)).setText(log.toMessage());
    }
}

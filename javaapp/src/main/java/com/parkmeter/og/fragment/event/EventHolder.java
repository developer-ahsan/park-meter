package com.parkmeter.og.fragment.event;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.parkmeter.og.R;
import com.parkmeter.og.model.Event;

import org.jetbrains.annotations.NotNull;

/**
 * A simple [RecyclerView.ViewHolder] that displays various events
 */
public class EventHolder extends RecyclerView.ViewHolder {

    public EventHolder(
        @NotNull View itemView
    ) {
        super(itemView);
    }

    public void bind(@NotNull Event event) {
        ((TextView) itemView.findViewById(R.id.method)).setText(event.getMethod());
        ((TextView) itemView.findViewById(R.id.message)).setText(event.getMessage());
    }
}

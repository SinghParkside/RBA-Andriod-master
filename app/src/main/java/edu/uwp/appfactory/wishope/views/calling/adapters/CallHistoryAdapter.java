package edu.uwp.appfactory.wishope.views.calling.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import edu.uwp.appfactory.wishope.R;
import edu.uwp.appfactory.wishope.utils.UserConstants;

/**
 * <h1>Adapter for the Call History RecyclerView</h1>
 * This calls inflates the Call History RecyclerView using the callHistory LinkedHashSet. Converts
 * the dateTime from the callHistory Map to the format `MMM. d yyyy` and call length to the format
 * `[0-9]*h [0-9]*m [0-9]*s`.
 *
 * @author Allen Rocha
 * @version 1.0
 * @since 03-01-2021
 */
public class CallHistoryAdapter extends RecyclerView.Adapter<CallHistoryAdapter.CallHistoryViewHolder> {
    private final List<Map<String, Object>> callHistory;
    private OnItemClickListener itemClickListener;

    public CallHistoryAdapter() {
        // Initialize the callHistory List using the LinkedHashSet.
        this.callHistory = new ArrayList<>(UserConstants.callHistory);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        itemClickListener = listener;
    }

    @NonNull
    @Override
    public CallHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.call_history_item, parent, false);
        return new CallHistoryViewHolder(v, itemClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull CallHistoryViewHolder holder, int position) {
        // Get the Map from the callHistory List at the current position of the RecyclerView.
        Map<String, Object> currentItem = callHistory.get(position);
        // Get the inner map containing the information of the other user.
        Map<String, String> otherUser = (Map<String, String>) currentItem.get("otherCaller");
        // Set the UI element of item in the RecyclerView at the current position using the callHistory List.
        holder.callHistoryNameTextView.setText(String.format("%s %s", otherUser.get("firstName"), otherUser.get("lastName")));
        holder.callHistoryTimeTextView.setText(generateCallLength(currentItem.get("length").toString()));
        holder.callHistoryDateTextView.setText(generateDateTime(currentItem.get("dateTime").toString()));
    }

    @Override
    public int getItemCount() {
        return callHistory.size();
    }

    /**
     * This function takes the dateTime value from a callHistory Map and converts it to the format
     * of `MMM. d yyyy`.
     *
     * @param dateTime the date and time that the call ended
     * @return String of format `MMM. d yyyy`
     */
    private String generateDateTime(final String dateTime) {
        try {
            final Date parsedDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss z", Locale.US).parse(dateTime);
            final String pattern = "MMM. d yyyy";
            return new SimpleDateFormat(pattern, Locale.US).format(parsedDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * This function takes the call length value from a callHistory Map and converts it to the format
     * of `[0-9]*h [0-9]*m [0-9]*s`.
     *
     * @param callLength the length that the call took in ms.
     * @return String of the format `[0-9]*h [0-9]*m [0-9]*s`.
     */
    private String generateCallLength(final String callLength) {
        int totalSeconds = (int) (Double.parseDouble(callLength) / 1000.0);
        final int hours = (int) (totalSeconds / 3600.0);
        final int minutes = (int) ((totalSeconds % 3600.0) / 60.0);
        final int seconds = (int) (totalSeconds % 60.0);
        if (hours < 1 && minutes < 1)
            return String.format("%ds", seconds);
        else if (hours < 1 && minutes > 0)
            return String.format("%dm %ds", minutes, seconds);
        else
            return String.format("%dh %dm %ds", hours, minutes, seconds);
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public static class CallHistoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView callHistoryNameTextView;
        private final TextView callHistoryTimeTextView;
        private final TextView callHistoryDateTextView;

        public CallHistoryViewHolder(@NonNull View itemView, OnItemClickListener listener) {
            super(itemView);
            callHistoryNameTextView = itemView.findViewById(R.id.callHistoryNameTextView);
            callHistoryTimeTextView = itemView.findViewById(R.id.callHistoryTimeTextView);
            callHistoryDateTextView = itemView.findViewById(R.id.callHistoryDateTextView);
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onItemClick(position);
                    }
                }
            });
        }
    }
}

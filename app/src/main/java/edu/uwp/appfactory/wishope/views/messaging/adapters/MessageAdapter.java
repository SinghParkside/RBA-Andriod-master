package edu.uwp.appfactory.wishope.views.messaging.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import edu.uwp.appfactory.wishope.R;
import edu.uwp.appfactory.wishope.utils.UserConstants;
import edu.uwp.appfactory.wishope.views.messaging.items.MessageItem;

/**
 * <h1>.</h1>
 * <p>
 * </p>
 *
 * @author Nick Apicelli
 * @version 1.9.5
 * @since 04-12-2020
 */
public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private OnItemClickListener itemClickListener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        itemClickListener = listener;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_user_item, parent, false);
        return new MessageViewHolder(v, itemClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        MessageItem currentItem = UserConstants.RECENT_CONVERSATIONS.get(UserConstants.RECENT_CONVERSATIONS.keySet().toArray()[position]);
        final String otherUID = currentItem
                .getConversationId()
                .replace(
                        UserConstants.UID,
                        ""
                );
        String lastMessageDate = deltaTime(currentItem.getLastDate());
        //stops duplication
        if (currentItem.getProfilePicture() != null)
            holder.imageProfileView.setImageBitmap(currentItem.getProfilePicture());
        holder.textNameView.setText(currentItem.getFullName());
        if (currentItem.getSender().equals(otherUID)) {
            holder.textMessageView.setText(
                    String.format(
                            "%s: %s",
                            currentItem.getFullName(),
                            currentItem.getLastMessage()
                    )
            );
            if (!currentItem.isRead())
                holder.unreadMessageCircle.setVisibility(View.VISIBLE);
        } else
            holder.textMessageView.setText(
                    String.format(
                            "You: %s",
                            currentItem.getLastMessage()
                    )
            );
        holder.textLastMessageDateView.setText(lastMessageDate);
    }

    @Override
    public int getItemCount() {
        return UserConstants.RECENT_CONVERSATIONS.size();
    }

    private String deltaTime(String time) {
        try {
            Date messageDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss z", Locale.US).parse(time);
            Date today = new Date();
            long diff = today.getTime() - messageDate.getTime();
            int numOfYears = (int) (diff / (1000 * 60 * 60 * 24 * 7 * 52));
            int numOfMonths = (int) (diff / (1000 * 60 * 60 * 24 * 7 * 4));
            int numOfWeeks = (int) (diff / (1000 * 60 * 60 * 24 * 7));
            int numOfDays = (int) (diff / (1000 * 60 * 60 * 24));
            int hours = (int) (diff / (1000 * 60 * 60));
            int minutes = (int) (diff / (1000 * 60));
            int seconds = (int) (diff / (1000));

            if (numOfYears > 0)
                return String.format(Locale.getDefault(), "%dy", numOfYears);
            if (numOfMonths > 0)
                return String.format(Locale.getDefault(), "%dm", numOfMonths);
            if (numOfWeeks > 0)
                return String.format(Locale.getDefault(), "%dw", numOfWeeks);
            if (numOfDays > 0)
                return String.format(Locale.getDefault(), "%dd", numOfDays);
            if (hours > 0)
                return String.format(Locale.getDefault(), "%dh", hours);
            if (minutes > 0)
                return String.format(Locale.getDefault(), "%dm", minutes);
            if (seconds > 0)
                return String.format(Locale.getDefault(), "%ds", seconds);

        } catch (ParseException e) {
            e.printStackTrace();
        }
        return "";
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        public ImageView imageProfileView;
        public ImageView unreadMessageCircle;
        public TextView textNameView;
        public TextView textMessageView;
        public TextView textLastMessageDateView;

        public MessageViewHolder(@NonNull View itemView, OnItemClickListener listener) {
            super(itemView);
            imageProfileView = itemView.findViewById(R.id.imageProfileView);
            unreadMessageCircle = itemView.findViewById(R.id.unreadMessageCircle);
            textNameView = itemView.findViewById(R.id.textNameView);
            textMessageView = itemView.findViewById(R.id.textMessageView);
            textLastMessageDateView = itemView.findViewById(R.id.textLastMessageDateView);
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

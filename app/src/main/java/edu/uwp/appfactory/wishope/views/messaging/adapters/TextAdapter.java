package edu.uwp.appfactory.wishope.views.messaging.adapters;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import edu.uwp.appfactory.wishope.R;
import edu.uwp.appfactory.wishope.utils.UserConstants;
import edu.uwp.appfactory.wishope.views.messaging.TextActivity;
import edu.uwp.appfactory.wishope.views.messaging.items.TextMessageItem;

/**
 * <h1>.</h1>
 * <p>
 * </p>
 *
 * @author Nick Apicelli
 * @version 1.9.5
 * @since 04-12-2020
 */
public class TextAdapter extends RecyclerView.Adapter<TextAdapter.TextViewHolder> {
    public static int lastMessageIndex;
    private final String TAG = "TextAdapter";
    private final List<TextMessageItem> textMessageItems;
    private Context context;

    public TextAdapter(List<TextMessageItem> textMessageItems) {
        this.textMessageItems = textMessageItems;
        getUsersLastSentMessage();
    }


    private void getUsersLastSentMessage() {
        lastMessageIndex = 0;
        List<TextMessageItem> auxTextMessageItems = new ArrayList<>(textMessageItems);
        Collections.reverse(auxTextMessageItems);
        for (int i = 0; i < auxTextMessageItems.size(); i++) {
            TextMessageItem item = auxTextMessageItems.get(i);
            if (item.getSender().equals(UserConstants.UID))
                lastMessageIndex = i;
        }
    }

    @NonNull
    @Override
    public TextAdapter.TextViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.text_bubble_item, parent, false);
        context = parent.getContext();
        return new TextViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TextAdapter.TextViewHolder holder, int position) {
        TextMessageItem currentItem = textMessageItems.get(textMessageItems.size() - position - 1);
        String delta = "";
        if (!currentItem.getDate().equals("Now"))
            delta = deltaTime(currentItem.getDate());
        else
            delta = "Now";
        // Current user
        if (currentItem.getSender().equals(UserConstants.UID)) {
            Log.d(TAG, "onBindViewHolder: lastMessageIndex: " + lastMessageIndex);
            if (position == lastMessageIndex) {
                if (position != (textMessageItems.size() - 1) || position == (textMessageItems.size() - 1) && UserConstants.RECENT_CONVERSATIONS.get(TextActivity.conversationId) != null && UserConstants.RECENT_CONVERSATIONS.get(TextActivity.conversationId).isRead())
                    holder.sentReceivedCheck.setImageDrawable(context.getDrawable(R.drawable.ic_checkmark_double));
                holder.sentReceivedCheck.setVisibility(View.VISIBLE);
                holder.sentReceivedCheck.setColorFilter(Color.argb(255, 255, 255, 255));
            } else {
                holder.sentReceivedCheck.setVisibility(View.GONE); //Possibly stops duplication of checks in messaging
            }
            holder.theirTextMessageBubbleCardView.setVisibility(View.GONE);
            holder.yourTextMessageBubbleCardView.setVisibility(View.VISIBLE);
            holder.yourTextMessageTextView.setText(currentItem.getTextMessage());
            holder.yourTextMessageDateTextView.setText(delta);
            holder.yourTextMessageBubbleCardView.setBackground(ContextCompat.getDrawable(context, R.drawable.transparent));
            holder.yourTextMessageBubbleCardView.setBackgroundColor(ContextCompat.getColor(context, R.color.yellow_msg_bubble));
        } else {
            if (position == (textMessageItems.size() - 1) && !UserConstants.RECENT_CONVERSATIONS.get(TextActivity.conversationId).isRead())
                FirebaseFirestore
                        .getInstance()
                        .collection("users")
                        .document(currentItem.getSender())
                        .collection("conversations")
                        .document(TextActivity.conversationId)
                        .update("read", true)
                        .addOnCompleteListener(updateReadMessageTask -> {
                            if (updateReadMessageTask.isSuccessful())
                                FirebaseFirestore
                                        .getInstance()
                                        .collection("users")
                                        .document(UserConstants.UID)
                                        .collection("conversations")
                                        .document(TextActivity.conversationId)
                                        .update("read", true);
                            else {
                                Log.e(TAG, "onBindViewHolder: ", updateReadMessageTask.getException());
                            }
                        });
            holder.yourTextMessageBubbleCardView.setVisibility(View.GONE);
            holder.theirTextMessageBubbleCardView.setVisibility(View.VISIBLE);
            holder.theirTextMessageTextView.setText(currentItem.getTextMessage());
            holder.theirTextMessageDateTextView.setText(delta);
            holder.theirTextMessageBubbleCardView.setBackground(ContextCompat.getDrawable(context, R.drawable.transparent));
            holder.theirTextMessageBubbleCardView.setBackgroundColor(ContextCompat.getColor(context, R.color.teal_msg_bubble));
        }
    }

    @Override
    public int getItemCount() {
        return textMessageItems.size();
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
                return String.format("%dy ago", numOfYears);
            if (numOfMonths > 0)
                return String.format("%dm ago", numOfMonths);
            if (numOfWeeks > 0)
                return String.format("%dw ago", numOfWeeks);
            if (numOfDays > 0)
                return String.format("%dd ago", numOfDays);
            if (hours > 0)
                return String.format("%dh ago", hours);
            if (minutes > 0)
                return String.format("%dm ago", minutes);
            if (seconds > 0)
                return String.format("%ds ago", seconds);

        } catch (ParseException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static class TextViewHolder extends RecyclerView.ViewHolder {
        private final TextView yourTextMessageTextView;
        private final TextView theirTextMessageTextView;
        private final TextView yourTextMessageDateTextView;
        private final TextView theirTextMessageDateTextView;
        private final CardView yourTextMessageBubbleCardView;
        private final CardView theirTextMessageBubbleCardView;
        private final ImageView sentReceivedCheck;

        public TextViewHolder(@NonNull View itemView) {
            super(itemView);
            yourTextMessageTextView = itemView.findViewById(R.id.yourTextMessageTextView);
            theirTextMessageTextView = itemView.findViewById(R.id.theirTextMessageTextView);
            yourTextMessageDateTextView = itemView.findViewById(R.id.yourTextMessageDateTextView);
            theirTextMessageDateTextView = itemView.findViewById(R.id.theirTextMessageDateTextView);
            yourTextMessageBubbleCardView = itemView.findViewById(R.id.yourTextMessageBubbleCardView);
            theirTextMessageBubbleCardView = itemView.findViewById(R.id.theirTextMessageBubbleCardView);
            sentReceivedCheck = itemView.findViewById(R.id.sentReceivedCheck);
        }
    }
}

package edu.uwp.appfactory.wishope.views.messaging;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.uwp.appfactory.wishope.R;
import edu.uwp.appfactory.wishope.utils.SharedPrefsManager;
import edu.uwp.appfactory.wishope.utils.UserConstants;
import edu.uwp.appfactory.wishope.views.calling.IncomingCallActivity;
import edu.uwp.appfactory.wishope.views.messaging.adapters.MessageAdapter;
import edu.uwp.appfactory.wishope.views.messaging.items.MessageItem;
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
public class MessagingFragment extends Fragment {
    private static final String TAG = "MessagingFragment";
    private final BroadcastReceiver incomingCallBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Here you can refresh your listview or other UI
            // Change activity/fragment
            startActivity(new Intent(requireContext(), IncomingCallActivity.class));
        }
    };
    public MessageAdapter messageAdapter;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    private TextView noMessagesTextView;
    private RecyclerView conversationsRecyclerView;
    private final BroadcastReceiver messagingFragmentBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Here you can refresh your listview or other UI
            noMessagesTextView.setVisibility(View.GONE);
            conversationsRecyclerView.setVisibility(View.VISIBLE);
            UserConstants.sortRecentConversations();
            messageAdapter.notifyDataSetChanged();
        }
    };
    private RecyclerView.LayoutManager conversationsLayoutManager;
    private CardView getConversationLoadingCardView;
    private TextView getConversationLoadingTextView;
    private boolean isLoading = false;

    public MessagingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Create the broadcast receiver
        requireContext().registerReceiver(messagingFragmentBroadcastReceiver, new IntentFilter("newMessage"));
        requireContext().registerReceiver(incomingCallBroadcastReceiver, new IntentFilter("incomingCall"));
        // Update RecyclerView
        messageAdapter.notifyDataSetChanged();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Remove the broadcast receiver
        requireContext().unregisterReceiver(messagingFragmentBroadcastReceiver);
        requireContext().unregisterReceiver(incomingCallBroadcastReceiver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_messaging, container, false);
        conversationsRecyclerView = view.findViewById(R.id.messageRecyclerView);
        getConversationLoadingCardView = view.findViewById(R.id.getConversationLoadingCardView);
        getConversationLoadingTextView = view.findViewById(R.id.getConversationLoadingTextView);
        noMessagesTextView = view.findViewById(R.id.noMessagesTextView);
        if (!UserConstants.RECENT_CONVERSATIONS.isEmpty()) {
            noMessagesTextView.setVisibility(View.GONE);
            conversationsRecyclerView.setVisibility(View.VISIBLE);
        }
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // Setup the RecyclerView
        setUpRecyclerView(view);
    }

    private void setUpRecyclerView(@NonNull View view) {
        conversationsLayoutManager = new LinearLayoutManager(requireContext());
        conversationsRecyclerView.setLayoutManager(conversationsLayoutManager);
        messageAdapter = new MessageAdapter();
        conversationsRecyclerView.setAdapter(messageAdapter);
        // Create the OnClick action for the RecyclerView items
        messageAdapter.setOnItemClickListener(position -> {
            if (!isLoading) {
                // Get the clicked on item
                isLoading = true;
                MessageItem currentMessageItem = UserConstants.RECENT_CONVERSATIONS.get(UserConstants.RECENT_CONVERSATIONS.keySet().toArray()[position]);
                // Mark the message as read, if it was not marked as read
                if (!currentMessageItem.isRead() && !currentMessageItem.getSender().equals(UserConstants.UID))
                    UserConstants.RECENT_CONVERSATIONS.get(UserConstants.RECENT_CONVERSATIONS.keySet().toArray()[position]).setRead(true);
                // Get the other users UID
                final String otherUID = currentMessageItem.getConversationId()
                        .replace(
                                UserConstants.UID,
                                ""
                        );
                // Initialize the conversation List if null
                if (TextActivity.conversation == null)
                    TextActivity.conversation = new ArrayList<>();
                // If the conversations cache List is empty, null, or this conversation does not exist in it, we do a get request to fetch this conversation.
                if (UserConstants.conversations == null || UserConstants.conversations.isEmpty() || UserConstants.conversations.get(currentMessageItem.getConversationId()) == null) {
                    getConversation(otherUID, view, position, currentMessageItem);
                } else {
                    db.collection("users")
                            .document(otherUID)
                            .get()
                            .addOnCompleteListener(otherUserDoc -> {
                                // Get the other users name and status
                                TextActivity.setNameText(currentMessageItem.getFullName());
                                if (TextActivity.conversation != null)
                                    TextActivity.conversation.clear();
                                else TextActivity.conversation = new ArrayList<>();
                                TextActivity.otherUID = otherUID;
                                String status = otherUserDoc.getResult().getString("status");
                                TextActivity.setStatus(status);
                                TextActivity.conversationId = UserConstants
                                        .RECENT_CONVERSATIONS
                                        .get(UserConstants.RECENT_CONVERSATIONS.keySet().toArray()[position])
                                        .getConversationId();
                                Bitmap imageBitmap = UserConstants.RECENT_CONVERSATIONS.get(UserConstants.RECENT_CONVERSATIONS.keySet().toArray()[position]).getProfilePicture();
                                TextActivity.setImage(imageBitmap);

                                // Check to see if the cached conversation is up to date with the document on Firestore
                                final String lastMessage1 = UserConstants.conversations.get(UserConstants.RECENT_CONVERSATIONS.get(UserConstants.RECENT_CONVERSATIONS.keySet().toArray()[position]).getConversationId()).get(0).get("message").toString();
                                final String lastMessage2 = UserConstants.RECENT_CONVERSATIONS.get(UserConstants.RECENT_CONVERSATIONS.keySet().toArray()[position]).getLastMessage();
                                final String sender1 = UserConstants.conversations.get(UserConstants.RECENT_CONVERSATIONS.get(UserConstants.RECENT_CONVERSATIONS.keySet().toArray()[position]).getConversationId()).get(0).get("sender").toString();
                                final String sender2 = UserConstants.RECENT_CONVERSATIONS.get(UserConstants.RECENT_CONVERSATIONS.keySet().toArray()[position]).getSender();
                                final String lastDate1 = UserConstants.conversations.get(UserConstants.RECENT_CONVERSATIONS.get(UserConstants.RECENT_CONVERSATIONS.keySet().toArray()[position]).getConversationId()).get(0).get("dateTime").toString();
                                final String lastDate2 = UserConstants.RECENT_CONVERSATIONS.get(UserConstants.RECENT_CONVERSATIONS.keySet().toArray()[position]).getLastDate();

                                if (UserConstants.conversations.get(UserConstants.RECENT_CONVERSATIONS.get(UserConstants.RECENT_CONVERSATIONS.keySet().toArray()[position]).getConversationId()) != null &&
                                        lastMessage1.equals(lastMessage2) && sender1.equals(sender2) && lastDate1.equals(lastDate2)) {
                                    // Add messages to the conversation List
                                    for (Map<String, Object> conversation : UserConstants.conversations.get(UserConstants.RECENT_CONVERSATIONS.get(UserConstants.RECENT_CONVERSATIONS.keySet().toArray()[position]).getConversationId())) {
                                        TextActivity.conversation.add(
                                                new TextMessageItem(
                                                        conversation.get("message").toString(),
                                                        conversation.get("sender").toString(),
                                                        conversation.get("dateTime").toString()
                                                )
                                        );
                                    }
                                    // Update RecyclerView
                                    messageAdapter.notifyDataSetChanged();
                                    // Navigate to the TextActivity
                                    isLoading = false;
                                    startActivity(new Intent(view.getContext(), TextActivity.class));
                                } else {
                                    // Cached conversation was not up to date, so we need to update it.
                                    getConversation(otherUID, view, position, currentMessageItem);
                                }
                            });
                }
            }
        });
    }

    /**
     * Get the conversation with another user.
     *
     * @param otherUID Other user's UID
     * @param view     current view
     * @param position current position
     */
    private void getConversation(final String otherUID, final View view, final int position, final MessageItem currentMessageItem) {
        getConversationLoadingCardView.setVisibility(View.VISIBLE);
        getConversationLoadingTextView.setText("One moment please...");
        if (UserConstants.conversations == null)
            UserConstants.conversations = new HashMap<>();
        // Get the conversation
        Map<String, Object> postMap = new HashMap<>();
        postMap.put("uid", otherUID);
        FirebaseFunctions
                .getInstance()
                .getHttpsCallable("getMessageArray")
                .call(postMap)
                .addOnCompleteListener(getMessageArrayTask -> {
                    if (!getMessageArrayTask.isSuccessful()) {
                        Exception e = getMessageArrayTask.getException();
                        if (e instanceof FirebaseFunctionsException) {
                            FirebaseFunctionsException ffe = (FirebaseFunctionsException) e;
                            FirebaseFunctionsException.Code code = ffe.getCode();
                            Log.e(TAG, "onComplete: getMessageArray: FFE: ", ffe);
                        }
                    } else {
                        List<Map<String, Object>> conversation;
                        conversation = (List<Map<String, Object>>) getMessageArrayTask.getResult().getData();
                        for (Map<String, Object> message : conversation) {
                            TextActivity.conversation.add(
                                    new TextMessageItem(
                                            message.get("message").toString(),
                                            message.get("sender").toString(),
                                            message.get("dateTime").toString()
                                    )
                            );
                        }
                        UserConstants.conversations.put(UserConstants.RECENT_CONVERSATIONS.get(UserConstants.RECENT_CONVERSATIONS.keySet().toArray()[position]).getConversationId(), conversation);
                        // Save conversation
                        SharedPrefsManager.saveConversations(requireActivity());
                        // Update RecyclerView
                        messageAdapter.notifyDataSetChanged();
                        db.collection("users")
                                .document(otherUID)
                                .get()
                                .addOnCompleteListener(otherUserDocGetTask -> {
                                    // Get the other users name and status
                                    TextActivity.setNameText(currentMessageItem.getFullName());
                                    TextActivity.otherUID = otherUID;
                                    String status = otherUserDocGetTask.getResult().getString("status");
                                    TextActivity.setStatus(status);
                                    TextActivity.conversationId = UserConstants
                                            .RECENT_CONVERSATIONS
                                            .get(UserConstants.RECENT_CONVERSATIONS.keySet().toArray()[position])
                                            .getConversationId();
                                    Bitmap imageBitmap = UserConstants
                                            .RECENT_CONVERSATIONS
                                            .get(UserConstants.RECENT_CONVERSATIONS.keySet().toArray()[position])
                                            .getProfilePicture();
                                    TextActivity.setImage(imageBitmap);
                                    getConversationLoadingCardView.setVisibility(View.GONE);
                                    getConversationLoadingTextView.setText("");
                                    // Navigate to the TextActivity
                                    db.collection("users")
                                            .document(otherUID)
                                            .collection("conversations")
                                            .document(TextActivity.conversationId)
                                            .update("read", true)
                                            .addOnCompleteListener(otherUserUpdateDoc -> {
                                                db.collection("users")
                                                        .document(UserConstants.UID)
                                                        .collection("conversations")
                                                        .document(TextActivity.conversationId)
                                                        .update("read", true)
                                                        .addOnCompleteListener(userUpdateDoc -> {
                                                            isLoading = false;
                                                            UserConstants.RECENT_CONVERSATIONS.get(TextActivity.conversationId);
                                                            messageAdapter.notifyDataSetChanged();
                                                            setUpRecyclerView(view);
                                                            startActivity(new Intent(view.getContext(), TextActivity.class));
                                                        });
                                            });
                                });
                    }
                });
    }
}
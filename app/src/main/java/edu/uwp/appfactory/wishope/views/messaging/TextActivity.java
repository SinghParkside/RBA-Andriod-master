package edu.uwp.appfactory.wishope.views.messaging;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.icu.lang.UCharacter;
import android.icu.text.BreakIterator;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.uwp.appfactory.wishope.R;
import edu.uwp.appfactory.wishope.utils.SharedPrefsManager;
import edu.uwp.appfactory.wishope.utils.UserConstants;
import edu.uwp.appfactory.wishope.views.CommunicateActivity;
import edu.uwp.appfactory.wishope.views.calling.IncomingCallActivity;
import edu.uwp.appfactory.wishope.views.landing.items.CoachProfileData;
import edu.uwp.appfactory.wishope.views.messaging.adapters.TextAdapter;
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
public class TextActivity extends AppCompatActivity {
    private static final String TAG = "TextActivity";
    public static String nameText;
    public static String otherUID = ""; // other person being texted
    public static String status = "Online";
    public static List<TextMessageItem> conversation;// list of conversations
    public static boolean isVisible = false;
    public static String conversationId = "";
    private static Bitmap imageBitmap = null;
    private final BroadcastReceiver incomingCallBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Here you can refresh your listview or other UI
            // Change activity/fragment
            startActivity(new Intent(context, IncomingCallActivity.class));
        }
    };
    public RecyclerView.Adapter conversationAdapter;
    private RecyclerView conversationRecyclerView;
    private final BroadcastReceiver textActivityBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Here you can refresh your listview or other UI
            conversationAdapter.notifyDataSetChanged();
            conversationRecyclerView.scrollToPosition(conversation.size() - 1);
            UserConstants.recentMessagesListener(UserConstants.UID, TextActivity.this);
            SharedPrefsManager.saveConversations(TextActivity.this);
        }
    };
    private EditText messageBox;
    private ImageView sendMessageButton;
    private ConstraintLayout parent;
    private TextView noMessagesTextView;

    // Empty Constructor
    public TextActivity() {
    }

    // Set the name of user
    public static void setNameText(String text) {
        nameText = text;
    }

    // Set the status of the user
    public static void setStatus(String s) {
        status = s;
    }

    // Set the profile image of the user
    public static void setImage(Bitmap img) {
        imageBitmap = img;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text);
        if (conversation == null)
            conversation = new ArrayList<>();
        isVisible = true;
        buildRecyclerView();
        parent = findViewById(R.id.textActivityView);

        // Get the name of the other user
        TextView otherUsersName = findViewById(R.id.textProfileName);
        messageBox = findViewById(R.id.editMessageText);
        sendMessageButton = findViewById(R.id.sendMessageButton);

        TextView statusTextView = findViewById(R.id.statusTextView);
        noMessagesTextView = findViewById(R.id.noMessagesTextView);
        statusTextView.setText(UCharacter.toTitleCase(status, BreakIterator.getTitleInstance()));
        ImageView statusIcon = findViewById(R.id.statusIcon);
        switch (status.toLowerCase()) {
            case "online":
                statusTextView.setTextColor(ContextCompat.getColor(this, R.color.status_online));
                statusIcon.setColorFilter(ContextCompat.getColor(this, R.color.status_online));
                break;
            case "busy":
                statusTextView.setTextColor(ContextCompat.getColor(this, R.color.status_busy));
                statusIcon.setColorFilter(ContextCompat.getColor(this, R.color.status_busy));
                break;
            default:
                statusTextView.setTextColor(ContextCompat.getColor(this, R.color.status_offline));
                statusIcon.setColorFilter(ContextCompat.getColor(this, R.color.status_offline));
                break;
        }

        ImageView textProfileImage = findViewById(R.id.textProfileImage);

        //Assuming there is a profile image, set image to that
        if (imageBitmap != null)
            textProfileImage.setImageBitmap(imageBitmap);
        ImageView backArrow = findViewById(R.id.backArrow);

        //On back arrow clicked empty texts
        backArrow.setOnClickListener(v -> {
            otherUID = "";
            setNameText("");
            setImage(null);
            setStatus("");
            isVisible = false;
            conversation.clear();
            onBackPressed();
        });
        ImageView callNowButton = findViewById(R.id.callNowButton);

        //If the user is a recoveree specifically grab the list of coaches
        if (UserConstants.ROLE.equalsIgnoreCase("recoveree"))
            callNowButton.setOnClickListener(v -> {
                CoachProfileData coachProfileData = new CoachProfileData(otherUID);
                if (UserConstants.ONLINE_USERS.contains(coachProfileData) && status.equalsIgnoreCase("online")) {
                    CommunicateActivity.Companion.setDisableMessage(true);
                    CommunicateActivity.coachData = UserConstants.ONLINE_USERS.get(UserConstants.ONLINE_USERS.indexOf(coachProfileData));
                    startActivity(new Intent(getApplicationContext(), CommunicateActivity.class));
                } else {
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
                    alertDialog
                            .setTitle(String.format("%s is not online!", nameText))
                            .setMessage("You can only call online users.")
                            .setIcon(R.drawable.bull_large_xxxhdpi)
                            .setNeutralButton("Dismiss", (dialog, which) -> {
                            })
                            .show();
                }
            });
        else
            callNowButton.setVisibility(View.GONE);

        //If message sent
        sendMessageButton.setOnClickListener(v -> {
            String message = messageBox.getText().toString().trim();
            //Constraints on message length
            if (message.length() < 1) {
                Snackbar.make(parent, "Message must not be empty.", Snackbar.LENGTH_LONG).show();
                return;
            }
            if (message.length() > 250) {
                Snackbar.make(parent, "Message must be 250 characters or less.", Snackbar.LENGTH_LONG).show();
                return;
            }
            sendMessageButton.setClickable(false);
            //Add the message to firebase
            Map<String, Object> sendMessage = new HashMap<>();
            sendMessage.put("message", message);
            sendMessage.put("to", otherUID);
            FirebaseFunctions mFunction = FirebaseFunctions.getInstance();
            if (conversationRecyclerView.getVisibility() != View.VISIBLE) {
                conversationRecyclerView.setVisibility(View.VISIBLE);
                noMessagesTextView.setVisibility(View.GONE);
                noMessagesTextView.setText("");
            }
            conversation.add(0, new TextMessageItem(messageBox.getText().toString(), UserConstants.UID, "Now"));
            //These next two lines stop the duplication of the checkmarks along with a line in TextAdapter
            TextAdapter textadapt = new TextAdapter(conversation);
            //textadapt.lastMessageIndex += 1;
            messageBox.setText("");
            conversationAdapter.notifyDataSetChanged();
            conversationRecyclerView.scrollToPosition(conversation.size() - 1);
            mFunction.getHttpsCallable("messageUser")
                    .call(sendMessage)
                    .addOnCompleteListener(new OnCompleteListener<HttpsCallableResult>() {
                        private static final String TAG = "TextActivity:mFunction:onCreate:messageUser:onComplete";

                        @Override
                        public void onComplete(@NonNull Task<HttpsCallableResult> task) {

                            if (task.isSuccessful()) {
                                Log.d(TAG, "SENT!" + messageBox.getText().toString());
                                // TODO use insertedIndex to update the UI to show the message was sent
                            } else {
                                Exception e = task.getException();
                                if (e instanceof FirebaseFunctionsException) {
                                    FirebaseFunctionsException ffe = (FirebaseFunctionsException) e;
                                    FirebaseFunctionsException.Code code = ffe.getCode();
                                    Snackbar.make(parent, "Error sending message", BaseTransientBottomBar.LENGTH_LONG);
                                    Log.e(TAG, String.format("Error: %s\nCode: %s", ffe.toString(), code.toString()));
                                }
                            }
                            sendMessageButton.setClickable(true);
                        }
                    });
        });
        otherUsersName.setText(nameText);
        buildRecyclerView();
        if (conversation.isEmpty()) {
            final String noConversations = String.format("You do not have a previous conversation with %s. Please send a message to %s to start a conversation.", nameText, nameText);
            conversationRecyclerView.setVisibility(View.GONE);
            noMessagesTextView.setVisibility(View.VISIBLE);
            noMessagesTextView.setText(noConversations);
        }
    }

    // Creating a recycler view of conversations
    public void buildRecyclerView() {
        conversationRecyclerView = findViewById(R.id.textRecyclerView);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        conversationAdapter = new TextAdapter(conversation);
        conversationRecyclerView.setLayoutManager(mLayoutManager);
        conversationRecyclerView.setAdapter(conversationAdapter);
        conversationRecyclerView.scrollToPosition(conversation.size() - 1);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.registerReceiver(textActivityBroadcastReceiver, new IntentFilter("newTextMessage"));
        this.registerReceiver(incomingCallBroadcastReceiver, new IntentFilter("incomingCall"));
    }

    @Override
    public void onPause() {
        super.onPause();
        this.unregisterReceiver(textActivityBroadcastReceiver);
        this.unregisterReceiver(incomingCallBroadcastReceiver);
    }

    @Override
    public void onBackPressed() {
        otherUID = "";
        setNameText("");
        setImage(null);
        setStatus("");
        isVisible = false;
        conversation.clear();
        super.onBackPressed();
    }
}
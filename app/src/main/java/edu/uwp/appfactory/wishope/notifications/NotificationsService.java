package edu.uwp.appfactory.wishope.notifications;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.RemoteInput;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import edu.uwp.appfactory.wishope.R;
import edu.uwp.appfactory.wishope.utils.UserConstants;
import edu.uwp.appfactory.wishope.views.calling.IncomingCallActivity;
import edu.uwp.appfactory.wishope.views.calling.VideoActivity;
import edu.uwp.appfactory.wishope.views.calling.VoiceActivity;
import edu.uwp.appfactory.wishope.views.landing.CommunicationActivity;
import edu.uwp.appfactory.wishope.views.messaging.TextActivity;
import edu.uwp.appfactory.wishope.views.messaging.items.TextMessageItem;


/**
 * <h1>Listens for incoming FCM payloads and will either generate or send a signal to update the UI</h1>
 * <p>This class handles the incoming data from the FCM payloads.</p>
 * <p>
 * REQUIRES ANDROID VERSION 26+ (OREO+)
 *
 * @author Allen Rocha
 * @version 2.0
 * @since 08-15-2020
 */
public class NotificationsService extends FirebaseMessagingService {
    // Key for the string that's delivered in the action's intent.
    private final String TAG = "NotificationsService";
    private final String TYPE_VIDEO = "video";
    private final String TYPE_VOICE = "voice";
    private final String TYPE_MESSAGE = "message";
    boolean isInForeground = false;
    Intent intent;

    /**
     * @param context Information about the application
     * @return if the app is currently running.
     */
    public static boolean isRunning(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        List<ActivityManager.RunningTaskInfo> tasks = activityManager.getRunningTasks(Integer.MAX_VALUE);

        for (ActivityManager.RunningTaskInfo task : tasks) {
            if (context.getPackageName().equalsIgnoreCase(task.baseActivity.getPackageName()))
                return true;
        }
        return false;
    }

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */

    @SuppressLint("WrongThread")
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onMessageReceived(@NotNull RemoteMessage remoteMessage) {
        // There are two types of messages data messages and notification messages. Data messages
        // are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data
        // messages are the type
        // traditionally used with GCM. Notification messages are only received here in
        // onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated
        // notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages
        // containing both notification
        // and data payloads are treated as notification messages. The Firebase console always
        // sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ

        // Check if the app is in the foreground
        try {
            isInForeground = new ForegroundCheckTask().execute(this).get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

        // Get the data from the FCM remote message
        if (remoteMessage.getData().size() > 0) {
            Map<String, String> dataPayload = remoteMessage.getData();
            // Check the type of the sent message
            // Possible types:
            //  VIDEO, VOICE, MESSAGE
            String type = dataPayload.get("type");
            // If the message is voice or video, it will display a notification
            // TODO use the fact the app may be in the foreground to update the UI of the app
            //  to notify the coach they are getting an incoming video or voice call
            if (!type.equals(TYPE_MESSAGE) &&
                    (type.equals(TYPE_VOICE)
                            || type.equals(TYPE_VIDEO))) {
                if (!isInForeground) {
                    videoVoiceNotification(dataPayload);
                } else {
//            Since the app is not in the foreground, we update the UI to show them they are being called.

//             TODO Keep track of which view is open, then update UI using a this.sendBroadcast(new Intent("incomingCall"));
                    IncomingCallActivity.Companion.setDataPayload(dataPayload);
                    Intent incomingCallIntent = new Intent("incomingCall");
                    // Inform the UI to navigate to the incoming call UI
                    this.sendBroadcast(incomingCallIntent);
                }

            } else if (type.equals(TYPE_MESSAGE) &&
                    !UserConstants.UID.equals("")) {
                // Create a new message map
                Map<String, Object> newMessage = new HashMap<>();
                newMessage.put("dateTime", dataPayload.get("dateTime"));
                newMessage.put("deleted", false);
                newMessage.put("message", dataPayload.get("body"));
                newMessage.put("sender", dataPayload.get("senderUID"));
                // If the app is not in the foreground or is not running, we display a notification.
                if (!isInForeground || !isRunning(this)) {
                    TextActivity.otherUID = dataPayload.get("senderUID");
                    newMessageNotification(dataPayload.get("title"), dataPayload.get("body"), dataPayload.get("senderUID"), dataPayload.get("senderName"));
                }
                // The app is running and the current view is the MessagingFragment
                else if (CommunicationActivity.viewPager != null &&
                        ((UserConstants.ROLE.equalsIgnoreCase("recoveree") && CommunicationActivity.viewPager.getCurrentItem() == 1) ||
                                (UserConstants.ROLE.equalsIgnoreCase("coach") && CommunicationActivity.viewPager.getCurrentItem() == 0))
                        && !TextActivity.isVisible) {
                    Log.d(TAG, "onMessageReceived: The app is running and the current view is the MessagingFragment");
                    try {
                        // Wait 2500 ms for the UI the be updated by the Firestore Listener
                        Thread.sleep(2500);
                        // Play a sound
                        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                        Ringtone ringtone = RingtoneManager.getRingtone(getApplicationContext(), notification);
                        ringtone.play();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                // Current view is the TextActivity
                else if (TextActivity.isVisible &&
                        !TextActivity.otherUID.isEmpty() &&
                        TextActivity.otherUID.equals(dataPayload.get("senderUID"))) {
                    Log.d(TAG, "onMessageReceived: The app is running and the current view is the TextActivity");
                    // Get the UID of the user that sent the message
                    TextActivity.otherUID = dataPayload.get("senderUID");
                    // Create a new TextMessage item containing the received message
                    TextActivity.conversation.add(
                            0,
                            new TextMessageItem(
                                    dataPayload.get("body"),
                                    dataPayload.get("senderUID"),
                                    "Now")
                    );
                    Intent newMessageTextViewIntent = new Intent("newTextMessage");
                    // Inform the adapter to update the RecyclerView
                    this.sendBroadcast(newMessageTextViewIntent);
                    // Mark the message as read for the other user.
                    FirebaseFirestore
                            .getInstance()
                            .collection("users")
                            .document(dataPayload.get("senderUID"))
                            .collection("conversations")
                            .document(TextActivity.conversationId)
                            .update("read", true)
                            .addOnCompleteListener(updateTheirReadField -> {
                                // After marking the message as read for the other user, we mark it read for this user
                                if (updateTheirReadField.isSuccessful())
                                    FirebaseFirestore
                                            .getInstance()
                                            .collection("users")
                                            .document(UserConstants.UID)
                                            .collection("conversations")
                                            .document(TextActivity.conversationId)
                                            .update("read", true)
                                            .addOnCompleteListener(updateYourReadField -> {
                                                if (updateYourReadField.isSuccessful())
                                                    Log.d(TAG, "onMessageReceived: Message marked as read.");
                                                else
                                                    Log.e(TAG, "onMessageReceived: ", updateYourReadField.getException());
                                            });
                                else {
                                    Log.e(TAG, "onMessageReceived: ", updateTheirReadField.getException());
                                }
                            });
                    try {
                        // Play a sound
                        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                        Ringtone ringtone = RingtoneManager.getRingtone(getApplicationContext(), notification);
                        ringtone.play();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.d(TAG, "onMessageReceived: The app is running and the current view is not the MessagingFragment or TextActivity");
                    // Create a regular notification
                    TextActivity.otherUID = dataPayload.get("senderUID");
                    newMessageNotification(dataPayload.get("title"), dataPayload.get("body"), dataPayload.get("senderUID"), dataPayload.get("senderName"));
                }
            } else if (type.equals(TYPE_MESSAGE)) {
                // Create a regular notification
                TextActivity.otherUID = dataPayload.get("senderUID");
                newMessageNotification(dataPayload.get("title"), dataPayload.get("body"), dataPayload.get("senderUID"), dataPayload.get("senderName"));
            }
        }
    }

    /**
     * This function gets all the video or voice information from the FCM payload.
     *
     * @param dataPayload The data the received from the FCM
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void videoVoiceNotification(final Map<String, String> dataPayload) {
        Bundle bundle = new Bundle();
        final String caller = dataPayload.get("caller");
        final String receiver = dataPayload.get("receiver");
        final String type = dataPayload.get("type");
        final String cDisplayName = dataPayload.get("cDisplayName");
        UserConstants.YOUR_DISPLAY_NAME = dataPayload.get("rDisplayName");
        // Create the room name, which will always be the sorted concatenated UIDs of the users
        UserConstants.ROOM = caller + receiver;
        String title;
        String body;
        if (type.equalsIgnoreCase(TYPE_VIDEO)) {
            intent = new Intent(this, VideoActivity.class);
            title = String.format("Incoming %s call from %s", type.toLowerCase(), cDisplayName);
            body = "Press accept to join the call.";
        } else if (type.equalsIgnoreCase(TYPE_VOICE)) {
            intent = new Intent(this, VoiceActivity.class);
            title = String.format("Incoming %s call from %s", type.toLowerCase(), cDisplayName);
            body = "Press accept to join the call.";
        } else {
            title = String.format("Incoming %s from %s", type.toLowerCase(), cDisplayName);
            body = "Press accept to reply.";
        }
        bundle.putString("YOUR_DISPLAY_NAME", UserConstants.YOUR_DISPLAY_NAME);
        bundle.putString("THEIR_DISPLAY_NAME", cDisplayName);
        bundle.putString("ROOM", caller + receiver);
        bundle.putString("OTHERUID", caller);
        UserConstants.THEIR_DISPLAY_NAME = cDisplayName;
        bundle.putString("AUTHENTICATION_TOKEN", UserConstants.AUTHENTICATION_TOKEN);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtras(bundle);

        // App is not in foreground
        sendNotification(title, body, type, caller);
    }

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    @Override
    public void onNewToken(@NotNull String token) {
        Log.d(TAG, String.format("Refreshed token: %s", token));

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        sendRegistrationToServer(token);
    }

    /**
     * Persist token to third-party servers.
     * <p>
     * Modify this method to associate the user's FCM InstanceID token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token) {
        UserConstants.FCM_TOKEN = token;
//        new SetFCM().POST();
    }

    /**
     * This function creates the notification for a newly received message.
     *
     * @param title     Title of the notification
     * @param body      Body of the notification
     * @param otherUID  UID of the user that sent the message
     * @param otherName Name of the user that sent the message
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void newMessageNotification(final String title, final String body, final String otherUID, final String otherName) {
        // Create a notification ID for this notification
        final int NOTIFICATION_ID = uidToInt(otherUID);
        // Create the ability to reply to the received message from the notification
        intent = new Intent(this, ReplyBroadcastReceiver.class);

        intent.putExtra("notificationId", NOTIFICATION_ID);

        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        RemoteInput remoteInput = new RemoteInput.Builder(ReplyBroadcastReceiver.KEY_TEXT_REPLY)
                .setLabel(String.format("Reply to %s", otherName))
                .build();
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntent(intent);

        // Indicating that this PendingIntent can be used only once.
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        // Create the reply action for the notification
        NotificationCompat.Action replyAction = new NotificationCompat.Action.Builder(
                R.drawable.ic_notification_reply,
                "Reply",
                pendingIntent
        ).addRemoteInput(remoteInput).setAllowGeneratedReplies(true).build();

        // Create the notification channel
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel incomingMessageNotificationChannel = ManageNotifications.createNotificationChannel(
                getString(R.string.incoming_message_notification_channel_id),
                getString(R.string.incoming_message_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
        );
        notificationManager.createNotificationChannel(incomingMessageNotificationChannel);
        Log.d(TAG, "Created incomingMessageNotificationChannel");
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationCompat.Builder incomingNotificationBuilder = Objects
                    .requireNonNull(
                            ManageNotifications.createNotification(
                                    null,
                                    this,
                                    getString(R.string.incoming_message_notification_channel_id),
                                    R.drawable.ic_incoming_message,
                                    title,
                                    body,
                                    NotificationCompat.PRIORITY_MAX
                            )
                    )
                    .setSound(defaultSoundUri)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .addAction(replyAction);
            Log.d(TAG, "Created incomingNotificationBuilder:message");
            notificationManager.notify(NOTIFICATION_ID, incomingNotificationBuilder.build());
        }
    }

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param messageTitle FCM message title received.
     * @param messageBody  FCM message body received.
     * @param channelId    Channel ID that the notification will be sent to
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void sendNotification(final String messageTitle, final String messageBody, final String channelId, final String otherUID) {
        // Opens the app to the talk fragment file (Start up)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // Indicating that this PendingIntent can be used only once.
        PendingIntent pendingIntent = PendingIntent
                .getActivity(
                        this,
                        0,
                        intent,
                        PendingIntent.FLAG_ONE_SHOT
                );
        NotificationCompat.Builder incomingNotificationBuilder;

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel incomingVideoCallNotificationChannel = ManageNotifications
                .createNotificationChannel(
                        getString(R.string.incoming_video_call_notification_channel_id),
                        getString(R.string.incoming_video_call_notification_channel_name),
                        NotificationManager.IMPORTANCE_HIGH
                );
        notificationManager.createNotificationChannel(incomingVideoCallNotificationChannel);
        Log.d(TAG, "Created incomingVideoCallNotificationChannel");

        NotificationChannel incomingVoiceCallNotificationChannel = ManageNotifications
                .createNotificationChannel(
                        getString(R.string.incoming_voice_call_notification_channel_id),
                        getString(R.string.incoming_voice_call_notification_channel_name),
                        NotificationManager.IMPORTANCE_HIGH
                );
        notificationManager.createNotificationChannel(incomingVoiceCallNotificationChannel);
        Log.d(TAG, "Created incomingVoiceCallNotificationChannel");

        NotificationChannel defaultNotificationChannel = ManageNotifications
                .createNotificationChannel(
                        getString(R.string.default_notification_channel_id),
                        getString(R.string.default_notification_channel_name),
                        NotificationManager.IMPORTANCE_HIGH
                );
        notificationManager.createNotificationChannel(defaultNotificationChannel);
        Log.d(TAG, "Created defaultNotificationChannel");

        // Sets the sound of the notification to the default one.
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        // TODO Implement the ability to delcine a call.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (channelId.equalsIgnoreCase(TYPE_VIDEO)) {
                // Create a Notification object
                incomingNotificationBuilder = Objects
                        .requireNonNull(
                                ManageNotifications
                                        .createNotification(
                                                null,
                                                this,
                                                getString(R.string.incoming_video_call_notification_channel_id),
                                                R.drawable.ic_incoming_video_call,
                                                messageTitle,
                                                messageBody,
                                                NotificationCompat.PRIORITY_MAX
                                        )
                        )
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setCategory(NotificationCompat.CATEGORY_CALL)
                        .addAction(
                                R.drawable.checkmark,
                                "Accept",
                                pendingIntent
                        );
                Log.d(TAG, "Created incomingNotificationBuilder:video_call");

            } else if (channelId.equalsIgnoreCase(TYPE_VOICE)) {
                incomingNotificationBuilder = Objects
                        .requireNonNull(
                                ManageNotifications
                                        .createNotification(
                                                null,
                                                this,
                                                getString(R.string.incoming_voice_call_notification_channel_id),
                                                R.drawable.ic_incoming_voice_call,
                                                messageTitle,
                                                messageBody,
                                                NotificationCompat.PRIORITY_MAX
                                        )
                        )
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setCategory(NotificationCompat.CATEGORY_CALL)
                        .addAction(
                                R.drawable.checkmark,
                                "Accept",
                                pendingIntent
                        );
                Log.d(TAG, "Created incomingNotificationBuilder:voice_call");
            } else {
                incomingNotificationBuilder = Objects
                        .requireNonNull(
                                ManageNotifications
                                        .createNotification(
                                                null,
                                                this,
                                                getString(R.string.default_notification_channel_id),
                                                R.drawable.ic_alert_icon,
                                                messageTitle,
                                                messageBody,
                                                NotificationCompat.PRIORITY_MAX
                                        )
                        )
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .addAction(
                                R.drawable.checkmark,
                                "Dismiss",
                                pendingIntent
                        );
                Log.d(TAG, "Created incomingNotificationBuilder:default_call");
            }
            final int UNIQUE_ID = uidToInt(otherUID);
            notificationManager.notify(UNIQUE_ID, incomingNotificationBuilder.build());
            Log.d(TAG, String.format("Built notification with ID: %s", UNIQUE_ID));
        }
    }

    private int uidToInt(final String uid) {
        final StringBuilder notificationID = new StringBuilder();

        // Iterate over the string tp covert all letters to their int values.
        for (int i = 0; i < uid.length(); i++) {
            if (Character.isDigit(uid.charAt(i))) {
                String cStr = String.format("%c", uid.charAt(i));
                notificationID.append(Integer.parseInt(cStr));
            } else {
                notificationID.append((int) uid.charAt(i));
            }
        }

        // Remove any leading zeroes
        String id = notificationID.toString().replaceFirst("^0*", "");
        final int firstDigit = Integer.parseInt(String.format("%c", id.charAt(0)));

        // Check if the leading digit is zero.
        // If it is we can use the first 10 digits as our id
        if (firstDigit == 1) {
            Log.d(
                    TAG,
                    String.format(
                            "Built notification with ID: %d",
                            Integer.parseInt(id.substring(0, 10))
                    )
            );
            return Integer.parseInt(id.substring(0, 10));
        } else {
            Log.d(
                    TAG,
                    String.format(
                            "Built notification with ID: %d",
                            Integer.parseInt(id.substring(0, 9))
                    )
            );
            return Integer.parseInt(id.substring(0, 9));
        }
    }

    /**
     * Check if the app is in the foreground
     */
    class ForegroundCheckTask extends AsyncTask<Context, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Context... params) {
            final Context context = params[0].getApplicationContext();
            return isAppOnForeground(context);
        }

        private boolean isAppOnForeground(Context context) {
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
            if (appProcesses == null) {
                return false;
            }
            final String packageName = context.getPackageName();
            for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
                if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName.equals(packageName)) {
                    return true;
                }
            }
            return false;
        }
    }
}

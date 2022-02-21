package edu.uwp.appfactory.wishope.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.RemoteInput;

import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;

import java.util.HashMap;
import java.util.Map;

import edu.uwp.appfactory.wishope.views.messaging.TextActivity;

/**
 * <h1>Listens for a reply the user adds to the generated messaging notification</h1>
 * <p>This class handles the message that the user entered for their notification reply.</p>
 *
 * @author Allen Rocha
 * @version 1.0
 * @since 12-20-2020
 */
public class ReplyBroadcastReceiver extends BroadcastReceiver {
    public static final String KEY_TEXT_REPLY = "key_text_reply";
    private final String TAG = "ReplyBroadcastReceiver";

    /**
     * This method is called when the BroadcastReceiver is receiving an Intent broadcast
     *
     * @param context The Context in which the receiver is running.
     * @param intent  The Intent being received.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        // Get remote input from received intent. Remote input is an answer a user has added to the reply. In our case it should contain a String.
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        if (remoteInput == null) {
            return;
        }
        // Get reply text
        final String message = remoteInput.getString(KEY_TEXT_REPLY);
        processInlineReply(message, context);
    }

    /**
     * Checks the reply message String length and sends calls the messageUser function.
     *
     * @param message The message the user entered for their reply to the received notification.
     * @param context The Context in which the receiver is running.
     */
    private void processInlineReply(final String message, final Context context) {
        if (message.length() < 1 || message.length() > 250)
            return;
        // Create a Map for the post request
        Map<String, Object> sendMessage = new HashMap<>();
        sendMessage.put("message", message);
        sendMessage.put("to", TextActivity.otherUID);
        // Send the reply to the other user
        FirebaseFunctions
                .getInstance()
                .getHttpsCallable("messageUser")
                .call(sendMessage)
                .addOnCompleteListener(messageUserTask -> {
                    if (messageUserTask.isSuccessful() && NotificationsService.isRunning(context)) {
                        // TODO notify the user their message has been sent
                    } else {
                        // TODO notify the user their message was not sent
                        Exception e = messageUserTask.getException();
                        if (e instanceof FirebaseFunctionsException) {
                            FirebaseFunctionsException ffe = (FirebaseFunctionsException) e;
                            FirebaseFunctionsException.Code code = ffe.getCode();
                            Log.e(TAG, String.format("Error: %s\nCode: %s", ffe.toString(), code.toString()));
                        }
                    }
                });
    }
}

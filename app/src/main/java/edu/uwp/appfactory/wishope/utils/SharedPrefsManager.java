package edu.uwp.appfactory.wishope.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static edu.uwp.appfactory.wishope.utils.UserConstants.conversations;

/**
 * <h1>Manages SharedPreferences for multiple different values we want to keep cached.</h1>
 * <p>
 * This class handles the saving and loading of the SharedPreferences items. Currently only handles
 * the saving of the users cached conversations.
 * </p>
 *
 * @author Allen Rocha
 * @version 1.0
 * @since 12-25-2020
 */
public class SharedPrefsManager {
    private final static String TAG = "SharedPrefsManager";

    private SharedPrefsManager() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Saves all the conversations using the SharedPreferences.
     *
     * @param activity the current activity the user is on.
     */
    public static void saveConversations(@NotNull final Activity activity) {
        try {
            SharedPreferences sharedPreferences = activity.getSharedPreferences(
                    "CONVERSATIONS",
                    Context.MODE_PRIVATE
            );
            SharedPreferences.Editor editor = sharedPreferences.edit();
            JSONObject jsonObject = new JSONObject(conversations);
            editor.putString("data", jsonObject.toString());
            editor.apply();
            Log.d(TAG, "saveConversations: Conversations saved!");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Exception raised while saving conversations: ", e);
        }
    }

    /**
     * Loads all the conversations the user has saved.
     *
     * @param activity the current activity the user is on.
     */
    public static void loadConversations(@NotNull final Activity activity) {
        SharedPreferences sharedPreferences = activity.getSharedPreferences(
                "CONVERSATIONS",
                Context.MODE_PRIVATE
        );
        String data = sharedPreferences.getString("data", null);
        if (data != null)
            try {
                JSONObject jsonObject = new JSONObject(data);
                Iterator<String> keys = jsonObject.keys();
                while (keys.hasNext()) {
                    String conversationId = keys.next();
                    if (conversations == null)
                        conversations = new HashMap<>();
                    if (jsonObject.get(conversationId) instanceof JSONArray) {
                        JSONArray jsonArray = (JSONArray) jsonObject.get(conversationId);
                        List<Map<String, Object>> conversation = new ArrayList<>();
                        for (int index = 0; index < jsonArray.length(); index++) {
                            JSONObject messageJson = jsonArray.getJSONObject(index);
                            Iterator<String> messageKeys = messageJson.keys();
                            Map<String, Object> message = new HashMap<>();
                            while (messageKeys.hasNext()) {
                                final String messageKey = messageKeys.next();
                                message.put(messageKey, jsonArray.getJSONObject(index).get(messageKey));
                            }
                            conversation.add(message);
                        }
                        conversations.put(conversationId, conversation);
                    }
                }
                Log.d(TAG, "loadConversations: Conversations loaded.");
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, "JSONException raised while loading conversations: ", e);
            }
    }

    /**
     * Clears all the conversations the user has saved.
     *
     * @param activity the current activity the user is on.
     */
    public static void clearConversations(@NotNull Activity activity) {
        try {
            activity.getSharedPreferences(
                    "CONVERSATIONS",
                    Context.MODE_PRIVATE
            )
                    .edit()
                    .clear()
                    .apply();
            conversations.clear();
            UserConstants.RECENT_CONVERSATIONS.clear();
            Log.d(TAG, "clearConversations: Conversations cleared!");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Exception raised while clearing conversations: ", e);
        }
    }

    /**
     * Saves the call history using the SharedPreferences.
     *
     * @param activity the current activity the user is on.
     */
    public static void saveCallHistory(@NotNull final Activity activity) {
        // Saves all Maps in the callHistory List
        try {
            SharedPreferences sharedPreferences = activity.getSharedPreferences(
                    "CALL_HISTORY",
                    Context.MODE_PRIVATE
            );
            SharedPreferences.Editor editor = sharedPreferences.edit();
            JSONArray jsonArray = new JSONArray(UserConstants.callHistory);
            editor.putString("data", jsonArray.toString());
            editor.apply();
            Log.d(TAG, "saveCallHistory: Call history saved!");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Exception raised while saving call history: ", e);
        }
    }


    /**
     * Loads the call history the user has saved.
     *
     * @param activity the current activity the user is on.
     */
    public static void loadCallHistory(@NotNull final Activity activity) {
        // Open the CALL_HISTORY shared preference file
        SharedPreferences sharedPreferences = activity.getSharedPreferences(
                "CALL_HISTORY",
                Context.MODE_PRIVATE
        );

        // Load the data if it exists
        String data = sharedPreferences.getString("data", null);
        if (data != null)
            try {
                // Convert the data to a JSON array
                JSONArray jsonArray = new JSONArray(data);
                // Iterate over the JSONs in the JSON array
                for (int index = 0; index < jsonArray.length(); index++) {
                    // Create the call Map
                    JSONObject call = jsonArray.getJSONObject(index);
                    Map<String, Object> callMap = new HashMap<>();
                    callMap.put("dateTime", call.get("dateTime"));
                    callMap.put("length", call.get("length"));
                    Map<String, String> otherUser = new HashMap<>();
                    otherUser.put("firstName", ((JSONObject) call.get("otherCaller")).get("firstName").toString());
                    otherUser.put("lastName", ((JSONObject) call.get("otherCaller")).get("lastName").toString());
                    otherUser.put("uid", ((JSONObject) call.get("otherCaller")).get("uid").toString());
                    callMap.put("otherCaller", otherUser);
                    // Add the call Map callHistory List of Maps
                    UserConstants.callHistory.add(callMap);
                }
                Log.d(TAG, "loadCallHistory: Call history loaded.");
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, "JSONException raised while loading call history: ", e);
            }
    }

    /**
     * Clears the call history user has saved.
     *
     * @param activity the current activity the user is on.
     */
    public static void clearCallHistory(@NotNull Activity activity) {
        try {
            activity.getSharedPreferences(
                    "CALL_HISTORY",
                    Context.MODE_PRIVATE
            )
                    .edit()
                    .clear()
                    .apply();
            UserConstants.callHistory.clear();
            Log.d(TAG, "clearCallHistory: Call history cleared!");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Exception raised while clearing call history: ", e);
        }
    }
}
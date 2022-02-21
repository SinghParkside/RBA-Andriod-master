package edu.uwp.appfactory.wishope.utils;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import edu.uwp.appfactory.wishope.views.landing.items.CoachProfileData;
import edu.uwp.appfactory.wishope.views.messaging.items.MessageItem;

/**
 * <h1>Contains lots of different static values that are accessed throughout the app.</h1>
 * <p>
 * This class handles the static values that is used in the app. This should be changed to not use
 * static values since it does not follow best OOP practices.
 * </p>
 *
 * @author Allen Rocha
 * @version 1.9.5
 * @since 08-15-2020
 */
public final class UserConstants {
    public static final List<CoachProfileData> ONLINE_USERS = new ArrayList<>();
    public static final Map<String, MessageItem> RECENT_CONVERSATIONS = new LinkedHashMap<>();
    private static final String TAG = "UserConstants";
    public static Map<String, List<Map<String, Object>>> conversations = new HashMap<>();
    public static Set<Map<String, Object>> callHistory = new LinkedHashSet<>();
    public static String YOUR_DISPLAY_NAME = "";
    public static String THEIR_DISPLAY_NAME = "";
    public static String THEIR_UID = "";
    public static String ROOM = "";
    public static String AUTHENTICATION_TOKEN = "";
    public static String EMAIL = "";
    public static String UID = "";
    public static String STATUS = "";
    public static String FCM_TOKEN = "";
    public static String ROLE = "";
    public static Bitmap PROFILE_PIC = null;
    public static String PROFILE_PIC_PATH = "";
    public static String COACH_BIO = "";
    public static String COACH_LOCATION = "";
    public static String FIRST_NAME = "";
    public static String LAST_NAME = "";

    private UserConstants() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * This function adds a Firestore listener that will listen for changes in this current user's
     * conversations sub-collection in their Firestore document. When the app first launches, this
     * function will generate the data for RECENT_CONVERSATIONS Map using the documents in the
     * user's conversations sub-collection. Any new conversations
     * (new documents in the sub-collection) will be added to the Map and any new recent messages
     * (updates to existing document in the sub-collection) will also be updated in the Map.
     *
     * @param uid      The current user's UID.
     * @param activity The current activity that is calling this function.
     * @return ignored.
     */
    public static ListenerRegistration recentMessagesListener(final String uid, final Activity activity) {
        return FirebaseFirestore
                .getInstance()
                .collection("users")
                .document(uid)
                .collection("conversations")
                .addSnapshotListener((documentSnapshots, error) -> {
                    // Log errors if there are any.
                    if (error != null) {
                        Log.e(TAG, "onEvent: ", error);
                    } else {
                        // Get the List of changed documents.
                        // On app startup, it will be all the documents but afterwards it will
                        // usually be just one document
                        final List<DocumentChange> documentChanges = documentSnapshots.getDocumentChanges();
                        // Iterate over the changed documents.
                        for (DocumentChange documentChange : documentChanges) {
                            // Get the current document ID (this is the conversation ID)
                            final String docId = documentChange.getDocument().getId();
                            // This conversation does not exist in our map, so we will add it.
                            if (RECENT_CONVERSATIONS.get(docId) == null) {
                                RECENT_CONVERSATIONS.put(docId, new MessageItem());
                                insertNewRecentMessage(documentChange.getDocument().getData(), docId, activity);
                            } else {
                                // Get the existing conversation
                                final MessageItem messageItem = RECENT_CONVERSATIONS.get(docId);
                                // Update lastDate, lastMessage, read, and sender
                                messageItem.setLastDate(
                                        documentChange
                                                .getDocument()
                                                .getData()
                                                .get("dateTime")
                                                .toString()
                                );
                                messageItem.setLastMessage(
                                        documentChange
                                                .getDocument()
                                                .getData()
                                                .get("lastMessage")
                                                .toString()
                                );
                                messageItem.setRead(
                                        (boolean) documentChange
                                                .getDocument()
                                                .getData()
                                                .get("read")
                                );
                                messageItem.setSender(
                                        documentChange
                                                .getDocument()
                                                .getData()
                                                .get("sender")
                                                .toString()
                                );
                                RECENT_CONVERSATIONS.put(docId, messageItem);
                                sortRecentConversations();
                                Intent newMessageIntent = new Intent("newMessage");
                                activity.sendBroadcast(newMessageIntent);
                            }
                        }
                    }
                });
    }

    /**
     * This function inserts a new recent message if the conversation (document) did not already
     * exist.
     *
     * @param documentData New data added to the current document.
     * @param documentId   The current document's ID.
     * @param activity     Current activity calling this function.
     */
    private static void insertNewRecentMessage(final Map<String, Object> documentData, final String documentId, Activity activity) {
        // Get the other user's UID by removing this user's UID from the conversation ID
        final String otherUID = documentId.replace(UID, "");
        FirebaseFirestore
                .getInstance()
                .collection("users")
                .document(otherUID)
                .get()
                .addOnCompleteListener(coachProfileSnapshot -> {
                    // Get the other user's information.
                    final DocumentSnapshot usersDocument = coachProfileSnapshot.getResult();
                    final String OTHER_USERS_NAME = usersDocument.get("firstName").toString() + " " + usersDocument.get("lastName").toString();
                    final String PROFILE_PIC_PATH = (String) usersDocument.get("profilePic");
                    // PROFILE_PIC_PATH is not null or empty if the other user is a coach.
                    if (PROFILE_PIC_PATH != null && !PROFILE_PIC_PATH.isEmpty()) {
                        // Begin downloading the profile picture from Firebase Storage.
                        final long ONE_MEGA_BYTE = 1048576L;
                        final FirebaseStorage storage = FirebaseStorage.getInstance();
                        final StorageReference storageRef = storage.getReference();
                        final StorageReference pathReference = storageRef.child(PROFILE_PIC_PATH);
                        pathReference
                                .getBytes(ONE_MEGA_BYTE)
                                .addOnCompleteListener(byteTask -> {
                                    // Decode byte array and generate it into a Bitmap
                                    final Bitmap profilePicture =
                                            BitmapFactory.decodeByteArray(
                                                    byteTask.getResult(),
                                                    0,
                                                    byteTask.getResult().length
                                            );
                                    // Add conversation to the RECENT_CONVERSATIONS Map
                                    RECENT_CONVERSATIONS
                                            .put(
                                                    documentId,
                                                    new MessageItem(
                                                            profilePicture,
                                                            OTHER_USERS_NAME,
                                                            (String) documentData.get("lastMessage"),
                                                            (String) documentData.get("dateTime"),
                                                            documentId,
                                                            (String) documentData.get("sender"),
                                                            (boolean) documentData.get("read")
                                                    )
                                            );
                                    // Sort the Map and update the UI if needed.
                                    sortRecentConversations();
                                    final Intent newMessageIntent = new Intent("newMessage");
                                    activity.sendBroadcast(newMessageIntent);
                                });
                    } else {
                        // Other user was not a coach so we add conversation to the
                        // RECENT_CONVERSATIONS Map with a null Bitmap Object.
                        RECENT_CONVERSATIONS
                                .put(
                                        documentId,
                                        new MessageItem(
                                                null,
                                                OTHER_USERS_NAME,
                                                (String) documentData.get("lastMessage"),
                                                (String) documentData.get("dateTime"),
                                                documentId,
                                                (String) documentData.get("sender"),
                                                (boolean) documentData.get("read")
                                        )
                                );
                        // Sort the Map and update the UI if needed.
                        sortRecentConversations();
                        final Intent newMessageIntent = new Intent("newMessage");
                        activity.sendBroadcast(newMessageIntent);
                    }
                });
    }

    /**
     * This function will sort the RECENT_CONVERSATIONS Map from newest at index 0 and oldest at the
     * last index.
     */
    public static void sortRecentConversations() {
        // Map only has 1 conversation, so it does not need to be sorted.
        if (RECENT_CONVERSATIONS.size() < 1)
            return;

        // Convert Map to List of Map
        final List<Map.Entry<String, MessageItem>> list = new LinkedList<>(RECENT_CONVERSATIONS.entrySet());

        // Sort list with Collections.sort(), provide a custom Comparator
        // Try switch the map1Entry map2Entry position for a different order
        list.sort((map1Entry, map2Entry) -> {
            if (map1Entry.getValue().getLastDate() != null && map2Entry.getValue().getLastDate() != null) {
                try {
                    final Date date1 = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss z", Locale.US).parse(map1Entry.getValue().getLastDate());
                    final Date date2 = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss z", Locale.US).parse(map2Entry.getValue().getLastDate());
                    // Compare the two dates
                    return date2.compareTo(date1);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            return 0;
        });

        // Loop the sorted list and put it into a new insertion order Map LinkedHashMap
        final Map<String, MessageItem> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<String, MessageItem> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        // Replace RECENT_CONVERSATIONS with the sorted Map.
        RECENT_CONVERSATIONS.clear();
        RECENT_CONVERSATIONS.putAll(sortedMap);
    }
}

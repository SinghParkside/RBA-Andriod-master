package edu.uwp.appfactory.wishope.views.landing;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

import edu.uwp.appfactory.wishope.R;
import edu.uwp.appfactory.wishope.utils.SharedPrefsManager;
import edu.uwp.appfactory.wishope.utils.UserConstants;
import edu.uwp.appfactory.wishope.views.auth.MainActivity;
import edu.uwp.appfactory.wishope.views.calling.IncomingCallActivity;
import edu.uwp.appfactory.wishope.views.calling.adapters.CallHistoryAdapter;

/**
 * This class updates the UI with the "CoachHomeFragment" to display to a coach
 * the home screen. It creates the view that allows the coach to edit their bio,
 * profile picture, and status. Displays their recent call history and duration
 * of the call with recoverees they have connected with
 * @author Allen Rocha
 * @version 1.9.5
 * @since 05-01-2021
 */
public class CoachHomeFragment extends Fragment {
    private final String TAG = "CoachHomeFragment";
    private final BroadcastReceiver incomingCallBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Here you can refresh your listview or other UI
            // Change activity/fragment
            startActivity(new Intent(requireContext(), IncomingCallActivity.class));
        }
    };
    private ImageView logoutButton;
    private ImageView pictureImageView;
    private TextView nameTextView;
    private TextView locationTextView;
    private TextView bioTextView;
    private TextView statusTextView;
    private SwitchCompat statusSwitch;
    private TextView noCallHistoryTextView;
    private ImageView noCallHistoryTextViewPadding;
    private ImageView callHistoryRecyclerViewPadding;
    private Context appContext;
    private CardView coachHomeLoadingCardView;
    private ProgressBar coachHomeLoadingProgressBar;
    private TextView coachHomeLoadingTextView;
    private RecyclerView callHistoryRecyclerView;
    private CallHistoryAdapter callHistoryAdapter;
    private RecyclerView.LayoutManager callHistoryLayoutManager;

    @Override
    public void onResume() {
        super.onResume();
        // Create the broadcast receiver
        // This is for a fragment
        requireContext().registerReceiver(incomingCallBroadcastReceiver, new IntentFilter("incomingCall"));

        // This is for an activity
//        this.registerReceiver(incomingCallBroadcastReceiver, new IntentFilter("incomingCall"));

    }


    @Override
    public void onPause() {
        super.onPause();
        // Remove the broadcast receiver
        // This is for a fragment
        requireContext().unregisterReceiver(incomingCallBroadcastReceiver);

        // This is for an activity
//        this.unregisterReceiver(incomingCallBroadcastReceiver);

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appContext = getContext();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_coach_home, container, false);
        logoutButton = view.findViewById(R.id.logout_button);
        pictureImageView = view.findViewById(R.id.coach_home_profile_picture);
        nameTextView = view.findViewById(R.id.coach_home_profile_name);
        locationTextView = view.findViewById(R.id.coach_home_profile_location);
        bioTextView = view.findViewById(R.id.coach_home_profile_bio);
        statusTextView = view.findViewById(R.id.coach_home_profile_status);
        statusSwitch = view.findViewById(R.id.coach_home_profile_status_switch);
        noCallHistoryTextView = view.findViewById(R.id.noCallHistoryTextView);
        noCallHistoryTextViewPadding = view.findViewById(R.id.noCallHistoryTextViewPadding);
        callHistoryRecyclerView = view.findViewById(R.id.callHistoryRecyclerView);
        callHistoryRecyclerViewPadding = view.findViewById(R.id.callHistoryRecyclerViewPadding);
        coachHomeLoadingCardView = view.findViewById(R.id.coachHomeLoadingCardView);
        coachHomeLoadingProgressBar = view.findViewById(R.id.coachHomeLoadingProgressBar);
        coachHomeLoadingTextView = view.findViewById(R.id.coachHomeLoadingTextView);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference profilePictureRef = storageRef.child(UserConstants.PROFILE_PIC_PATH);
        final long ONE_MEGABYTE = 1024L * 1024L;
        profilePictureRef
                .getBytes(ONE_MEGABYTE)
                .addOnCompleteListener(imageBytesTask -> {
                    if (imageBytesTask.isSuccessful()) {
                        UserConstants.PROFILE_PIC =
                                BitmapFactory.decodeByteArray(
                                        imageBytesTask.getResult(),
                                        0,
                                        imageBytesTask.getResult().length
                                );
                        pictureImageView.setImageBitmap(UserConstants.PROFILE_PIC);
                    } else {
                        Exception exception = imageBytesTask.getException();
                        Log.e(TAG, "onViewCreated:onFailure: ", exception);
                    }
                });
        if (UserConstants.callHistory != null && !UserConstants.callHistory.isEmpty())
            initializeRecyclerView();
        else {
            noCallHistoryTextView.setVisibility(View.VISIBLE);
            noCallHistoryTextViewPadding.setVisibility(View.VISIBLE);
            callHistoryRecyclerView.setVisibility(View.GONE);
            callHistoryRecyclerViewPadding.setVisibility(View.GONE);
        }
        //Checks the coach's status, that is retrieved from Firebase Firestore, to display the correct status for themselves/recoverees
        if (UserConstants.STATUS.equalsIgnoreCase("online")) {
            statusTextView.setText("Online");
            statusSwitch.setChecked(true);
            statusTextView.setTextColor(ContextCompat.getColor(appContext, R.color.status_online));
        } else {
            statusTextView.setText("Offline");
            statusSwitch.setChecked(false);
            statusTextView.setTextColor(ContextCompat.getColor(appContext, R.color.status_offline));
        }
        //A listener changes a coach's status in Firebase Firestore when they use the "statusSwitch" to change from 'online' to 'offline'
        // and vice versa
        statusSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            showLoadingUI("One moment please.");
            FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
            Map<String, Object> statusUpdate = new HashMap<>();
            if (isChecked) {
                statusTextView.setText("Online");
                UserConstants.STATUS = "Online";
                statusTextView.setTextColor(ContextCompat.getColor(appContext, R.color.status_online));
                statusUpdate.put("status", "online");
            } else {
                statusTextView.setText("Offline");
                UserConstants.STATUS = "Offline";
                statusTextView.setTextColor(ContextCompat.getColor(appContext, R.color.status_offline));
                statusUpdate.put("status", "offline");
            }
            //Updates the coach's status in Firebase Firestore after that status checks
            firebaseFirestore
                    .collection("users")
                    .document(UserConstants.UID)
                    .update(statusUpdate)
                    //Notify the coach with an alert dialog that their status update was successful.
                    //If not successful it alerts the coach of the result.
                    .addOnCompleteListener(updateTask -> {
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(appContext);
                        if (updateTask.isSuccessful()) {
                            alertDialog
                                    .setTitle(
                                            Html.fromHtml(
                                                    "<font color='#78A8AC'>Successfully updated status</font>",
                                                    Html.FROM_HTML_MODE_LEGACY
                                            )
                                    )
                                    .setMessage("Your status has been successfully updated!");
                        } else {
                            if (isChecked) {
                                statusTextView.setText("Offline");
                                UserConstants.STATUS = "Offline";
                                statusSwitch.setChecked(false);
                                statusTextView.setTextColor(ContextCompat.getColor(appContext, R.color.status_offline));
                            } else {
                                statusTextView.setText("Online");
                                UserConstants.STATUS = "Online";
                                statusSwitch.setChecked(true);
                                statusTextView.setTextColor(ContextCompat.getColor(appContext, R.color.status_online));
                            }
                            alertDialog
                                    .setTitle(
                                            Html.fromHtml(
                                                    "<font color='#78A8AC'>Could not update status</font>",
                                                    Html.FROM_HTML_MODE_LEGACY
                                            )
                                    )
                                    .setMessage("Your status could not be updated! Please try check your network and try again.");
                        }
                        hideLoadingUI();
                        alertDialog
                                .setIcon(R.drawable.bull_large_xxxhdpi)
                                .setNeutralButton("Dismiss", (dialog, which) -> {
                                })
                                .show();
                    });
        });
        logoutButton.setOnClickListener(v -> {
            showLoadingUI("Signing out. Please wait one moment...");
            // Remove your FCM token from Firestore
            clearFCM()
                    .addOnCompleteListener(setFCMTask -> {
                        if (setFCMTask.isSuccessful()) {
                            // Set your presence to offline on Firestore
                            clearPresence()
                                    .addOnCompleteListener(updatePresenceTask -> {
                                        if (updatePresenceTask.isSuccessful()) {
                                            SharedPrefsManager.clearConversations(requireActivity());
                                            SharedPrefsManager.clearCallHistory(requireActivity());

                                            UserConstants.STATUS = "offline";

                                            // Sign the user out
                                            FirebaseAuth.getInstance().signOut();

                                            // Hide loading UI
                                            hideLoadingUI();

                                            // Navigate to the LoginFragment
                                            navigateToStart();
                                        } else {
                                            // There was an issue executing the `updateUserPresence` method.
                                            genSnackBar(
                                                    updatePresenceTask.getException(),
                                                    "Error updating presence. Please try again."
                                            );
                                        }
                                    });
                        } else {
                            // There was an issue executing the `setFCM` method.
                            genSnackBar(setFCMTask.getException(), "Error removing token. Please try again.");
                        }
                    });
        });
        final String FULL_NAME = UserConstants.FIRST_NAME + " " + UserConstants.LAST_NAME;
        nameTextView.setText(FULL_NAME);
        locationTextView.setText(UserConstants.COACH_LOCATION);
        bioTextView.setText(UserConstants.COACH_BIO);
    }

    /**
     * This function initializes the RecyclerView Adapter, LinearLayoutManager, and sets them.
     */
    private void initializeRecyclerView() {
        callHistoryLayoutManager = new LinearLayoutManager(requireContext());
        callHistoryRecyclerView.setLayoutManager(callHistoryLayoutManager);
        callHistoryAdapter = new CallHistoryAdapter();
        callHistoryRecyclerView.setAdapter(callHistoryAdapter);
    }

    /**
     * This method call the Firebase Cloud Function `setFCM` to remove this user's FCM.
     */
    private Task<HttpsCallableResult> clearFCM() {
        FirebaseFunctions functions = FirebaseFunctions.getInstance();
        Map<String, Object> fcmPost = new HashMap<>();
        fcmPost.put("token", "");
        return functions
                .getHttpsCallable("setFCM")
                .call(fcmPost);
    }

    /**
     * This method call the Firebase Cloud Function `updateUserPresence` to set this user's status
     * to offline.
     */
    private Task<HttpsCallableResult> clearPresence() {
        FirebaseFunctions functions = FirebaseFunctions.getInstance();
        Map<String, Object> presencePost = new HashMap<>();
        presencePost.put("status", "offline");
        return functions
                .getHttpsCallable("updateUserPresence")
                .call(presencePost);
    }

    /**
     * This function will generate a SnackBar to notify the user that there was an error executing
     * the log out code.
     *
     * @param exception exception from the created in the Firebase Function.
     * @param message   Message to display where the Exception was raised.
     */
    private void genSnackBar(final Exception exception, final String message) {
        if (exception instanceof FirebaseFunctionsException) {
            Snackbar
                    .make(
                            requireView(),
                            message,
                            BaseTransientBottomBar.LENGTH_LONG
                    )
                    .setAction("Ok", snackBarView -> {
                    })
                    .show();
        }
    }

    /**
     * This method is called after the user signs out and it returns the to the MainActivity.
     */
    private void navigateToStart() {
        startActivity(new Intent(requireContext(), MainActivity.class));
    }

    /**
     * Will show the loading message when the Firebase SDKs are executing.
     *
     * @param loadingMessage Message to display where the Exception was raised.
     */
    private void showLoadingUI(final String loadingMessage) {
        coachHomeLoadingCardView.setVisibility(View.VISIBLE);
        coachHomeLoadingProgressBar.setVisibility(View.VISIBLE);
        coachHomeLoadingTextView.setVisibility(View.VISIBLE);
        coachHomeLoadingTextView.setText(loadingMessage);
        logoutButton.setClickable(false);
        statusSwitch.setClickable(false);
    }

    /**
     * Will hide the loading message after the Firebase SDKs are finished.
     */
    private void hideLoadingUI() {
        coachHomeLoadingCardView.setVisibility(View.GONE);
        coachHomeLoadingProgressBar.setVisibility(View.GONE);
        coachHomeLoadingTextView.setVisibility(View.GONE);
        coachHomeLoadingTextView.setText("");
        logoutButton.setClickable(true);
        statusSwitch.setClickable(true);
    }
}

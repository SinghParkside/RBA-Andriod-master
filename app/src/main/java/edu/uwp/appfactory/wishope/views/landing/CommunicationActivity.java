package edu.uwp.appfactory.wishope.views.landing;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.viewpager.widget.ViewPager;

import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.HashMap;
import java.util.Map;

import edu.uwp.appfactory.wishope.R;
import edu.uwp.appfactory.wishope.utils.SharedPrefsManager;
import edu.uwp.appfactory.wishope.utils.UserConstants;
import edu.uwp.appfactory.wishope.views.auth.MainActivity;
import edu.uwp.appfactory.wishope.views.calling.CallHistoryFragment;
import edu.uwp.appfactory.wishope.views.messaging.MessagingFragment;

/**
 * <h1>View for the Communication nav item</h1>
 * This class sets up the view for the `activity_communication` layout file. Contains the logic to
 * set TabLayout and TabItems. Also contains the logic to sign a user out when the click the
 * `signOutButton`.
 *
 * @author Allen Rocha
 * @version 1.0
 * @since 04-12-2020
 */
public class CommunicationActivity extends AppCompatActivity {
    public static ViewPager viewPager;
    private final String TAG = "CommunicationActivity";
    private CardView commActLogoutLoadingCardView;
    private ProgressBar commActLogoutLoadingProgressBar;
    private TextView commActLogoutLoadingTextView;
    private View parentView;

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed: Called");
        if (UserConstants.ROLE.equals("recoveree"))
            startActivity(new Intent(this, RecovereeLandingActivity.class));
        else
            startActivity(new Intent(this, CoachLandingActivity.class));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_communication);
        parentView = findViewById(R.id.communicationConstraintLayout);
        // Sign out button
        ImageView signOutButton = findViewById(R.id.rbaTabViewHeaderLogOutButton);

        // Assign the loading UI
        commActLogoutLoadingCardView = findViewById(R.id.commActLogoutLoadingCardView);
        commActLogoutLoadingProgressBar = findViewById(R.id.commActLogoutLoadingProgressBar);
        commActLogoutLoadingTextView = findViewById(R.id.commActLogoutLoadingTextView);

        // When the sign out button is pressed.
        signOutButton.setOnClickListener(v -> {
            showLoadingUI("Signing out. Please wait one moment...");
            // Remove your FCM token from Firestore
            clearFCM()
                    .addOnCompleteListener(setFCMTask -> {
                        if (setFCMTask.isSuccessful()) {
                            // Set your presence to offline on Firestore
                            clearPresence()
                                    .addOnCompleteListener(updatePresenceTask -> {
                                        if (updatePresenceTask.isSuccessful()) {
                                            SharedPrefsManager.clearConversations(this);
                                            SharedPrefsManager.clearCallHistory(this);

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
        // Assign TabLayout and the items
        TabLayout tabLayout = findViewById(R.id.communicationTabLayout);
        viewPager = findViewById(R.id.viewPager);

        // Initialize the Adapter for the TabLayout
        CommunicationTabAdapter communicationTabAdapter = new CommunicationTabAdapter(getSupportFragmentManager());

        // Add the `fragment_call_history` fragment as a TabItem
        if (UserConstants.ROLE.equalsIgnoreCase("recoveree"))
            communicationTabAdapter.addFragment(new CallHistoryFragment(), "Call History");
        // Add the `fragment_messaging` fragment as a TabItem
        communicationTabAdapter.addFragment(new MessagingFragment(), "Messaging");

        // Add the Adapter to the ViewPager
        viewPager.setAdapter(communicationTabAdapter);

        // Add the ViewPager to the TabLayout
        tabLayout.setupWithViewPager(viewPager);
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
                            parentView,
                            message,
                            BaseTransientBottomBar.LENGTH_LONG
                    )
                    .setAction("Ok", snackBarView -> {
                    })
                    .show();
        }
    }

    /**
     * This function shows the loading UI components.
     *
     * @param status the text to be displayed under the loading circle.
     */
    private void showLoadingUI(final String status) {
        commActLogoutLoadingCardView.setVisibility(View.VISIBLE);
        commActLogoutLoadingProgressBar.setVisibility(View.VISIBLE);
        commActLogoutLoadingTextView.setVisibility(View.VISIBLE);
        commActLogoutLoadingTextView.setText(status);
    }

    /**
     * This method hides the loading UI and is called after the functions have finished executing.
     */
    private void hideLoadingUI() {
        commActLogoutLoadingCardView.setVisibility(View.GONE);
        commActLogoutLoadingProgressBar.setVisibility(View.GONE);
        commActLogoutLoadingTextView.setVisibility(View.GONE);
        commActLogoutLoadingTextView.setText("");
    }

    /**
     * This method is called after the user signs out and it returns the to the MainActivity.
     */
    private void navigateToStart() {
        startActivity(new Intent(this, MainActivity.class));
    }
}
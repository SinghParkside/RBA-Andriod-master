package edu.uwp.appfactory.wishope.views.auth;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Objects;

import edu.uwp.appfactory.wishope.R;
import edu.uwp.appfactory.wishope.models.PermissionRequest;
import edu.uwp.appfactory.wishope.utils.NetworkUtil;

/**
 * <h1>Handles the loading of the LoginFragment and permission requests.</h1>
 *
 * @author Allen Rocha
 * @version 1.9.5
 * @since 15-08-2020
 */
public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivity";
    private final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate: Called");
        if (!NetworkUtil.Companion.checkConnection(this)) {
            final Activity currentActivity = this;
            new AlertDialog.Builder(this)
                    .setTitle(
                            Html.fromHtml(
                                    "<font color='#2D8C7F'>No Internet connection!</font>",
                                    Html.FROM_HTML_MODE_LEGACY
                            )
                    )
                    .setMessage("Could not connect to the Internet! Network connectivity is required to use this app.")
                    .setIcon(R.drawable.bull_small_xxxhdpi)
                    .setNeutralButton("Exit", (dialogInterface, i) -> currentActivity.finishAffinity())
                    .show();
        } else {
            if (!videoAndAudioPermission()) {
                PermissionRequest p = new PermissionRequest();
                p.setUpPermissions(this, this);
            }
            enableMyLocation();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(
                            R.id.main_fragment_container,
                            new LoginFragment(),
                            "LoginFragment"
                    )
                    .addToBackStack(null)
                    .commit();
            fragmentDelay(3);
        }
    }

    private void fragmentDelay(final int delayTime) {
        Log.d(TAG, "fragmentDelay: Called.");
        final Handler handler = new Handler();
        handler.postDelayed(() -> getSupportFragmentManager()
                        .beginTransaction()
                        .replace(
                                R.id.main_fragment_container,
                                new LoginFragment(),
                                "LoginFragment"
                        )
                        .addToBackStack(null)
                        .commit(),
                delayTime
        );
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed: Called");
        switch (Objects.requireNonNull(getSupportFragmentManager().getFragments().get(0).getTag())) {
            case "WelcomeFragment":
            case "LoginFragment":
                this.finishAffinity();
                break;
            case "RegisterFragment":
            case "PasswordResetFragment":
                super.onBackPressed();
                break;
            default:
                break;
        }
    }

    /**
     * Check for camera and record audio permissions
     */
    private boolean videoAndAudioPermission() {
        Log.d(TAG, "videoAndAudioPermission: ");
        return ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
                && (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED);
    }

    /**
     * Check for fine and coarse location permissions
     */
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
        )
                != PackageManager.PERMISSION_GRANTED
        )
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
    }

}

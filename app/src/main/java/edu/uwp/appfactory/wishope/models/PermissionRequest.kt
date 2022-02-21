package edu.uwp.appfactory.wishope.models

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * This class provides the implementation for permission requests
 * Requests permissions for:
 *          RECORD_AUDIO, CAMERA
 */
class PermissionRequest : AppCompatActivity() {

    private val RECORD_REQUEST_CODE = 101
    private val AUDIO_TAG = "Audio Permission"

    fun setUpPermissions(mContext: Context, fragmentActivity: FragmentActivity?) {

        val audioPermission = ContextCompat.checkSelfPermission(
            mContext,
            Manifest.permission.RECORD_AUDIO
        )

        val cameraPermission = ContextCompat.checkSelfPermission(
            mContext,
            Manifest.permission.CAMERA
        )

        if (audioPermission != PackageManager.PERMISSION_GRANTED) {
            makeRequest(fragmentActivity)
        }

        if (cameraPermission != PackageManager.PERMISSION_GRANTED) {
            makeRequest(fragmentActivity)
        }
    }

    private fun makeRequest(fragmentActivity: FragmentActivity?) {
        ActivityCompat.requestPermissions(
            fragmentActivity as Activity,
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA),
            RECORD_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {

            //TODO expand logging for other permissions

            RECORD_REQUEST_CODE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.i(AUDIO_TAG, "Permission denied by user.")
                } else {
                    Log.i(AUDIO_TAG, "Permission granted by user.")
                }
            }
        }
    }
}
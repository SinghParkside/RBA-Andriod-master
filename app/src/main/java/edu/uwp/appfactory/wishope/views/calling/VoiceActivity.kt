package edu.uwp.appfactory.wishope.views.calling

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.HttpsCallableResult
import com.google.firebase.ktx.Firebase
import edu.uwp.appfactory.wishope.R
import edu.uwp.appfactory.wishope.models.VideoRoom
import edu.uwp.appfactory.wishope.utils.UserConstants
import edu.uwp.appfactory.wishope.views.auth.MainActivity
import edu.uwp.appfactory.wishope.views.landing.CoachLandingActivity
import edu.uwp.appfactory.wishope.views.landing.RecovereeLandingActivity
import kotlinx.android.synthetic.main.activity_voice.*
import java.util.*


/**
 * This class displays the UI for voice-only calling. It still takes
 * the coach and recoveree to a Video Room, but has their cameras turned off
 * when they connect to the room. It only allows the users to mute/unmute with no
 * ability to turn on their cameras. Similarly to the 'VideoActivity' class
 * it works the same in how a call is ended. Either by a user leaving or a coach declining a call.
 *
 * @author Allen Rocha
 * @version 1.9.5
 * @since 15-08-2020
 */
class VoiceActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var room: VideoRoom
    private var micFlag: Boolean = true
    private val TAG = "VoiceActivity"
    private lateinit var listener: ListenerRegistration

    //TODO have a way to distinguish recoveree/life coach accounts

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice)
        val firebaseFirestore = FirebaseFirestore.getInstance()
        if (profileImageBitmap != null)
            voiceProfileImage.setImageBitmap(profileImageBitmap)

        //The following lines are taking all the information needed to initialize the Voice Calling Room
        //all of it is taken from the bundle passed from the when the recoveree calls the coach from their
        //Activity/Fragment
        val bundle: Bundle? = intent.extras
        if (bundle != null) {
            showLoadingUI("One moment please...")
            UserConstants.ROOM = bundle.getString("ROOM", null)
            UserConstants.ROOM = sortedString(UserConstants.ROOM)
            Log.d(TAG, "Extras found!")
            UserConstants.THEIR_DISPLAY_NAME =
                bundle.getString("THEIR_DISPLAY_NAME", null)
            Log.d(
                "VoiceActivity:THEIR_DISPLAY_NAME",
                UserConstants.THEIR_DISPLAY_NAME
            )
            UserConstants.YOUR_DISPLAY_NAME =
                bundle.getString("YOUR_DISPLAY_NAME", null)
            Log.d(
                "VoiceActivity:YOUR_DISPLAY_NAME",
                UserConstants.YOUR_DISPLAY_NAME
            )
            Log.d(
                "VoiceActivity:ROOM",
                UserConstants.ROOM
            )
            UserConstants.THEIR_UID =
                bundle.getString("OTHERUID", null)
            yourNameTextView.text =
                UserConstants.YOUR_DISPLAY_NAME
            theirNameTextView.text =
                UserConstants.THEIR_DISPLAY_NAME
            genAccessToken()
                .addOnCompleteListener { accessTokenTask: Task<HttpsCallableResult> ->
                    if (accessTokenTask.isSuccessful) {
                        Log.d(TAG, accessTokenTask.result?.data.toString())
                        if (accessTokenTask.result?.data.toString().isNotEmpty()) {
                            UserConstants.AUTHENTICATION_TOKEN =
                                accessTokenTask.result?.data.toString()
                            room = VideoRoom(this) //Create a room instance
                            room.setAccessToken(UserConstants.AUTHENTICATION_TOKEN)
                            room.connectToRoom(UserConstants.ROOM)
                            connectionStatus.text = "Connected"
                            //Alert Dialog is created to notify the recoveree that they have connected to the room
                            //and are currently waiting for the coach to join.
                            val alertDialogBuilder =
                                AlertDialog.Builder(this)
                            alertDialogBuilder
                                .setTitle(
                                    Html.fromHtml(
                                        String.format(
                                            "<font color='#78A8AC'>Waiting for %s to join.</font>",
                                            UserConstants.THEIR_DISPLAY_NAME
                                        ),
                                        Html.FROM_HTML_MODE_LEGACY
                                    )
                                )
                                .setIcon(R.drawable.bull_large_xxxhdpi)
                                .setNeutralButton("Ok") { _, _ -> }.show()
                        }
                    }
                    hideLoadingUI()
                }
        } else {
            if (!UserConstants.YOUR_DISPLAY_NAME.isNullOrEmpty()) {
                yourNameTextView.text =
                    UserConstants.YOUR_DISPLAY_NAME
            }
            if (!UserConstants.THEIR_DISPLAY_NAME.isNullOrEmpty()) {
                theirNameTextView.text =
                    UserConstants.THEIR_DISPLAY_NAME
                connect()
            }
        }

        //Listener detects when the user has tapped the hang up button
        buHangUp.setOnClickListener {
            hangUpCall()
        }

        //If permissions exist, initialize the room
        if (videoAndAudioPermission()) {
            room = VideoRoom(this) //Create a room instance
        }

        // Allows the user to click the mic image to mute/unmute
        ic_mic_on.setOnClickListener {
            micFlag = if (micFlag) {
                room.muteLocalParticipant()
                ic_mic_on.setImageResource(R.drawable.ic_mic_off)
                false
            } else {
                room.unmuteLocalParticipant()
                ic_mic_on.setImageResource(R.drawable.ic_mic_on)
                true
            }
        }

        //Creates a snapshot listener on the recoveree's 'isCalling' field to detect when it changes
        // from "true" to "false". If changed to "false" it will automatically end the call for the
        // recoveree and bring them back to their home screen
        val docRef = firebaseFirestore.collection("users").document(UserConstants.UID)
        listener = docRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w(TAG, "Listen failed.", e)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                Log.d(TAG, "Current data: ${snapshot.data}")
                if(snapshot.data?.get("isCalling").toString() == "false") {
                    firebaseFirestore.collection("users")
                        .document(UserConstants.UID)
                        .update("isCalling", "true")
                    declinedCall()
                }
            } else {
                Log.d(TAG, "Current data: null")
            }
        }
    }

    /**
     * Ends the call for user by ensuring they wish to exit the room. It then
     * takes them back to their respective home screen.
     */
    private fun hangUpCall() {
        showLoadingUI("One moment please...")
        listener.remove()
        val alertDialogBuilder =
            AlertDialog.Builder(this)
        alertDialogBuilder
            .setTitle(
                Html.fromHtml(
                    "<font color='#78A8AC'>Confirm</font>",
                    Html.FROM_HTML_MODE_LEGACY
                )
            )
            .setMessage("Are you sure you wish to exit the room?")
            .setIcon(R.drawable.bull_large_xxxhdpi)
            .setPositiveButton("Yes") { _, _ ->
                room.hangUp()
                if (room.startTime != Long.MIN_VALUE && room.endTime != Long.MIN_VALUE) {
                    val callLength: Long = room.callDuration
                    recordCallTime(callLength)
                        .addOnCompleteListener { recordCallTimeTask ->
//                            UserConstants.THEIR_UID = ""
                            if (recordCallTimeTask.isSuccessful) {
                                Log.d(TAG, "hangUpCall: Recorded Call Time Successfully")
                                getAllCallTimes()
                                    .addOnCompleteListener { getCallsTask ->
                                        if (getCallsTask.isSuccessful) {
                                            UserConstants.callHistory.clear()
                                            val allCallHistory =
                                                getCallsTask.result!!.data as ArrayList<Map<String, Any>>
                                            for (callHistory: Map<String, Any> in allCallHistory)
                                                UserConstants.callHistory.add(callHistory)
                                        } else {
                                            val e = getCallsTask.exception
                                            if (e is FirebaseFunctionsException) {
                                                val code = e.code
                                                val details = e.details
                                                Log.e(TAG, "hangUpCall: Exception", e)
                                            }
                                        }
                                        //We empty the information so the recoveree eventually can call another coach or the same coach
                                        connectionStatus.text = "Not Connected"
                                        UserConstants.AUTHENTICATION_TOKEN = ""
                                        UserConstants.ROOM = ""
                                        UserConstants.THEIR_DISPLAY_NAME = ""
                                        theirNameTextView.text = "NO USER"
                                        hideLoadingUI()
                                        auth = Firebase.auth
                                        if (!UserConstants.UID.isNullOrEmpty()) {
                                            if (!UserConstants.ROLE.isNullOrEmpty() && UserConstants.ROLE.toLowerCase() == "recoveree") {
                                                startActivity(
                                                    Intent(
                                                        this,
                                                        RecovereeLandingActivity::class.java
                                                    )
                                                )
                                            } else if (!UserConstants.ROLE.isNullOrEmpty() && UserConstants.ROLE.toLowerCase() == "coach") {
                                                startActivity(
                                                    Intent(
                                                        this,
                                                        CoachLandingActivity::class.java
                                                    )
                                                )
                                            } else {
                                                startActivity(
                                                    Intent(
                                                        this,
                                                        MainActivity::class.java
                                                    )
                                                )
                                            }
                                        } else {
                                            startActivity(Intent(this, MainActivity::class.java))
                                        }
                                    }

                            }
                            Log.e(TAG, "hangUpCall: Recorded Call Time Successfully")
                        }
                } else {
                    connectionStatus.text = "Not Connected"
                    UserConstants.AUTHENTICATION_TOKEN = ""
                    UserConstants.ROOM = ""
                    UserConstants.THEIR_DISPLAY_NAME = ""
                    theirNameTextView.text = "NO USER"
                    hideLoadingUI()
                    auth = Firebase.auth
                    if (!UserConstants.UID.isNullOrEmpty()) {
                        //These if-statements determine what is the role of the user that is leaving the room
                        //to lead them to their respective Landing Activities
                        if (!UserConstants.ROLE.isNullOrEmpty() && UserConstants.ROLE.toLowerCase() == "recoveree") {
                            startActivity(Intent(this, RecovereeLandingActivity::class.java))
                        } else if (!UserConstants.ROLE.isNullOrEmpty() && UserConstants.ROLE.toLowerCase() == "coach") {
                            startActivity(Intent(this, CoachLandingActivity::class.java))
                        } else {
                            startActivity(Intent(this, MainActivity::class.java))
                        }
                    } else {
                        startActivity(Intent(this, MainActivity::class.java))
                    }
                }

            }
            .setNegativeButton("No") { _, _ -> }
            .show()
    }

    /**
     * Connects the user to their respective room
     */
    fun connect() {
        //Check if room not initialized
        showLoadingUI("One moment please...")
        // Init handler
        if (!UserConstants.ROOM.isNullOrEmpty() && !this::room.isInitialized)
            UserConstants.ROOM = sortedString(
                UserConstants.ROOM
            )
        //We generate an access token for the user to allow them to connect to the room and distinguish who they are
        genAccessToken()
            .addOnCompleteListener { task: Task<HttpsCallableResult> ->
                if (task.isSuccessful) {
                    Log.d(TAG, task.result?.data.toString())
                    if (!task.result?.data.toString().isEmpty()) {
                        UserConstants.AUTHENTICATION_TOKEN = task.result?.data.toString()
                        room = VideoRoom(this)
                        room.setAccessToken(UserConstants.AUTHENTICATION_TOKEN)
                        room.connectToRoom(UserConstants.ROOM)
                        //This room name seems to not matter when using access tokens
                        //since the actual room info is embedded in them
                        connectionStatus.text = "Connected"
                        val alertDialogBuilder =
                            AlertDialog.Builder(this)
                        alertDialogBuilder
                            .setTitle(
                                Html.fromHtml(
                                    String.format(
                                        "<font color='#78A8AC'>Waiting for %s to join.</font>",
                                        UserConstants.THEIR_DISPLAY_NAME
                                    ),
                                    Html.FROM_HTML_MODE_LEGACY
                                )
                            )
                            .setIcon(R.drawable.bull_large_xxxhdpi)
                            .setNeutralButton("Ok") { _, _ -> }.show()
                    }
                }
                hideLoadingUI()
            }
    }

    /**
     * This method is similar to the hangUpCall() except it does not
     * confirm if the user wishes to leave the room as the 'coach', who
     * receives the call from the 'recoveree', has denied the call.
     */
    private fun declinedCall() {
        //Removed the listener to avoid having the recoveree have their call immediately being declined when listening for the document change
        listener.remove()
        room.hangUp()
        if (room.startTime != Long.MIN_VALUE && room.endTime != Long.MIN_VALUE) {
            val callLength: Long = room.callDuration
            recordCallTime(callLength)
                .addOnCompleteListener { recordCallTimeTask ->
                    if (recordCallTimeTask.isSuccessful) {
                        Log.d(TAG, "hangUpCall: Recorded Call Time Successfully")
                        getAllCallTimes()
                            .addOnCompleteListener { getCallsTask ->
                                if (getCallsTask.isSuccessful) {
                                    UserConstants.callHistory.clear()
                                    val allCallHistory =
                                        getCallsTask.result!!.data as ArrayList<Map<String, Any>>
                                    for (callHistory: Map<String, Any> in allCallHistory)
                                        UserConstants.callHistory.add(callHistory)
                                } else {
                                    val e = getCallsTask.exception
                                    if (e is FirebaseFunctionsException) {
                                        val code = e.code
                                        val details = e.details
                                        Log.e(TAG, "hangUpCall: Exception", e)
                                    }
                                }
                                connectionStatus.text = "Not Connected"
                                UserConstants.AUTHENTICATION_TOKEN = ""
                                UserConstants.ROOM = ""
                                UserConstants.THEIR_DISPLAY_NAME = ""
                                theirNameTextView.text = "NO USER"
                                hideLoadingUI()
                                auth = Firebase.auth
                                if (!UserConstants.UID.isNullOrEmpty()) {
                                    if (!UserConstants.ROLE.isNullOrEmpty() && UserConstants.ROLE.toLowerCase() == "recoveree") {
                                        startActivity(
                                            Intent(
                                                this,
                                                RecovereeLandingActivity::class.java
                                            )
                                        )
                                    } else if (!UserConstants.ROLE.isNullOrEmpty() && UserConstants.ROLE.toLowerCase() == "coach") {
                                        startActivity(
                                            Intent(
                                                this,
                                                CoachLandingActivity::class.java
                                            )
                                        )
                                    } else {
                                        startActivity(
                                            Intent(
                                                this,
                                                MainActivity::class.java
                                            )
                                        )
                                    }
                                } else {
                                    startActivity(Intent(this, MainActivity::class.java))
                                }
                            }

                    }
                    Log.e(TAG, "hangUpCall: Recorded Call Time Successfully")
                }
        } else {
            connectionStatus.text = "Not Connected"
            UserConstants.AUTHENTICATION_TOKEN = ""
            UserConstants.ROOM = ""
            UserConstants.THEIR_DISPLAY_NAME = ""
            theirNameTextView.text = "NO USER"
            hideLoadingUI()
            auth = Firebase.auth
            if (!UserConstants.UID.isNullOrEmpty()) {
                if (!UserConstants.ROLE.isNullOrEmpty() && UserConstants.ROLE.toLowerCase() == "recoveree") {
                    startActivity(Intent(this, RecovereeLandingActivity::class.java))
                } else if (!UserConstants.ROLE.isNullOrEmpty() && UserConstants.ROLE.toLowerCase() == "coach") {
                    startActivity(Intent(this, CoachLandingActivity::class.java))
                } else {
                    startActivity(Intent(this, MainActivity::class.java))
                }
            } else {
                startActivity(Intent(this, MainActivity::class.java))
            }
        }
    }


    /**
     * Check for camera and record audio permissions
     */
    private fun videoAndAudioPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
                && (ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED)
    }

    /**
     * Hangs up the call when the back button is pressed and takes
     * the user to their respective home screen.
     */
    override fun onBackPressed() {
        hangUpCall()
        super.onBackPressed()
    }

    private fun sortedString(inputString: String): String? {
        val arr = inputString.toCharArray()
        Arrays.sort(arr)
        return String(arr)
    }

    /**
     * Generates an access token for the room
     */
    private fun genAccessToken(): Task<HttpsCallableResult> {
        return FirebaseFunctions.getInstance().getHttpsCallable("accessToken")
            .call(
                mapOf(
                    "room" to UserConstants.ROOM,
                    "email" to FirebaseAuth.getInstance().currentUser!!.email
                )
            )
    }

    /**
     * Retrieves the recorded call time from Firebase firestore
     */
    private fun recordCallTime(callLength: Long): Task<HttpsCallableResult> {
        Log.d(TAG, "recordCallTime: callLength: $callLength")
        Log.d(TAG, "recordCallTime: UserConstants.THEIR_UID: " + UserConstants.THEIR_UID)
        return FirebaseFunctions.getInstance().getHttpsCallable("recordCallTime")
            .call(
                mapOf(
                    "callLength" to callLength,
                    "otherCaller" to UserConstants.THEIR_UID
                )
            )
    }

    private fun getAllCallTimes(): Task<HttpsCallableResult> {
        return FirebaseFunctions.getInstance().getHttpsCallable("getAllCallTimes")
            .call()
    }

    companion object {
        var profileImageBitmap: Bitmap? = null
    }

    /**
     * Method displays the loading UI
     */
    private fun showLoadingUI(loadingMessage: String) {
        ic_mic_on.isClickable = false
        buHangUp.isClickable = false
        loadingCardView.visibility = View.VISIBLE
        loadingProgressBar.visibility = View.VISIBLE
        loadingTextView.visibility = View.VISIBLE
        loadingTextView.text = loadingMessage
    }

    /**
     * Method hides the loading UI
     */
    private fun hideLoadingUI() {
        ic_mic_on.isClickable = true
        buHangUp.isClickable = true
        loadingCardView.visibility = View.GONE
        loadingProgressBar.visibility = View.GONE
        loadingTextView.visibility = View.GONE
        loadingTextView.text = ""
    }
}

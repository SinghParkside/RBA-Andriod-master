package edu.uwp.appfactory.wishope.views.calling

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
import kotlinx.android.synthetic.main.activity_video.*
import java.util.*


/**
 * This class hosts video calling communication between a coach and recoveree.
 * It takes all the information from the bundle, that is passed, to create a room
 * using the coach's and recoveree's UID as a sorted string. The recoveree is always the first one
 * to join/create the room as coaches only RECEIVE calls. It allows both users to mute/unmute and
 * disable/enable their video. The recoveree is alerted when a coach declines a call by
 * a listener on their "isCalling" field, in their document, that changes to 'false' when a coach declines a call.
 * Their UI is then updated back to their home-screen and are disconnected from the video room.
 * If either user leaves the call then user that is still connected get notified.
 * @author Allen Rocha
 * @version 1.9.5
 * @since 15-08-2020
 */
class VideoActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var room: VideoRoom
    private var micFlag: Boolean = true
    private var vidFlag: Boolean = true
    private val TAG = "VideoActivity"
    private lateinit var listener: ListenerRegistration

    //TODO have a way to distinguish recoveree/life coach accounts

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)
        val firebaseFirestore = FirebaseFirestore.getInstance()

        val bundle: Bundle? = intent.extras
        if (bundle != null) {
            showLoadingUI("One moment please...")
            Log.d(TAG, "Extras found!")
            UserConstants.ROOM = bundle.getString("ROOM", null)
            UserConstants.ROOM = sortedString(UserConstants.ROOM)
            Log.d(
                "VideoActivity:ROOM",
                UserConstants.ROOM
            )

            UserConstants.THEIR_DISPLAY_NAME =
                bundle.getString("THEIR_DISPLAY_NAME", null)
            Log.d(
                "VideoActivity:THEIR_DISPLAY_NAME",
                UserConstants.THEIR_DISPLAY_NAME
            )
            UserConstants.YOUR_DISPLAY_NAME =
                bundle.getString("YOUR_DISPLAY_NAME", null)
            UserConstants.THEIR_UID =
                bundle.getString("OTHERUID", null)
            Log.d(
                "VideoActivity:YOUR_DISPLAY_NAME",
                UserConstants.YOUR_DISPLAY_NAME
            )
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

        //When the hang up button is clicked it will call a method to end the call for the user
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

        // Allows the user to click the camera image to turn on/off video
        ic_video_on.setOnClickListener {
            vidFlag = if (vidFlag) {
                room.disableVideoLocalParticipant()
                ic_video_on.setImageResource(R.drawable.ic_video_off)
                false
            } else {
                room.enableVideoLocalParticipant()
                ic_video_on.setImageResource(R.drawable.ic_video_on)
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
     * This method disconnects the user from the video room, but confirms if they want
     * to leave the room. It then adds the call as the most recent on in their call history
     * and changes their payload information to empty strings or default settings. Depending if
     * the user's role is a 'coach' or 'recoveree' it will take them to their respective Landing Activity
     * once they left the room.
     */
    private fun hangUpCall() {
        showLoadingUI("One moment please...")
        listener.remove()
        //Displays an alert dialog to confirm if the user wants to end the call
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
                    //Records the call time and checks if the recording was successful in adding it
                    // to the user's Firestore document
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
                                        hideLoadingUI()
                                        connectionStatus.text = "Not Connected"
                                        UserConstants.AUTHENTICATION_TOKEN = ""
                                        UserConstants.ROOM = ""
                                        UserConstants.THEIR_DISPLAY_NAME = ""
                                        theirNameTextView.text = "NO USER"
                                        auth = Firebase.auth
                                        if (!UserConstants.UID.isNullOrEmpty()) {
                                            //Determines if the user is a 'recoveree' to transition them to their respective landing activity'
                                            if (!UserConstants.ROLE.isNullOrEmpty() && UserConstants.ROLE.toLowerCase() == "recoveree") {
                                                startActivity(
                                                    Intent(
                                                        this,
                                                        RecovereeLandingActivity::class.java
                                                    )
                                                )
                                            //Determines if the user is a 'coach' to transition them to the coach's landing activity
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
                            } else {
                                val e = recordCallTimeTask.exception
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
                                    startActivity(Intent(this, CoachLandingActivity::class.java))
                                } else {
                                    startActivity(Intent(this, MainActivity::class.java))
                                }
                            } else {
                                startActivity(Intent(this, MainActivity::class.java))
                            }
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
            .setNegativeButton("No") { _, _ -> }
            .show()
    }

    /**
     * This method is similar to the hangUpCall() except it does not
     * confirm if the user wishes to leave the room as the 'coach', who
     * receives the call from the 'recoveree', has denied the call.
     */
    private fun declinedCall() {
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
                                hideLoadingUI()
                                connectionStatus.text = "Not Connected"
                                UserConstants.AUTHENTICATION_TOKEN = ""
                                UserConstants.ROOM = ""
                                UserConstants.THEIR_DISPLAY_NAME = ""
                                theirNameTextView.text = "NO USER"
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
                    } else {
                        val e = recordCallTimeTask.exception
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
                            startActivity(Intent(this, CoachLandingActivity::class.java))
                        } else {
                            startActivity(Intent(this, MainActivity::class.java))
                        }
                    } else {
                        startActivity(Intent(this, MainActivity::class.java))
                    }
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
     * This method connects the user to the room and lets them know when they have connected.
     * It also displays an alert dialog to notify the user who they are waiting on to join.
     */
    fun connect() {
        //Check if room not initialized
        //TODO Add fields to capture the user's intended room/user(peer2peer)
        showLoadingUI("One moment please...")
        // Init handler
        if (!UserConstants.ROOM.isNullOrEmpty() && !this::room.isInitialized)
            UserConstants.ROOM = sortedString(
                UserConstants.ROOM
            )
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

    override fun onBackPressed() {
        hangUpCall()
        super.onBackPressed()
    }

    private fun sortedString(inputString: String): String? {
        val arr = inputString.toCharArray()
        Arrays.sort(arr)
        return String(arr)
    }

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
     * Records the call time between the coach and recoveree
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

    /**
     * Retrieves all call times from firebase firestore
     */
    private fun getAllCallTimes(): Task<HttpsCallableResult> {
        return FirebaseFunctions.getInstance().getHttpsCallable("getAllCallTimes")
            .call()
    }

    /**
     * Shows the loading UI
     */
    private fun showLoadingUI(loadingMessage: String) {
        loadingCardView.visibility = View.VISIBLE
        loadingProgressBar.visibility = View.VISIBLE
        loadingTextView.visibility = View.VISIBLE
        loadingTextView.text = loadingMessage
        ic_mic_on.isClickable = false
        ic_video_on.isClickable = false
        buHangUp.isClickable = false
    }

    /**
     * Hides the loading UI
     */
    private fun hideLoadingUI() {
        ic_mic_on.isClickable = true
        ic_video_on.isClickable = true
        buHangUp.isClickable = true
        loadingCardView.visibility = View.GONE
        loadingProgressBar.visibility = View.GONE
        loadingTextView.visibility = View.GONE
        loadingTextView.text = ""
    }
}

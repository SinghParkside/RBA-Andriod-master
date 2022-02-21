package edu.uwp.appfactory.wishope.views.calling

import android.content.Intent
import android.media.RingtoneManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import edu.uwp.appfactory.wishope.R
import edu.uwp.appfactory.wishope.utils.UserConstants
import edu.uwp.appfactory.wishope.views.landing.CoachLandingActivity
import kotlinx.android.synthetic.main.activity_incomingcall.*


/**
 * This class launches the UI for an incoming call that a coach is receiving
 * when they have the app running in the foreground.
 * It plays a ringtone alert to notify the coach, and allows them to
 * accept or decline the call. Currently the "message" button does not work
 * as it is a feature that may be removed in the future.
 * @author Elicipri Maldonado
 *
 */
class IncomingCallActivity : AppCompatActivity() {

    private lateinit var callerUID: String
    private lateinit var receiverUID: String
    private lateinit var type: String
    private lateinit var cDisplayName: String
    private lateinit var rDisplayName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incomingcall)
        callerUID = dataPayload["caller"].toString()
        receiverUID = dataPayload["receiver"].toString()
        type = dataPayload["type"].toString()
        cDisplayName = dataPayload["cDisplayName"].toString()
        UserConstants.YOUR_DISPLAY_NAME = dataPayload["rDisplayName"]
        // Create the room name, which will always be the sorted concatenated UIDs of the users
        UserConstants.ROOM = callerUID + receiverUID
        Log.e("Incoming Call Activity", cDisplayName)
        incomingCallersNameText.text = cDisplayName
        //Connect to Firebase Firestore to update the recoveree's 'isCalling' field to true
        val firebaseFirestore = FirebaseFirestore.getInstance()
        firebaseFirestore.collection("users")
            .document(callerUID)
            .update("isCalling", "true")
        //Since the app is not in the foreground, we update the UI to show them they are being called.
        val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        val ringtone = RingtoneManager.getRingtone(applicationContext, notification)
        ringtone.play()
        //Allows the coach to decline the call and takes them back to the coach home page
        hangupButton.setOnClickListener {
            ringtone.stop()
            //When the coach declines the call it updates the recoveree's 'isCalling' field to "false"
            firebaseFirestore.collection("users")
                .document(callerUID)
                .update("isCalling", "false")
            intent = Intent(this, CoachLandingActivity::class.java)
            startActivity(intent)
        }
        answerButton.setOnClickListener {
            ringtone.stop()
            //Determines the type of call the coach is receiving
            intent = if (type.equals("video", ignoreCase = true)) {
                Intent(this, VideoActivity::class.java)

            } else {
                Intent(this, VoiceActivity::class.java)
            }
            //Places all the information needed for the coach when they accept the call
            intent.putExtra("YOUR_DISPLAY_NAME", UserConstants.YOUR_DISPLAY_NAME)
            intent.putExtra("THEIR_DISPLAY_NAME", cDisplayName)
            intent.putExtra("ROOM", callerUID + receiverUID)
            intent.putExtra("OTHERUID", callerUID)
            UserConstants.THEIR_DISPLAY_NAME = cDisplayName
            intent.putExtra("AUTHENTICATION_TOKEN", UserConstants.AUTHENTICATION_TOKEN)
            startActivity(intent)
        }
        //Consider to remove/add this feature
        messageButton.setOnClickListener {
            ringtone.stop()
            Toast.makeText(this, "Feature in Development", Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        @JvmStatic
        var dataPayload: Map<String, String> = HashMap()
    }

}

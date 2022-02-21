package edu.uwp.appfactory.wishope.views

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.HttpsCallableResult
import edu.uwp.appfactory.wishope.R
import edu.uwp.appfactory.wishope.utils.SharedPrefsManager
import edu.uwp.appfactory.wishope.utils.UserConstants
import edu.uwp.appfactory.wishope.views.calling.VideoActivity
import edu.uwp.appfactory.wishope.views.calling.VoiceActivity
import edu.uwp.appfactory.wishope.views.landing.items.CoachProfileData
import edu.uwp.appfactory.wishope.views.messaging.TextActivity
import edu.uwp.appfactory.wishope.views.messaging.items.TextMessageItem
import kotlinx.android.synthetic.main.activity_communicate.*
import java.util.*

/**
 * <h1>This class is for cluster item objects.</h1>
 * <p>Lets map pins carry more information and importantly lets them be clustered.</p>
 *
 * @author Allen Rocha
 * @version 1.9.5
 * @since 07-01-2020
 */
class CommunicateActivity : AppCompatActivity() {
    private var isLoading: Boolean = false
    private val TAG = "CommunicateActivity"
    private lateinit var overlayCoachProfileConstraintLayoutClose: ImageView
    private lateinit var startAConversationTextView: TextView
    private lateinit var messageGridTextView: TextView
    private lateinit var bigCoachProfilePicture: ImageView
    private lateinit var bigCoachLocation: TextView
    private lateinit var bigCoachBio: TextView
    private lateinit var videoCallButton: CardView
    private lateinit var messageButton: CardView
    private lateinit var voiceCallButton: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_communicate)
        val currentActivity = this
        overlayCoachProfileConstraintLayoutClose =
            findViewById(R.id.overlayCoachProfileConstraintLayoutClose)
        startAConversationTextView = findViewById(R.id.startAConversationTextView)
        messageGridTextView = findViewById(R.id.messageGridTextView)
        bigCoachProfilePicture = findViewById(R.id.bigCoachProfilePicture)
        bigCoachLocation = findViewById(R.id.bigCoachLocation)
        bigCoachBio = findViewById(R.id.bigCoachBio)
        videoCallButton = findViewById(R.id.videoCallButton)
        voiceCallButton = findViewById(R.id.voiceCallButton)
        messageButton = findViewById(R.id.messageButton)
        overlayCoachProfileConstraintLayoutClose.setOnClickListener {
            disableMessage = false
            onBackPressed()
        }
        bigCoachProfilePicture.setImageBitmap(coachData.imageBitmap)
        bigCoachLocation.text = coachData.location
        bigCoachBio.text = coachData.bio
        val conversationStr = String.format(
            "Start a conversation with %s %s", toTileCase(
                coachData.firstName
            ), toTileCase(
                coachData.lastName
            )
        )
        startAConversationTextView.text = conversationStr
        videoCallButton.setOnClickListener {
            showLoadingUI("One moment please...")
            val type = "Video"
            callUser(type)
                .addOnCompleteListener { callUserTask: Task<HttpsCallableResult?> ->
                    if (!callUserTask.isSuccessful) {
                        val e = callUserTask.exception
                        if (e is FirebaseFunctionsException) {
                            Log.e(TAG, "CallUser:Video:CODE ", e)
                        } else e!!.printStackTrace()
                        hideLoadingUI()
                    } else {
                        Log.d(TAG, "Success")
                        UserConstants.THEIR_DISPLAY_NAME =
                            coachData.firstName + " " + coachData.lastName
                        UserConstants.ROOM = coachData.uid + UserConstants.UID
                        genAccessToken()
                            .addOnCompleteListener { genAccessTokenTask: Task<String> ->
                                if (!genAccessTokenTask.isSuccessful) {
                                    val e = genAccessTokenTask.exception
                                    if (e is FirebaseFunctionsException) {
                                        Log.e(TAG, "AccessToken: ", e)
                                    } else e!!.printStackTrace()
                                    hideLoadingUI()
                                } else {
                                    Log.d(TAG, "AccessToken: Success")
                                    UserConstants.AUTHENTICATION_TOKEN =
                                        genAccessTokenTask.result
                                    hideLoadingUI()
                                    startActivity(
                                        Intent(
                                            applicationContext,
                                            VideoActivity::class.java
                                        )
                                    )
                                }
                            }
                    }
                }
        }
        voiceCallButton.setOnClickListener {
            showLoadingUI("One moment please...")
            val type = "Voice"
            callUser(type)
                .addOnCompleteListener { callUserTask: Task<HttpsCallableResult?> ->
                    if (!callUserTask.isSuccessful) {
                        val e = callUserTask.exception
                        if (e is FirebaseFunctionsException) {
                            Log.e(TAG, "CallUser:Voice:CODE ", e)
                        } else e!!.printStackTrace()
                        hideLoadingUI()
                    } else {
                        Log.d(TAG, "Success")
                        UserConstants.THEIR_DISPLAY_NAME =
                            coachData.firstName + " " + coachData.lastName
                        UserConstants.ROOM = coachData.uid + UserConstants.UID
                        genAccessToken()
                            .addOnCompleteListener { genAccessTokenTask: Task<String> ->
                                if (!genAccessTokenTask.isSuccessful) {
                                    val e = genAccessTokenTask.exception
                                    if (e is FirebaseFunctionsException) {
                                        Log.e(TAG, "AccessToken: ", e)
                                    } else e!!.printStackTrace()
                                    hideLoadingUI()
                                } else {
                                    Log.d(TAG, "AccessToken: Success")
                                    UserConstants.AUTHENTICATION_TOKEN =
                                        genAccessTokenTask.result
                                    VoiceActivity.profileImageBitmap = coachData.imageBitmap
                                    hideLoadingUI()
                                    startActivity(
                                        Intent(
                                            applicationContext,
                                            VoiceActivity::class.java
                                        )
                                    )
                                }
                            }
                    }
                }
        }
        if (!disableMessage) {
            messageButton.setOnClickListener {
                showLoadingUI("One moment please...")
                // Initialize the conversation List if null
                if (TextActivity.conversation == null)
                    TextActivity.conversation = ArrayList()
                // Get the other users name and status
                TextActivity.setNameText(
                    "${toTileCase(coachData.firstName)} ${toTileCase(coachData.lastName)}"
                )
                TextActivity.otherUID = coachData.uid
                val status = coachData.status
                TextActivity.setStatus(status)
                TextActivity.conversationId = coachData.uid + UserConstants.UID
                val imageBitmap = coachData.imageBitmap
                TextActivity.setImage(imageBitmap)
                // The user does not have previous conversation or any conversations
                if (UserConstants.RECENT_CONVERSATIONS.isEmpty() || UserConstants.RECENT_CONVERSATIONS[coachData.uid + UserConstants.UID] == null) {
                    hideLoadingUI()
                    startActivity(Intent(applicationContext, TextActivity::class.java))
                }
                // The user has previous conversation but it is not cached
                else if (UserConstants.RECENT_CONVERSATIONS[coachData.uid + UserConstants.UID] != null &&
                    UserConstants.conversations[coachData.uid + UserConstants.UID] == null
                ) {
                    val postMap: MutableMap<String, Any> = HashMap()
                    postMap["uid"] = coachData.uid
                    FirebaseFunctions
                        .getInstance()
                        .getHttpsCallable("getMessageArray")
                        .call(postMap)
                        .addOnCompleteListener { getMessageArrayTask: Task<HttpsCallableResult> ->
                            if (!getMessageArrayTask.isSuccessful) {
                                val e = getMessageArrayTask.exception
                                if (e is FirebaseFunctionsException) {
                                    val ffe = e
                                    val code = ffe.code
                                    Log.e(
                                        TAG,
                                        "onComplete: getMessageArray: FFE: ",
                                        ffe
                                    )
                                }
                                hideLoadingUI()
                            } else {
                                val conversation: List<Map<String, Any>>? =
                                    getMessageArrayTask.result!!
                                        .data as List<Map<String, Any>>?
                                for (message in conversation!!) {
                                    TextActivity.conversation.add(
                                        TextMessageItem(
                                            message["message"].toString(),
                                            message["sender"].toString(),
                                            message["dateTime"].toString()
                                        )
                                    )
                                }
                                UserConstants.conversations[coachData.uid + UserConstants.ROLE] =
                                    conversation
                                // Save conversation
                                SharedPrefsManager.saveConversations(currentActivity)
                                // Navigate to the TextActivity
                                hideLoadingUI()
                                startActivity(Intent(applicationContext, TextActivity::class.java))
                            }
                        }
                } // The user has previous conversation and it is cached
                else if (UserConstants.RECENT_CONVERSATIONS[coachData.uid + UserConstants.UID] != null &&
                    UserConstants.conversations[coachData.uid + UserConstants.UID] != null
                ) {


                    // Check to see if the cached conversation is up to date with the document on Firestore
                    val lastMessage1 =
                        UserConstants.conversations[UserConstants.RECENT_CONVERSATIONS[UserConstants.RECENT_CONVERSATIONS.keys.toTypedArray()[position]]!!.conversationId]!![0]["message"].toString()
                    val lastMessage2 =
                        UserConstants.RECENT_CONVERSATIONS[UserConstants.RECENT_CONVERSATIONS.keys.toTypedArray()[position]]!!.lastMessage
                    val sender1 =
                        UserConstants.conversations[UserConstants.RECENT_CONVERSATIONS[UserConstants.RECENT_CONVERSATIONS.keys.toTypedArray()[position]]!!.conversationId]!![0]["sender"].toString()
                    val sender2 =
                        UserConstants.RECENT_CONVERSATIONS[UserConstants.RECENT_CONVERSATIONS.keys.toTypedArray()[position]]!!.sender
                    val lastDate1 =
                        UserConstants.conversations[UserConstants.RECENT_CONVERSATIONS[UserConstants.RECENT_CONVERSATIONS.keys.toTypedArray()[position]]!!.conversationId]!![0]["dateTime"].toString()
                    val lastDate2 =
                        UserConstants.RECENT_CONVERSATIONS[UserConstants.RECENT_CONVERSATIONS.keys.toTypedArray()[position]]!!.lastDate

                    if (UserConstants.conversations[UserConstants.RECENT_CONVERSATIONS[UserConstants.RECENT_CONVERSATIONS.keys.toTypedArray()[position]]!!.conversationId] != null && lastMessage1 == lastMessage2 && sender1 == sender2 && lastDate1 == lastDate2
                    ) {
                        val conversation =
                            UserConstants.conversations[coachData.uid + UserConstants.UID]
                        for (message in conversation!!) {
                            TextActivity.conversation.add(
                                TextMessageItem(
                                    message["message"].toString(),
                                    message["sender"].toString(),
                                    message["dateTime"].toString()
                                )
                            )
                        }
                        UserConstants.conversations[UserConstants.RECENT_CONVERSATIONS[UserConstants.RECENT_CONVERSATIONS.keys.toTypedArray()[position]]!!
                            .conversationId] = conversation
                        // Save conversation
                        SharedPrefsManager.saveConversations(currentActivity)
                        // Navigate to the TextActivity
                        hideLoadingUI()
                        startActivity(Intent(applicationContext, TextActivity::class.java))
                    } else {
                        val postMap: MutableMap<String, Any> = HashMap()
                        postMap["uid"] = coachData.uid
                        FirebaseFunctions
                            .getInstance()
                            .getHttpsCallable("getMessageArray")
                            .call(postMap)
                            .addOnCompleteListener { getMessageArrayTask: Task<HttpsCallableResult> ->
                                if (!getMessageArrayTask.isSuccessful) {
                                    val e = getMessageArrayTask.exception
                                    if (e is FirebaseFunctionsException) {
                                        val ffe = e
                                        val code = ffe.code
                                        Log.e(
                                            TAG,
                                            "onComplete: getMessageArray: FFE: ",
                                            ffe
                                        )
                                    }
                                    hideLoadingUI()
                                } else {
                                    val conversation: List<Map<String, Any>>? =
                                        getMessageArrayTask.result!!
                                            .data as List<Map<String, Any>>?
                                    for (message in conversation!!) {
                                        TextActivity.conversation.add(
                                            TextMessageItem(
                                                message["message"].toString(),
                                                message["sender"].toString(),
                                                message["dateTime"].toString()
                                            )
                                        )
                                    }
                                    UserConstants.conversations[coachData.uid + UserConstants.ROLE] =
                                        conversation
                                    // Save conversation
                                    SharedPrefsManager.saveConversations(currentActivity)
                                    // Navigate to the TextActivity
                                    hideLoadingUI()
                                    startActivity(
                                        Intent(
                                            applicationContext,
                                            TextActivity::class.java
                                        )
                                    )
                                }
                            }
                    }
                }
                Log.d(TAG, "showCoachProfile: Clicked on message button")
            }
        } else {
            messageButton.visibility = View.GONE
            messageGridTextView.visibility = View.GONE
        }
    }

    override fun onBackPressed() {
        if (!isLoading) {
            disableMessage = false
            super.onBackPressed()
        }
    }

    private fun toTileCase(str: String?): String? {
        if (str == null) {
            return null
        }
        var space = true
        val builder = StringBuilder(str)
        for (i in builder.indices) {
            val c = builder[i]
            if (space) {
                if (!Character.isWhitespace(c)) {
                    // Convert to title case and switch out of whitespace mode.
                    builder.setCharAt(i, Character.toTitleCase(c))
                    space = false
                }
            } else if (Character.isWhitespace(c)) {
                space = true
            } else {
                builder.setCharAt(i, Character.toLowerCase(c))
            }
        }
        return builder.toString()
    }

    private fun callUser(type: String): Task<HttpsCallableResult?> {
        UserConstants.THEIR_UID = coachData.uid
        val ruid = coachData.uid
        val post: MutableMap<String, String> = HashMap()
        post["cuid"] = UserConstants.UID
        post["ruid"] = ruid
        post["type"] = type
        return FirebaseFunctions
            .getInstance()
            .getHttpsCallable("callUser")
            .call(post)
            .continueWith { task ->
                task.result?.data as HttpsCallableResult?
            }
    }

    private fun genAccessToken(): Task<String> {
        val post = HashMap<String, String?>()
        UserConstants.ROOM = sortedString(UserConstants.ROOM).trim { it <= ' ' }
        println(FirebaseAuth.getInstance().currentUser!!.email)
        post["email"] = FirebaseAuth.getInstance().currentUser!!.email
        post["room"] = UserConstants.ROOM
        println(UserConstants.EMAIL)
        return FirebaseFunctions
            .getInstance()
            .getHttpsCallable("accessToken")
            .call(post)
            .continueWith { task ->
                val result = task.result?.data as String
                result
            }
    }

    private fun sortedString(inputString: String): String {
        val arr = inputString.toCharArray()
        Arrays.sort(arr)
        return String(arr)
    }

    private fun showLoadingUI(loadingMessage: String) {
        isLoading = true
        videoCallButton.isClickable = false
        voiceCallButton.isClickable = false
        messageButton.isClickable = false
        overlayCoachProfileConstraintLayoutClose.isClickable = false
        loadingCardView.visibility = View.VISIBLE
        loadingProgressBar.visibility = View.VISIBLE
        loadingTextView.visibility = View.VISIBLE
        loadingTextView.text = loadingMessage
    }

    private fun hideLoadingUI() {
        isLoading = false
        videoCallButton.isClickable = true
        voiceCallButton.isClickable = true
        messageButton.isClickable = true
        overlayCoachProfileConstraintLayoutClose.isClickable = true
        loadingCardView.visibility = View.GONE
        loadingProgressBar.visibility = View.GONE
        loadingTextView.visibility = View.GONE
        loadingTextView.text = ""
    }

    companion object {
        var position: Int = 0
        var disableMessage = false
        lateinit var coachData: CoachProfileData
    }
}
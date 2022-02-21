package edu.uwp.appfactory.wishope.views.auth

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import edu.uwp.appfactory.wishope.R
import edu.uwp.appfactory.wishope.models.PermissionRequest
import edu.uwp.appfactory.wishope.utils.SharedPrefsManager
import edu.uwp.appfactory.wishope.utils.UserConstants
import edu.uwp.appfactory.wishope.views.landing.CoachLandingActivity
import edu.uwp.appfactory.wishope.views.landing.RecovereeLandingActivity
import kotlinx.android.synthetic.main.fragment_login.*

/**
 * <h1>Hosts login functionality using Firebase and checks login fields and logs in according to validity.</h1>
 * <p>
 * This class handles the logging in, Firebase Cloud Functions calls, and navigation to the password
 * reset, user registration, and the landing activity.
 * </p>
 *
 * @author Allen Rocha
 * @version 1.9.5
 * @since 15-08-2020
 */
class LoginFragment : Fragment() {

    private val TAG: String? = "LoginFragment"
    private lateinit var auth: FirebaseAuth//declare instance of FirebaseAuth
    private val gettingAccInfoStr: String = "Retrieving account information..."
    private val loggingInStr: String = "Logging you in..."

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth//Initialize Firebase Auth
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fragmentTransaction: FragmentTransaction = parentFragmentManager.beginTransaction()

        // Persistence check
        // If the user did not sign out during their last session,
        // it will automatically resign them in.
        val currentUser = auth.currentUser
        if (currentUser != null && currentUser.isEmailVerified) {
            showLoadingUI(loggingInStr)
            persistenceSignIn(currentUser)
        }

        //Call doLogin() which verifies fields
        buSignIn.setOnClickListener {
            if (videoAndAudioPermission()) {
                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                    == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                    == PackageManager.PERMISSION_GRANTED) {
                    onLoginPressed()
                } else {
                    enableMyLocation()
                    Toast.makeText(
                        requireContext(),
                        "You must accept location sharing to use this app.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    "You must accept camera and microphone permissions to use this app.",
                    Toast.LENGTH_LONG
                ).show()
                val p = PermissionRequest()
                p.setUpPermissions(requireContext(), requireActivity())
            }
        }

        //Take user to register page
        signUpView.setOnClickListener {
            fragmentTransaction
                .replace(
                    R.id.main_fragment_container,
                    RegisterFragment(),
                    "RegisterFragment"
                )
                .addToBackStack(null)
                .commit()
        }

        //Take user to password reset page
        resetView.setOnClickListener {
            fragmentTransaction
                .replace(
                    R.id.main_fragment_container,
                    PasswordResetFragment(),
                    "PasswordResetFragment"
                )
                .addToBackStack(null)
                .commit()
        }


    }

    private fun onLoginPressed() {
        //Check email for empty
        if (email.text.toString().isEmpty()) {
            email.error = "Please enter email"
            email.requestFocus()
            return
        }

        //Check email for formatting
        if (!Patterns.EMAIL_ADDRESS.matcher(email.text.toString()).matches()) {
            email.error = "Please enter valid email"
            email.requestFocus()
            return
        }

        //Check password for empty
        if (password.text.toString().isEmpty()) {
            password.error = "Please enter password"
            password.requestFocus()
            return
        }
        auth.signInWithEmailAndPassword(email.text.toString(), password.text.toString())
            .addOnCompleteListener { signInTask ->
                if (signInTask.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    val currentUser = auth.currentUser
                    Log.d(TAG, "Sign in:success")
                    if (currentUser != null && currentUser.isEmailVerified) {
                        FirebaseMessaging.getInstance().token
                            .addOnCompleteListener { instanceIdTask ->
                                // There was an issue getting the user's FCM token
                                if (!instanceIdTask.isSuccessful) {
                                    Log.e(
                                        TAG,
                                        "doInBackground: " + instanceIdTask.exception.toString()
                                    )
                                    hideLoadingUI()

                                } else {
                                    showLoadingUI(gettingAccInfoStr)
                                    // Get new Instance ID token
                                    UserConstants.FCM_TOKEN = instanceIdTask.result
                                    // Get the user's UID
                                    UserConstants.UID = currentUser.uid
                                    // Get the user's email
                                    UserConstants.EMAIL = currentUser.email
                                    // Get new Instance ID token
                                    val db = FirebaseFirestore.getInstance()
                                    // Get the user's document
                                    val docRef =
                                        db.collection("users")
                                            .document(UserConstants.UID.toString())
                                    docRef.get()
                                        .addOnSuccessListener { document ->
                                            if (document != null) {
                                                // Assigns the user's role
                                                UserConstants.ROLE =
                                                    document.get("role").toString()
                                                UserConstants.STATUS = "offline"
                                                UserConstants.PROFILE_PIC_PATH =
                                                    document.get("profilePic").toString()
                                                UserConstants.FIRST_NAME =
                                                    document.get("firstName").toString()
                                                UserConstants.LAST_NAME =
                                                    document.get("lastName").toString()
                                                UserConstants.COACH_BIO =
                                                    document.get("bio").toString()
                                                UserConstants.COACH_LOCATION =
                                                    document.get("location").toString()

                                                // Set the locally saved call history with the one from the
                                                // Firestore document.
                                                // Get the call history from this user's document
                                                val callHistory: List<Map<String, Any>> =
                                                    document.get("callHistory") as List<Map<String, Any>>
                                                // Check if the last call in the cached call history is the same
                                                // as last one from the document.
                                                UserConstants.callHistory.clear()
                                                val reversed = callHistory.reversed()
                                                for (map: Map<String, Any> in reversed)
                                                    UserConstants.callHistory.add(map)
                                                SharedPrefsManager.saveCallHistory(requireActivity())
                                                FirebaseFirestore
                                                    .getInstance()
                                                    .collection("users")
                                                    .document(UserConstants.UID)
                                                    .update(
                                                        mapOf(
                                                            "fcmToken" to UserConstants.FCM_TOKEN,
                                                        )
                                                    )
                                                    .addOnCompleteListener { navToLanding() }
                                            }
                                        }
                                }
                            }
                    } else {
                        Snackbar
                            .make(
                                fragment_welcome_constraint_layout,
                                "Please verify your email address",
                                BaseTransientBottomBar.LENGTH_LONG
                            )
                            .setAction("OK") {
                                //does nothing so that the user can click to dismiss
                            }
                            .show()
                    }
                } else {
                    // If sign in fails, display a message to the user.
                    Log.e(TAG, "Sign in failed" + signInTask.exception.toString())
                    val errorSnackbar = Snackbar.make(
                        requireView(),
                        "Wrong email or password", BaseTransientBottomBar.LENGTH_LONG
                    )
                    errorSnackbar.setAction("OK") {
                        //does nothing so that the user can click to dismiss
                    }
                    errorSnackbar.show()
                }
            }
    }

    private fun persistenceSignIn(currentUser: FirebaseUser?) {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { instanceIdTask ->
                if (!instanceIdTask.isSuccessful) {
                    Log.e(
                        TAG,
                        "persistenceSignIn: getInstanceId failed" + instanceIdTask.exception.toString()
                    )
                } else {
                    // Get new Instance ID token
                    UserConstants.FCM_TOKEN = instanceIdTask.result
                    // Get user's UID
                    UserConstants.UID = currentUser!!.uid
                    // Get user email
                    UserConstants.EMAIL = currentUser.email
                    val db = FirebaseFirestore.getInstance()
                    val docRef =
                        db.collection("users").document(UserConstants.UID.toString())
                    docRef
                        .get()
                        .addOnSuccessListener { document ->
                            if (document != null) {
                                UserConstants.ROLE = document.get("role").toString()
                                UserConstants.STATUS = document.get("status").toString()
                                UserConstants.PROFILE_PIC_PATH =
                                    document.get("profilePic").toString()
                                UserConstants.FIRST_NAME = document.get("firstName").toString()
                                UserConstants.LAST_NAME = document.get("lastName").toString()
                                UserConstants.COACH_BIO = document.get("bio").toString()
                                UserConstants.COACH_LOCATION = document.get("location").toString()
                                // Check if this user has any previous calls
                                if ((document.get("callHistory") as List<Map<String, Any>>).isNotEmpty()) {
                                    // Get the call history from this user's document
                                    val callHistory: List<Map<String, Any>> =
                                        document.get("callHistory") as List<Map<String, Any>>
                                    // Check if the last call in the cached call history is the same
                                    // as last one from the document.
                                    UserConstants.callHistory.clear()
                                    val reversed = callHistory.reversed()
                                    for (map: Map<String, Any> in reversed)
                                        UserConstants.callHistory.add(map)
                                }
                                // Update this user's presence and FCM token.
                                FirebaseFirestore
                                    .getInstance()
                                    .collection("users")
                                    .document(UserConstants.UID)
                                    .update(
                                        mapOf(
                                            "fcmToken" to UserConstants.FCM_TOKEN,
                                        )
                                    )
                                    .addOnCompleteListener { navToLanding() }
                            }
                        }
                }
            }
    }

    /**
     * Will show the loading message when the Firebase Cloud Functions are executing.
     *
     * @param loadingMessage    Message to display where the Exception was raised.
     */
    private fun showLoadingUI(loadingMessage: String) {
        email.isFocusable = false
        password.isFocusable = false
        resetView.isClickable = false
        buSignIn.isClickable = false
        signUpView.isClickable = false
        loginLoadingCardView.visibility = View.VISIBLE
        loginLoadingProgressBar.visibility = View.VISIBLE
        loginLoadingTextView.visibility = View.VISIBLE
        loginLoadingTextView.text = loadingMessage
    }

    /**
     * Will hide the loading message after the Firebase Cloud Functions are finished.
     *
     */
    private fun hideLoadingUI() {
        email.isFocusableInTouchMode = true
        password.isFocusableInTouchMode = true
        resetView.isClickable = true
        buSignIn.isClickable = true
        signUpView.isClickable = true
        loginLoadingCardView.visibility = View.GONE
        loginLoadingProgressBar.visibility = View.GONE
        loginLoadingTextView.visibility = View.GONE
        loginLoadingTextView.text = ""
    }

    /**
     * Navigates the user to their respective landing activity.
     *
     */
    private fun navToLanding() {
        SharedPrefsManager.loadConversations(requireActivity())
        Thread.sleep(250)
        UserConstants.recentMessagesListener(
            UserConstants.UID, requireActivity()
        )
        if (UserConstants.FIRST_NAME.isNullOrEmpty() || UserConstants.LAST_NAME.isNullOrEmpty()) {
            val fragmentTransaction: FragmentTransaction = parentFragmentManager.beginTransaction()
            fragmentTransaction
                .replace(
                    R.id.main_fragment_container,
                    SetNameFragment(),
                    "SetNameFragment"
                )
                .addToBackStack(null)
                .commit()

        } else {
            when (UserConstants.ROLE.toLowerCase()) {
                "coach" -> {
                    startActivity(
                        Intent(
                            requireActivity(),
                            CoachLandingActivity::class.java
                        )
                    )
                }
                "recoveree" -> {
                    startActivity(
                        Intent(
                            requireActivity(),
                            RecovereeLandingActivity::class.java
                        )
                    )
                }
                else -> {
                    startActivity(
                        Intent(
                            requireActivity(),
                            RecovereeLandingActivity::class.java
                        )
                    )
                }
            }
        }
    }

    /**
     * Check for camera and record audio permissions
     */
    private fun videoAndAudioPermission(): Boolean {
        Log.d(TAG, "videoAndAudioPermission: ")
        return (ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED)
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED
        )
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED
        )
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
    }

    companion object {
        /**
         * Request code for location permission request.
         *
         * @see .onRequestPermissionsResult
         */
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}

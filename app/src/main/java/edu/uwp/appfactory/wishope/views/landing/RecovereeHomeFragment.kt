package edu.uwp.appfactory.wishope.views.landing

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.HttpsCallableResult
import com.google.firebase.ktx.Firebase
import edu.uwp.appfactory.wishope.R
import edu.uwp.appfactory.wishope.utils.SharedPrefsManager
import edu.uwp.appfactory.wishope.utils.UserConstants
import edu.uwp.appfactory.wishope.views.CommunicateActivity
import edu.uwp.appfactory.wishope.views.auth.MainActivity
import edu.uwp.appfactory.wishope.views.landing.adapters.OnlineUserAdapter
import edu.uwp.appfactory.wishope.views.landing.items.CoachProfileData
import kotlinx.android.synthetic.main.fragment_recoveree_home.*

/**
 * <h1>This class hosts the home fragment.</h1>
 * <p>
 * </p>
 *
 * @author Allen Rocha
 * @version 1.9.5
 * @since 15-08-2020
 */
class RecovereeHomeFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var TAG = "RecovereeHomeFragment"
    private lateinit var onlineUsersRecyclerView: RecyclerView
    private lateinit var onlineUsersAdapter: OnlineUserAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        db = FirebaseFirestore.getInstance()
        UserConstants.ONLINE_USERS.clear()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_recoveree_home, container, false)
        onlineUsersRecyclerView = view.findViewById(R.id.onlineUsersRecyclerView)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeOnlineCoachesRecyclerView()
        showLoadingUI("One moment please...")
        refreshtable()
        val docRef = db.collection("users").whereEqualTo("role","coach").addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w(TAG, "Listen failed.", e)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                refreshtable()
            } else {
                Log.d(TAG, "Current data: null")
            }
        }

        swipeOnlineUsersRecyclerView.setProgressBackgroundColorSchemeColor(resources.getColor(R.color.colorPrimaryDark))
        swipeOnlineUsersRecyclerView.setColorSchemeColors(Color.WHITE)

        swipeOnlineUsersRecyclerView.setOnRefreshListener {
            swipeOnlineUsersRecyclerView.isRefreshing = true
            val mFunction = FirebaseFunctions.getInstance()
            mFunction
                .getHttpsCallable("onlineUsers")
                .call()
                .continueWith { httpsCallableResultTask: Task<HttpsCallableResult?> ->
                    val result = httpsCallableResultTask.result
                    result!!.data
                }
                .addOnCompleteListener { task: Task<Any?> ->
                    if (!task.isSuccessful) {
                        val e = task.exception
                        if (e is FirebaseFunctionsException) {
                            val ffe =
                                e
                            val code = ffe.code
                            val details = ffe.details
                            Log.e("GetOnlineUsers:FFE: ", ffe.toString())
                            Log.e("GetOnlineUsers:CODE: ", code.toString())
                        } else e!!.printStackTrace()
                    } else {
                        val userMapList =
                            task.result as ArrayList<HashMap<String, String>>?
                        val newData: ArrayList<CoachProfileData> = ArrayList()
                        for (userMap in userMapList!!) {
                            if (userMap["uid"] != UserConstants.UID) {
                                val tempUser = CoachProfileData(userMap["uid"])
                                if (!UserConstants.ONLINE_USERS.contains(tempUser)) {
                                    newData.add(
                                        CoachProfileData(
                                            userMap["firstName"],
                                            userMap["lastName"],
                                            userMap["uid"],
                                            userMap["profilePic"],
                                            userMap["bio"],
                                            userMap["location"],
                                            userMap["email"],
                                            userMap["status"],
                                        )
                                    )
                                } else
                                    newData.add(
                                        UserConstants.ONLINE_USERS[UserConstants.ONLINE_USERS.indexOf(
                                            tempUser
                                        )]
                                    )
                            }
                        }
                        UserConstants.ONLINE_USERS.clear()
                        UserConstants.ONLINE_USERS.addAll(newData)
                        initializeOnlineCoachesRecyclerView()
                        hideLoadingUI()
                    }
                }
        }



        //Sign out and go back to login
        logout_button.setOnClickListener {
            showLoadingUI("One moment please...")
            // Remove your FCM token from Firestore
            clearFCM()
                ?.addOnCompleteListener { setFCMTask: Task<HttpsCallableResult?> ->
                    if (setFCMTask.isSuccessful) {
                        // Set your presence to offline on Firestore
                        clearPresence()
                            ?.addOnCompleteListener { updatePresenceTask: Task<HttpsCallableResult?> ->
                                if (updatePresenceTask.isSuccessful) {
                                    SharedPrefsManager.clearConversations(requireActivity())
                                    SharedPrefsManager.clearCallHistory(requireActivity())

                                    // Sign the user out
                                    FirebaseAuth.getInstance().signOut()

                                    // Hide loading UI
                                    hideLoadingUI()

                                    // Navigate to the LoginFragment
                                    navigateToStart()
                                } else {
                                    // There was an issue executing the `updateUserPresence` method.
                                    updatePresenceTask.exception?.let { ex ->
                                        genSnackBar(
                                            ex,
                                            "Error updating presence. Please try again."
                                        )
                                    }
                                }
                            }
                    } else {
                        // There was an issue executing the `setFCM` method.
                        setFCMTask.exception?.let { ex ->
                            genSnackBar(
                                ex,
                                "Error removing token. Please try again."
                            )
                        }
                    }
                }
        }
    }

    /**
     * This method loads the view for the recoveree to see the list of coaches online.
     */
    private fun initializeOnlineCoachesRecyclerView() {
        linearLayoutManager = LinearLayoutManager(requireContext())
        onlineUsersAdapter =
            OnlineUserAdapter(
                requireActivity()
            )
        onlineUsersRecyclerView.layoutManager = linearLayoutManager
        onlineUsersRecyclerView.adapter = onlineUsersAdapter
        onlineUsersAdapter.setOnItemClickListener {
            showCoachProfile(it)
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity).supportActionBar?.title = "Home"//Set the fragment title
    }

    /**
     * Method shows the loading icon when the view is refreshed or a new intent is loading
     */
    private fun showLoadingUI(loadingMessage: String) {
        loadingCardView.visibility = View.VISIBLE
        loadingProgressBar.visibility = View.VISIBLE
        loadingTextView.visibility = View.VISIBLE
        loadingTextView.text = loadingMessage
    }

    /**
     * Hides the loading icon indicating it has been completed
     */
    private fun hideLoadingUI() {
        loadingCardView.visibility = View.GONE
        loadingProgressBar.visibility = View.GONE
        if (swipeOnlineUsersRecyclerView.isRefreshing)
            swipeOnlineUsersRecyclerView.isRefreshing = false
        loadingTextView.visibility = View.GONE
        loadingTextView.text = ""
    }

    private fun showCoachProfile(position: Int) {
        CommunicateActivity.coachData = UserConstants.ONLINE_USERS[position]
        CommunicateActivity.position = position
        startActivity(Intent(requireContext(), CommunicateActivity::class.java))
    }

    /**
     * This method call the Firebase Cloud Function `setFCM` to remove this user's FCM.
     */
    private fun clearFCM(): Task<HttpsCallableResult?>? {
        val functions = FirebaseFunctions.getInstance()
        val fcmPost: MutableMap<String, Any> = HashMap()
        fcmPost["token"] = ""
        return functions
            .getHttpsCallable("setFCM")
            .call(fcmPost)
    }

    /**
     * This method call the Firebase Cloud Function `updateUserPresence` to set this user's status
     * to offline.
     */
    private fun clearPresence(): Task<HttpsCallableResult?>? {
        val functions = FirebaseFunctions.getInstance()
        val presencePost: MutableMap<String, Any> = HashMap()
        presencePost["status"] = "offline"
        return functions
            .getHttpsCallable("updateUserPresence")
            .call(presencePost)
    }

    /**
     * This function will generate a SnackBar to notify the user that there was an error executing
     * the log out code.
     *
     * @param exception exception from the created in the Firebase Function.
     * @param message   Message to display where the Exception was raised.
     */
    private fun genSnackBar(exception: Exception, message: String) {
        if (exception is FirebaseFunctionsException) {
            Snackbar
                .make(
                    requireView(),
                    message,
                    BaseTransientBottomBar.LENGTH_LONG
                )
                .setAction("Ok") { snackBarView: View? -> }
                .show()
        }
    }

    /**
     * This method is called after the user signs out and it returns the to the MainActivity.
     */
    private fun navigateToStart() {
        startActivity(Intent(requireActivity(), MainActivity::class.java))
    }

    /**
     * This method is called when the snapshot listener is triggered. Updates online coaches.
     */
    fun refreshtable(){
            val mFunction = FirebaseFunctions.getInstance()
            mFunction
                .getHttpsCallable("onlineUsers")
                .call()
                .continueWith { httpsCallableResultTask: Task<HttpsCallableResult?> ->
                    val result = httpsCallableResultTask.result
                    result!!.data
                }
                .addOnCompleteListener { task: Task<Any?> ->
                    if (!task.isSuccessful) {
                        val e = task.exception
                        if (e is FirebaseFunctionsException) {
                            val ffe =
                                e
                            val code = ffe.code
                            val details = ffe.details
                            Log.e("GetOnlineUsers:FFE: ", ffe.toString())
                            Log.e("GetOnlineUsers:CODE: ", code.toString())
                            //                            Log.e("GetOnlineUsers:DETAILS: ", (String) details);
                        } else e!!.printStackTrace()
                    } else {
                        val userMapList =
                            task.result as ArrayList<HashMap<String, String>>?
                        val newData: ArrayList<CoachProfileData> = ArrayList()
                        for (userMap in userMapList!!) {
                            if (userMap["uid"] != UserConstants.UID) {
                                val tempUser = CoachProfileData(userMap["uid"])
                                if (!UserConstants.ONLINE_USERS.contains(tempUser)) {
                                    newData.add(
                                        CoachProfileData(
                                            userMap["firstName"],
                                            userMap["lastName"],
                                            userMap["uid"],
                                            userMap["profilePic"],
                                            userMap["bio"],
                                            userMap["location"],
                                            userMap["email"],
                                            userMap["status"],
                                        )
                                    )
                                } else
                                    newData.add(
                                        UserConstants.ONLINE_USERS[UserConstants.ONLINE_USERS.indexOf(
                                            tempUser
                                        )]
                                    )
                            }
                        }
                        UserConstants.ONLINE_USERS.clear()
                        UserConstants.ONLINE_USERS.addAll(newData)
                        initializeOnlineCoachesRecyclerView()
                        hideLoadingUI()
                    }
                }
    }

    /**
     * Keeping just in case seems refreshtable() just works better in all situations
     */
    fun loadtable(){
        GETOnlineUsers()
            .addOnCompleteListener { task: Task<HttpsCallableResult?> ->
                if (!task.isSuccessful) {
                    val e = task.exception
                    if (e is FirebaseFunctionsException) {
                        val ffe =
                            e
                        val code = ffe.code
                        val details = ffe.details
                        Log.e("GetOnlineUsers:FFE: ", ffe.toString())
                        Log.e("GetOnlineUsers:CODE: ", code.toString())
                    } else e!!.printStackTrace()
                } else {
                    val userMapList =
                        task.result?.data as ArrayList<HashMap<String, String>>?
                    for (userMap in userMapList!!) {
                        if (userMap["uid"] != UserConstants.UID) {
                            val user =
                                CoachProfileData(
                                    userMap["firstName"],
                                    userMap["lastName"],
                                    userMap["uid"],
                                    userMap["profilePic"],
                                    userMap["bio"],
                                    userMap["location"],
                                    userMap["email"],
                                    userMap["status"],
                                )
                            UserConstants.ONLINE_USERS.add(user)
                        }
                    }
                }
                linearLayoutManager = LinearLayoutManager(requireContext())
                onlineUsersAdapter =
                    OnlineUserAdapter(
                        requireActivity()
                    )
                onlineUsersRecyclerView.layoutManager = linearLayoutManager
                onlineUsersRecyclerView.adapter = onlineUsersAdapter
                onlineUsersAdapter.setOnItemClickListener {
                    showCoachProfile(it)
                }
                GETDisplayName()
                    .addOnCompleteListener { task: Task<String?> ->
                        if (!task.isSuccessful) {
                            val e = task.exception
                            if (e is FirebaseFunctionsException) {
                                val ffe =
                                    e
                                val code =
                                    ffe.code
                                val details = ffe.details
                                Log.e(
                                    "fragment_login:GETDisplayName ",
                                    e.toString()
                                )
                                Log.e(
                                    "fragment_login:GETDisplayName ",
                                    code.toString()
                                )
                                try {
                                    Log.e(
                                        "fragment_login:GETDisplayName ",
                                        details as String?
                                    )
                                } catch (ignored: NullPointerException) {
                                }
                            }
                        } else
                            UserConstants.YOUR_DISPLAY_NAME = task.result
                        hideLoadingUI()
                    }
            }
        refreshtable()
    }
    
    /**
     * This method calls the Firebase Cloud Function "onlineUsers" to load the
     * recycler view of online coaches
     */
    fun GETOnlineUsers(): Task<HttpsCallableResult> {
        return FirebaseFunctions.getInstance()
            .getHttpsCallable("onlineUsers")
            .call()
    }

    /**
     * This method calls the Firebase Cloud Function "userDisplayName" to retrieve
     * the respective coaches' display name
     */
    private fun GETDisplayName(): Task<String?> {
        return FirebaseFunctions
            .getInstance()
            .getHttpsCallable("userDisplayName")
            .call(mapOf("uid" to UserConstants.UID))
            .continueWith { task: Task<HttpsCallableResult?> ->
                val result = task.result
                val data =
                    result!!.data as String?
                data
            }
    }
}

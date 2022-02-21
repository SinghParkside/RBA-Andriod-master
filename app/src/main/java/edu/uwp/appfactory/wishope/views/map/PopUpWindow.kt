package edu.uwp.appfactory.wishope.views.map


import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firestore.v1.DocumentTransform
import edu.uwp.appfactory.wishope.R
import edu.uwp.appfactory.wishope.utils.UserConstants
import kotlinx.android.synthetic.main.activity_pop_up_window.*
import java.sql.Timestamp
import java.util.*

/**
 * <h1>.</h1>
 * <p>
 *     Window used to report a Map icon
 * </p>
 *
 * @author Nick Apicelli
 * @version 1.9.5
 * @since 04-21-2021
 */

class PopUpWindow : AppCompatActivity() {
    private var db = FirebaseFirestore.getInstance()
    private var uid = FirebaseAuth.getInstance()
    private var popupTitle = ""
    private var popupAddress = ""
    private var popupPhone = ""
    private var popupZipcode = ""
    private var popupType = ""
    private var popupWebsite = ""
    private var popupDescription = ""
    private var userEmail = uid.currentUser!!.email.toString()
    private var userFullName = UserConstants.FIRST_NAME + " " + UserConstants.LAST_NAME
    private var issueDate = Calendar.getInstance().time.toString()
    private var darkStatusBar = false
    private var TAG = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Bundle got from Map fragment of tab info
        val bundle = intent.extras
        popupTitle = bundle?.getString("Title", "Title") ?: ""
        popupAddress = bundle?.getString("Address", "Address") ?: ""
        popupPhone = bundle?.getString("Phone", "Phone") ?: ""
        popupWebsite = bundle?.getString("Web", "Web") ?: ""
        popupType = bundle?.getString("Type", "Type") ?: ""

        setContentView(R.layout.activity_pop_up_window)

        //change all the text to match the map tab info
        titleTextViewReport.text = popupTitle
        addressTextViewReport.text = popupAddress
        phoneTextViewReport.text = popupPhone
        webTextViewReport.text = popupWebsite



        this.popup_window_button.setOnClickListener {
            popupDescription = editTextDescription.text.toString()

            //hashmap of info about map tab and user info to be sent to firebase
            val docData = hashMapOf(
                "address" to popupAddress,
                "businessName" to popupTitle,
                "case" to "open",
                "email" to userEmail,
                "fullName" to userFullName,
                "issue" to popupDescription,
                "issueDate" to Timestamp(System.currentTimeMillis()),
                "otherInfo" to "Phone: $popupPhone, Website: $popupWebsite, Type: $popupType"

            )
            db.collection("issues").add(docData)
                .addOnSuccessListener { Log.d(TAG, "successful report") }
                .addOnFailureListener { e -> Log.w(TAG, "Error writing report", e) }
            this.popup_window_button.setOnClickListener {
                onBackPressed()
            }
        }
    }
}

package edu.uwp.appfactory.wishope.views.auth

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_LONG
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import edu.uwp.appfactory.wishope.R
import kotlinx.android.synthetic.main.fragment_register.*


/**
 * <h1>This class allows a user to register using an email, password, and a first and last name.</h1>
 *
 * @author Allen Rocha
 * @version 1.9.5
 * @since 15-08-2020
 */
class RegisterFragment : Fragment() {

    private lateinit var auth: FirebaseAuth//declare instance of FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buRegister.setOnClickListener {
            signUpUser()
        }
    }

    /**
     * Checks for valid field inputs
     */
    private fun signUpUser() {

        val fragmentTransaction: FragmentTransaction = parentFragmentManager.beginTransaction()

        //Check email for empty
        if (registerEmail.text.toString().isEmpty()) {
            registerEmail.error = "Please enter email"
            registerEmail.requestFocus()
            return
        }

        if (registerEmail.text.toString().length > 64) {
            registerEmail.error = "Over character limit"
            password.requestFocus()
            return
        }

        //Check email for formatting
        if (!Patterns.EMAIL_ADDRESS.matcher(registerEmail.text.toString()).matches()) {
            registerEmail.error = "Please enter valid email"
            registerEmail.requestFocus()
            return
        }

        for(char in registerEmail.text.toString()){
            if(!(char.isLetterOrDigit() || char == '!' || char == '@' || char == '#' || char == '$' || char == '%' || char == '*' || char == '-' || char == '?' || char == '_' || char == '.')){
                registerEmail.error = "Email contains invalid characters"
                registerEmail.requestFocus()
                return
            }
        }

        //Check password for empty
        if (password.text.toString().isEmpty()) {
            password.error = "Please enter password"
            password.requestFocus()
            return
        }

        if (password.text.toString().length < 8) {
            password.error = "Password must be 8 characters or longer"
            password.requestFocus()
            return
        }

        if (password.text.toString().length > 64) {
            password.error = "Password over character limit"
            password.requestFocus()
            return
        }

        //no repeats of 4 characters in a row
        var char1 = '1'
        var char2 = '2'
        var char3 = '3'
        var char4 = '4'
        for(char in password.text.toString()){
            char1 = char2;
            char2 = char3;
            char3 = char4;
            char4 = char;
            if(char1 == char2 && char2 == char3 && char3 == char4) {
                password.error = "Too many repeated characters in a row"
                password.requestFocus()
                return
            }
        }
        var alpha = false;
        var numer = false;
        var special = false;
        for(char in password.text.toString()){
            if(char.isLetter()){
                alpha = true
            }
            if(char.isDigit()){
                numer = true
            }
            if(char == '!' || char == '@' || char == '#' || char == '$' || char == '%' || char == '*' || char == '-' || char == '?'){
                special = true
            }

        }
        if(!(alpha && numer && special)) {
            password.error = "Password must contain a number, letter, and special character"
            password.requestFocus()
            return
        }
        //Check first name for empty
        if (firstName.text.toString().isEmpty()) {
            firstName.error = "First name cannot be empty"
            firstName.requestFocus()
            return
        }

        if (firstName.text.toString().length > 64) {
            firstName.error = "Over character limit"
            firstName.requestFocus()
            return
        }

        //Check first name for non letters
        for(char in firstName.text.toString()){
            if(!(char.isLetter())){
                firstName.error = "Names may only contain letters"
                firstName.requestFocus()
                return
            }
        }

        //Check last name for empty
        if (lastName.text.toString().isEmpty()) {
            lastName.error = "First name cannot be empty"
            lastName.requestFocus()
            return
        }

        if (lastName.text.toString().length > 64) {
            lastName.error = "Over character limit"
            lastName.requestFocus()
            return
        }

        //Check last name for non letters
        for(char in lastName.text.toString()){
            if(!(char.isLetter())){
                lastName.error = "Names may only contain letters."
                lastName.requestFocus()
                return
            }
        }


        //Create the user
        auth.createUserWithEmailAndPassword(registerEmail.text.toString(), password.text.toString())
            .addOnCompleteListener { createUserTask ->
                showLoadingUI()
                if (createUserTask.isSuccessful) {
                    Log.d(TAG, "createUserWithEmail:success")
                    val userDisplayName = userProfileChangeRequest {
                        displayName = firstName.text.toString() + " " + lastName.text.toString()
                    }
                    createUserTask.result!!.user!!.updateProfile(userDisplayName)
                        .addOnCompleteListener{
                            hideLoadingUI()
                            if (it.isSuccessful) {
                                val user = auth.currentUser
                                user?.sendEmailVerification()
                                    ?.addOnCompleteListener { sendVerificationEmailTask ->
                                        if (sendVerificationEmailTask.isSuccessful) {
                                            Log.d(TAG, "Email sent.")
                                            fragmentTransaction
                                                .replace(
                                                    R.id.main_fragment_container,
                                                    LoginFragment(),
                                                    "LoginFragment"
                                                )
                                                .addToBackStack(null)
                                                .commit()
//                                    }
                                        } else
                                            hideLoadingUI()
                                    }
                            }
                        }
                } else {
                    hideLoadingUI()
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "createUserWithEmail:failure", createUserTask.exception)

                    val errorSnackbar = Snackbar.make(
                        requireView(),
                        "Signup failed", LENGTH_LONG
                    )
                    errorSnackbar.setAction("OK", View.OnClickListener {
                        //does nothing so that the user can click to dismiss
                    })
                    errorSnackbar.show()
                }
            }
    }

    private fun showLoadingUI() {
        registerLoadingCardView.visibility = View.VISIBLE
        registerLoadingTextView.text = "One moment please"
        buRegister.isClickable = false
        registerEmail.isFocusable = false
        password.isFocusable = false
        firstName.isFocusable = false
        lastName.isFocusable = false
    }

    private fun hideLoadingUI() {
        registerLoadingCardView.visibility = View.GONE
        registerLoadingTextView.text = ""
        buRegister.isClickable = true
        registerEmail.isFocusableInTouchMode = true
        password.isFocusableInTouchMode = true
        firstName.isFocusableInTouchMode = true
        lastName.isFocusableInTouchMode = true
    }
}

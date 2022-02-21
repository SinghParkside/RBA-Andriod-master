package edu.uwp.appfactory.wishope.views.auth

import android.content.Context
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import edu.uwp.appfactory.wishope.R
import kotlinx.android.synthetic.main.fragment_password_reset.*


/**
 * <h1>Hosts password resetting functionality.</h1>
 * <p>
 * This class handles resetting a users password using the provided email.
 * </p>
 *
 * @author Allen Rocha
 * @version 1.9.5
 * @since 15-08-2020
 */
class PasswordResetFragment : Fragment() {

    private lateinit var auth: FirebaseAuth//declare instance of FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()//Initialize Firebase Auth
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_password_reset, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buResetPassword.setOnClickListener {

            //Check email for empty
            var validEmail = checkEmail()

            if (validEmail) {
                //Create a dialog that facilitates proceeding with
                //reset email or cancelling

                val inputManager =
                    requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputManager.hideSoftInputFromWindow(
                    requireActivity().currentFocus!!.windowToken,
                    InputMethodManager.HIDE_NOT_ALWAYS
                )
                createDialog()
            }

        }
    }

    /**
     * Function for testing if an email input is valid
     */
    private fun checkEmail(): Boolean {
        if (emailReset.text.toString().isNullOrEmpty()) {
            emailReset.error = "Please enter email"
            emailReset.requestFocus()
            return false
        }

        //Check email for formatting
        if (!Patterns.EMAIL_ADDRESS.matcher(emailReset.text.toString()).matches()) {
            emailReset.error = "Please enter valid email"
            emailReset.requestFocus()
            return false
        }

        return true
    }

    /**
     * Function for creating a dialog to verify
     * if a user wants to proceed with resetting email or cancelling.
     */
    private fun createDialog() {
        val dialogBuilder = android.app.AlertDialog.Builder(requireContext())

        dialogBuilder.setMessage("Are you sure you want to send a password reset to this email?")
            .setCancelable(true)

            //Continue and perform functions
            .setPositiveButton("Yes") { dialog, which ->
                //disable button, change color
                buResetPassword.isClickable = false
                buResetPassword.setBackgroundResource(R.drawable.oval_button_gray)

                //Send email
                auth.sendPasswordResetEmail(emailReset.text.toString())
                    .addOnCompleteListener {
                        //Snackbar about email sent
                        Snackbar
                            .make(
                                resetScrollViewLayout,
                                "Password reset sent to email", BaseTransientBottomBar.LENGTH_LONG
                            )
                            .setAction("OK") {
                                parentFragmentManager
                                    .beginTransaction()
                                    .replace(
                                        R.id.main_fragment_container,
                                        LoginFragment(),
                                        "LoginFragment"
                                    )
                                    .addToBackStack(null)
                                    .commit()
                            }
                            .show()
                    }
                    .addOnFailureListener {
                        //Snackbar about email sent
                        Snackbar
                            .make(
                                resetScrollViewLayout,
                                "Error resetting password." + it.message,
                                BaseTransientBottomBar.LENGTH_LONG
                            )
                            .setAction("OK") {
                                //does nothing so that the user can click to dismiss
                            }
                            .show()
                    }

            }

            //Cancel the dialog, do nothing
            .setNegativeButton("Cancel") { dialog, which ->
                dialog.cancel()
            }

        val alert = dialogBuilder.create()

        alert.setTitle("Confirm email")
        alert.show()
    }
}

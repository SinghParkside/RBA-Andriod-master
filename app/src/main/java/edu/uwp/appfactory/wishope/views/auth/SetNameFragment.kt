package edu.uwp.appfactory.wishope.views.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import edu.uwp.appfactory.wishope.R
import edu.uwp.appfactory.wishope.utils.UserConstants
import edu.uwp.appfactory.wishope.views.landing.CoachLandingActivity
import edu.uwp.appfactory.wishope.views.landing.RecovereeLandingActivity
import kotlinx.android.synthetic.main.fragment_set_name.*

class SetNameFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            val fragmentTransaction: FragmentTransaction = parentFragmentManager.beginTransaction()
            fragmentTransaction
                .replace(
                    R.id.main_fragment_container,
                    LoginFragment(),
                    "LoginFragment"
                )
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_set_name, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        buContinue.setOnClickListener {
            setDisplayName()
        }
    }

    private fun setDisplayName() {
        if (firstName.text.isNullOrEmpty()) {
            firstName.error = "Please a first name"
            firstName.requestFocus()
            return
        }
        if (lastName.text.isNullOrEmpty()) {
            lastName.error = "Please a last name"
            lastName.requestFocus()
            return
        }
        if (firstName.text.toString().length > 20) {
            firstName.error = "First name is too long! Please shorten it!"
            firstName.requestFocus()
            return
        }
        if (lastName.text.toString().length > 20) {
            lastName.error = "Last name is too long! Please shorten it!"
            lastName.requestFocus()
            return
        }
        showLoadingUI()
        val db = FirebaseFirestore.getInstance()

        val user = mapOf(
            "firstName" to firstName.text.toString(),
            "lastName" to lastName.text.toString()
        )
        db.collection("users")
            .document(auth.currentUser!!.uid)
            .update(user)
            .addOnCompleteListener {
                hideLoadingUI()
                if (it.isSuccessful) {
                    Toast.makeText(requireContext(), "Success!", Toast.LENGTH_LONG).show()
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
                } else {
                    Toast.makeText(requireContext(), "Error setting name!", Toast.LENGTH_LONG)
                        .show()
                }
            }
    }


    private fun showLoadingUI() {
        setNameLoadingCardView.visibility = View.VISIBLE
        setNameLoadingTextView.text = "One moment please"
        buContinue.isClickable = false
        firstName.isFocusable = false
        firstName.isFocusable = false
    }

    private fun hideLoadingUI() {
        setNameLoadingCardView.visibility = View.GONE
        setNameLoadingTextView.text = ""
        buContinue.isClickable = true
        firstName.isFocusableInTouchMode = true
        lastName.isFocusableInTouchMode = true
    }
}
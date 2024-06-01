package com.example.billbook

import android.app.ProgressDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.billbook.databinding.ActivityEditProfileBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressDialog = ProgressDialog(this).apply {
            setMessage("Loading...")
            setCancelable(false)
        }

        // Ensure 'main' view exists and is correctly referenced
        val mainView: View? = findViewById(R.id.main)
        mainView?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        } ?: run {
            Log.e("EditProfileActivity", "View with ID 'main' not found")
        }

        firebaseAuth = FirebaseAuth.getInstance()

        binding.editShopInfo.setOnClickListener {
            progressDialog.show()
            updateDetails()
        }
    }

    private fun updateDetails() {
        val uid = firebaseAuth.currentUser?.uid
        if (uid != null) {
            val userRef = FirebaseFirestore.getInstance().collection("Shops")
                .document("Users")
                .collection("User Data")
                .document(uid)

            val name = binding.editName.text.toString()
            val phone = binding.editNumber.text.toString()
            val address = binding.editAddress.text.toString()

            val updates = hashMapOf(
                "name" to name,
                "phone" to phone,
                "address" to address
            )

            userRef.update(updates as Map<String, Any>).addOnCompleteListener { task ->
                progressDialog.dismiss()
                if (task.isSuccessful) {
                    Snackbar.make(binding.root, "Details Updated", Snackbar.LENGTH_SHORT).show()
                } else {
                    Snackbar.make(binding.root, "Failed to Update", Snackbar.LENGTH_SHORT).show()
                }
            }
        } else {
            progressDialog.dismiss()
            Snackbar.make(binding.root, "User not logged in", Snackbar.LENGTH_SHORT).show()
        }
    }
}

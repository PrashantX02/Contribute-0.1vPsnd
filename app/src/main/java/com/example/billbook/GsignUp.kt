package com.example.billbook

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.billbook.databinding.ActivityGsignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso

class GsignUp : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    private lateinit var binding: ActivityGsignUpBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestoreHelper: FirebaseFirestore
    private var imageUri: Uri? = null

    private lateinit var progressDialog: ProgressDialog

    private val PICK_IMAGE_REQUEST = 1011

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityGsignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val rootView = binding.root
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        firebaseAuth = FirebaseAuth.getInstance()
        firestoreHelper = FirebaseFirestore.getInstance()

        // Initialize the progress dialog
        progressDialog = ProgressDialog(this).apply {
            setMessage("Loading...")
            setCancelable(false)
        }

        binding.textView.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
        }

        binding.userImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        binding.button.setOnClickListener {
            progressDialog.show()
            val name = binding.nameEt.text.toString()
            val phone = binding.numberEt.text.toString()
            val address = binding.pinEt.text.toString()
            val pass = binding.passET.text.toString()
            val confirmPass = binding.confirmPassEt.text.toString()

            if (name.isNotEmpty() && phone.isNotEmpty() && address.isNotEmpty() && pass.isNotEmpty() && confirmPass.isNotEmpty()) {
                if (pass == confirmPass) {
                    createUser(pass, name, phone, address)
                } else {
                    progressDialog.dismiss()
                    Toast.makeText(this, "Password is not matching", Toast.LENGTH_SHORT).show()
                }
            } else {
                progressDialog.dismiss()
                Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.data?.let {
                imageUri = it
                Picasso.get().load(it).into(binding.userImage)
            }
        }
    }

    private fun createUser(pass: String, name: String, phone: String, address: String) {
        firebaseAuth.createUserWithEmailAndPassword(phone + "@example.com", pass) // Using phone as email
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = firebaseAuth.currentUser?.uid

                    if (userId != null) {
                        uploadImageAndSaveUser(userId, name, phone, address)
                    } else {
                        progressDialog.dismiss()
                        Toast.makeText(this, "User ID is null", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    progressDialog.dismiss()
                    Toast.makeText(this, "Error creating user: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun uploadImageAndSaveUser(userId: String, name: String, phone: String, address: String) {
        imageUri?.let { uri ->
            val storageReference = FirebaseStorage.getInstance().reference.child("profile_pic").child(userId)
            storageReference.putFile(uri)
                .addOnSuccessListener {
                    storageReference.downloadUrl.addOnSuccessListener { downloadUri ->
                        val imageUrl = downloadUri.toString()
                        saveUserToFirestore(userId, name, phone, address, imageUrl)
                    }.addOnFailureListener { e ->
                        progressDialog.dismiss()
                        Toast.makeText(this, "Error getting download URL: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener { e ->
                    progressDialog.dismiss()
                    Toast.makeText(this, "Error uploading image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } ?: run {
            progressDialog.dismiss()
            Toast.makeText(this, "Image URI is null", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveUserToFirestore(userId: String, name: String, phone: String, address: String, imageUrl: String) {
        val user = hashMapOf(
            "name" to name,
            "phone" to phone,
            "address" to address,
            "img" to imageUrl
        )

        firestoreHelper.collection("Shops").document("Users")
            .collection("User Data").document(userId).set(user)
            .addOnSuccessListener {
                progressDialog.dismiss()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                Toast.makeText(this, "User Sign Up Successful", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "Error saving user: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

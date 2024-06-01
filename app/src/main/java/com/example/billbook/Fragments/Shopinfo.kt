package com.example.billbook.Fragments

import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.billbook.EditProfileActivity
import com.example.billbook.Login
import com.example.billbook.R
import com.example.billbook.Utils.FirestoreHelper
import com.example.billbook.databinding.AddProductBinding
import com.example.billbook.databinding.FragmentShopinfoBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.squareup.picasso.Picasso
import com.google.android.material.snackbar.Snackbar

class Shopinfo : Fragment() {

    private lateinit var binding: FragmentShopinfoBinding
    private lateinit var firefirebashAuth: FirebaseAuth

    private var imageUrl: String? = null
    private var imageUri: Uri? = null

    private lateinit var progressDialog: ProgressDialog

    private val PICK_IMAGE_REQUEST = 1011

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentShopinfoBinding.inflate(inflater, container, false)

        firefirebashAuth = FirebaseAuth.getInstance()

        // Initialize the progress dialog
        progressDialog = ProgressDialog(requireContext()).apply {
            setMessage("Loading...")
            setCancelable(false)
        }

        fetchDataFromFirebase()

        // Set onClickListener for signing out
        binding.signOutButton.setOnClickListener {
            signOut()
        }

        binding.userImage.setOnClickListener {
            update()
        }

        binding.editShopInfo.setOnClickListener {
            startActivity(Intent(requireActivity(), EditProfileActivity::class.java))
        }

        return binding.root
    }

    private fun fetchDataFromFirebase() {
        progressDialog.show()

        val instance = FirebaseFirestore.getInstance()
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid != null) {
            instance.collection("Shops")
                .document("Users")
                .collection("User Data")
                .document(uid)
                .get()
                .addOnSuccessListener { document ->
                    progressDialog.dismiss()
                    if (document != null && document.exists()) {
                        val name = document.getString("name")
                        val address = document.getString("address")
                        val email = document.getString("email")
                        val phone = document.getString("phone")
                        val image = document.getString("img")

                        binding.name.setText(name)
                        binding.city.setText(address)
                        binding.number.setText(phone)
                        binding.email.setText(email)

                        if (image != null) {
                            Picasso.get().load(image).placeholder(R.drawable.user)
                                .into(binding.userImage)
                        } else {
                            Toast.makeText(context, "Image is null", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        println("Document does not exist")
                    }
                }
                .addOnFailureListener {
                    progressDialog.dismiss()
                    Toast.makeText(context, "Error fetching data: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            progressDialog.dismiss()
            Toast.makeText(context, "User ID is null", Toast.LENGTH_SHORT).show()
        }
    }

    private fun signOut() {
        firefirebashAuth.signOut()
        val intent = Intent(requireActivity(), Login::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.data != null) {
            imageUri = data.data

            val uid = FirebaseAuth.getInstance().currentUser?.uid

            if (uid != null && imageUri != null) {
                val userRef = FirebaseFirestore.getInstance().collection("Shops")
                    .document("Users")
                    .collection("User Data")
                    .document(uid)

                FirebaseStorage.getInstance().reference.child("profile_pic").child(uid)
                    .delete().addOnCompleteListener { task ->
                        if (task.isSuccessful || task.exception is StorageException && (task.exception as StorageException).errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) {

                            val ref = FirebaseStorage.getInstance().reference.child("profile_pic")
                                .child(uid)

                            ref.putFile(imageUri!!).addOnSuccessListener {
                                ref.downloadUrl.addOnSuccessListener { uri ->
                                    val imageUrl = uri.toString()

                                    // Update the Firestore document with the new image URL
                                    val updates = hashMapOf<String, Any>(
                                        "img" to imageUrl
                                    )

                                    userRef.update(updates).addOnCompleteListener { updateTask ->
                                        if (updateTask.isSuccessful) {
                                            Snackbar.make(
                                                binding.root,
                                                "Profile picture updated",
                                                Snackbar.LENGTH_SHORT
                                            ).show()
                                            Picasso.get()
                                                .load(imageUrl)
                                                .placeholder(R.drawable.user)
                                                .into(binding.userImage)
                                        } else {
                                            Snackbar.make(
                                                binding.root,
                                                "Error updating profile picture",
                                                Snackbar.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }.addOnFailureListener { e ->
                                    Snackbar.make(
                                        binding.root,
                                        "Error getting download URL",
                                        Snackbar.LENGTH_SHORT
                                    ).show()
                                }
                            }.addOnFailureListener { e ->
                                Snackbar.make(
                                    binding.root,
                                    "Error uploading image",
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            Snackbar.make(
                                binding.root,
                                "Error deleting old profile picture",
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }
                    }
            } else {
                Snackbar.make(binding.root, "User ID or image URI is null", Snackbar.LENGTH_SHORT)
                    .show()
            }
        }
    }

    fun update() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }
}

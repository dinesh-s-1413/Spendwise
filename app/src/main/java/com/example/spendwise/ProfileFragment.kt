package com.example.spendwise

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.spendwise.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            binding.ivProfilePic.setImageURI(uri)
            binding.tvAvatar.visibility = View.GONE
            // Save URI to SharedPreferences
            saveImageUri(uri.toString())
            Toast.makeText(requireContext(), "Photo updated! ✅", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        loadUserData()
        loadSavedImage()

        // Tap profile pic or camera icon to change photo
        binding.ivProfilePic.setOnClickListener { openImagePicker() }
        binding.tvEditPhoto.setOnClickListener { openImagePicker() }

        // Edit name button
        binding.btnEdit.setOnClickListener { showEditDialog() }

        // Logout button
        binding.btnLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes") { _, _ ->
                    auth.signOut()
                    startActivity(Intent(requireContext(), LoginActivity::class.java))
                    requireActivity().finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        imagePickerLauncher.launch(intent)
    }

    private fun saveImageUri(uri: String) {
        val userId = auth.currentUser?.uid ?: return
        requireContext()
            .getSharedPreferences("SpendWise_Prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("profile_image_$userId", uri)
            .apply()
    }

    private fun loadSavedImage() {
        val userId = auth.currentUser?.uid ?: return
        val savedUri = requireContext()
            .getSharedPreferences("SpendWise_Prefs", Context.MODE_PRIVATE)
            .getString("profile_image_$userId", null)

        if (!savedUri.isNullOrEmpty()) {
            try {
                binding.ivProfilePic.setImageURI(Uri.parse(savedUri))
                binding.tvAvatar.visibility = View.GONE
            } catch (e: Exception) {
                binding.tvAvatar.visibility = View.VISIBLE
            }
        }
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        val email = auth.currentUser?.email ?: ""
        val country = CurrencyHelper.getCountry(requireContext())
        val symbol = CurrencyHelper.getCurrencySymbol(requireContext())
        binding.tvCountry.text = "🌍 $country ($symbol)"

        database.getReference("users").child(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!isAdded || _binding == null) return

                    val name = snapshot.child("name")
                        .getValue(String::class.java) ?: "User"
                    val firstLetter = name.first().uppercaseChar().toString()

                    binding.tvAvatar.text = firstLetter
                    binding.tvName.text = name
                    binding.tvEmail.text = email
                    binding.tvProfileName.text = name
                    binding.tvProfileEmail.text = email
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun showEditDialog() {
        val userId = auth.currentUser?.uid ?: return
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_edit_profile, null)

        val etName = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(
            R.id.etEditName
        )
        val actvCountry = dialogView.findViewById<android.widget.AutoCompleteTextView>(
            R.id.actvEditCountry
        )

        // Pre-fill current name
        etName.setText(binding.tvProfileName.text)

        // Setup country dropdown
        val countries = CurrencyHelper.getCountryNames()
        val countryAdapter = android.widget.ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            countries
        )
        actvCountry.setAdapter(countryAdapter)

        // Pre-fill current country
        val currentCountry = CurrencyHelper.getCountry(requireContext())
        actvCountry.setText(currentCountry, false)

        AlertDialog.Builder(requireContext())
            .setTitle("✏️ Edit Profile")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newName = etName.text.toString().trim()
                val newCountry = actvCountry.text.toString().trim()

                if (newName.isEmpty()) {
                    Toast.makeText(requireContext(),
                        "Name cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (newCountry.isEmpty()) {
                    Toast.makeText(requireContext(),
                        "Please select a country", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Save country and currency locally
                CurrencyHelper.saveCountry(requireContext(), newCountry)
                val symbol = CurrencyHelper.getCurrencySymbol(requireContext())

                // Update Firebase
                val updates = mapOf(
                    "name" to newName,
                    "country" to newCountry,
                    "currency" to symbol
                )
                database.getReference("users").child(userId)
                    .updateChildren(updates)
                    .addOnSuccessListener {
                        // Update UI
                        binding.tvCountry.text = "🌍 $newCountry ($symbol)"
                        Toast.makeText(
                            requireContext(),
                            "Profile updated! ✅",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(),
                            "Update failed!", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun showCountryDialog() {
        val countries = CurrencyHelper.getCountryNames()
        val currentCountry = CurrencyHelper.getCountry(requireContext())
        val currentIndex = countries.indexOf(currentCountry).takeIf { it >= 0 } ?: 0

        AlertDialog.Builder(requireContext())
            .setTitle("🌍 Select Country")
            .setSingleChoiceItems(
                countries.toTypedArray(),
                currentIndex
            ) { dialog, which ->
                val selectedCountry = countries[which]
                CurrencyHelper.saveCountry(requireContext(), selectedCountry)
                val symbol = CurrencyHelper.getCurrencySymbol(requireContext())

                // Save to Firebase too
                val userId = auth.currentUser?.uid ?: return@setSingleChoiceItems
                database.getReference("users").child(userId)
                    .child("country").setValue(selectedCountry)
                database.getReference("users").child(userId)
                    .child("currency").setValue(symbol)

                binding.tvCountry.text = "🌍 $selectedCountry ($symbol)"
                Toast.makeText(
                    requireContext(),
                    "Currency set to $symbol ✅",
                    Toast.LENGTH_SHORT
                ).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
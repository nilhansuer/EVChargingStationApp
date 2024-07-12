package com.nilhansuer.evchargingstationapp

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray

class ProfileActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()
    private lateinit var textViewHi: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        firebaseAuth = FirebaseAuth.getInstance()
        val currentUser: FirebaseUser? = firebaseAuth.currentUser

        textViewHi = findViewById(R.id.textHi)

        currentUser?.uid?.let { userId ->
            db.collection("users").document(userId).get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("name")
                    textViewHi.text = "Hi $name!"
                    textViewHi.textSize = 50f
                    textViewHi.setTextColor(Color.WHITE)
                    textViewHi.setTypeface(textViewHi.typeface, Typeface.BOLD)
                    textViewHi.setPadding(20, 10, 20, 5)
                }
            }
        }

        // Find the Nearest Stations button
        val nearestStationsButton: Button = findViewById(R.id.buttonNearestStations)
        val chargingHistoryButton: Button = findViewById(R.id.buttonChargingHistory)
        val favoriteStationsButton: Button = findViewById(R.id.buttonFavoriteStations)
        val aboutButton: Button = findViewById(R.id.buttonAbout)
        val helpButton: Button = findViewById(R.id.buttonHelp)

        favoriteStationsButton.setOnClickListener {
            val intent = Intent(this, FavoriteActivity::class.java)
            startActivity(intent)
        }

        chargingHistoryButton.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }

        nearestStationsButton.setOnClickListener {
            val intent = Intent(this, NearbyStationsActivity::class.java)
            startActivity(intent)
        }

        aboutButton.setOnClickListener {
            showAboutPopup()
        }

        helpButton.setOnClickListener {
            showHelpPopup()
        }

        val exitButton: ImageView = findViewById(R.id.buttonExit)
        exitButton.setOnClickListener {
            firebaseAuth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showAboutPopup() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.about_layout_dialog) // Your popup layout
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val btnClosePopup = dialog.findViewById<Button>(R.id.buttonOK)
        btnClosePopup.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showHelpPopup() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.help_layout_dialog) // Your popup layout
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val btnClosePopup = dialog.findViewById<Button>(R.id.buttonOK)
        btnClosePopup.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}

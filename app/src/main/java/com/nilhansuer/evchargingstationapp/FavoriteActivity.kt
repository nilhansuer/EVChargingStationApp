package com.nilhansuer.evchargingstationapp

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FavoriteActivity : AppCompatActivity(){

    private lateinit var firebaseAuth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite)
        firebaseAuth = FirebaseAuth.getInstance()
        displayFavoriteStations()

        val exitButton: ImageView = findViewById(R.id.buttonExit)
        exitButton.setOnClickListener {
            firebaseAuth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        val backButton: ImageView = findViewById(R.id.buttonBack)
        backButton.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

    }

    private fun displayFavoriteStations() {
        val userId = firebaseAuth.currentUser?.uid ?: return

        val favoriteListLayout = findViewById<LinearLayout>(R.id.favoriteListLayout)
        favoriteListLayout.removeAllViews()

        db.collection("favoriteStations").document(userId).get()
            .addOnSuccessListener { documentSnapshot ->
                val favoriteStations = if (documentSnapshot.exists()) {
                    (documentSnapshot.get("stations") as? List<String>) ?: emptyList()
                } else {
                    emptyList()
                }

                for (stationName in favoriteStations) {
                    val textView = TextView(this)
                    textView.text = stationName
                    textView.textSize = 22f
                    textView.setTextColor(Color.BLACK)
                    textView.setTypeface(textView.typeface, Typeface.BOLD)
                    textView.setPadding(60, 60, 20, 5)
                    favoriteListLayout.addView(textView)

                    val iconListLayout = findViewById<LinearLayout>(R.id.iconListLayout)
                    val iconImageView = ImageView(this)

                    val iconResourceId = R.drawable.fav
                    iconImageView.setImageResource(iconResourceId)

                    val layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    layoutParams.topMargin = 25
                    iconImageView.layoutParams = layoutParams
                    iconImageView.scaleType = ImageView.ScaleType.CENTER_INSIDE

                    iconListLayout.addView(iconImageView)

                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load favorites: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

}
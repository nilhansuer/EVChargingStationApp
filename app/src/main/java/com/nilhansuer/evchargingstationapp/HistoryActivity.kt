package com.nilhansuer.evchargingstationapp

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HistoryActivity: AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        displayHistory()
        firebaseAuth = FirebaseAuth.getInstance()

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

    private fun displayHistory() {
        val userId = FirebaseAuth.getInstance().currentUser!!.uid
        val db = FirebaseFirestore.getInstance()
        val historyDocument = db.collection("stationHistory").document(userId)

        historyDocument.get().addOnSuccessListener { document ->
            val listHistoryLayout = findViewById<LinearLayout>(R.id.historyListLayout)
            listHistoryLayout.removeAllViews()

            if (document != null && document.exists()) {
                val historyMap = document.getData() ?: emptyMap()
                historyMap.forEach { (stationName, activities) ->
                    val textView = TextView(this)
                    val parsedHistory = parseHistoryString(activities.toString())

                    textView.text = parsedHistory
                    textView.textSize = 22f
                    textView.setTextColor(Color.BLACK)
                    textView.setTypeface(textView.typeface, Typeface.BOLD)
                    textView.setPadding(80, 80, 20, 5)
                    listHistoryLayout.addView(textView)
                }

            }
        }.addOnFailureListener { exception ->
            Log.e("FirebaseError", "Error fetching history: $exception")
        }
    }

    fun parseHistoryString(historyString: String): String {
        val stationActivities = historyString.replace("{", "")
            .replace("}", "")
            .split(", Station ")

        var i = 0
        var allHistory = ""
        val parsedEntries = stationActivities.map { entry ->
            val parts = entry.split("=", limit = 2)
            if (parts.size == 2) {
                val stationName = parts[0]
                val activities = parts[1]
                if(i >= 1){
                    allHistory = allHistory + "Station $stationName: $activities\n\n"
                } else {
                    allHistory = "$stationName: $activities\n\n"
                }
                i += 1

                val iconListLayout = findViewById<LinearLayout>(R.id.iconListLayout)
                val iconImageView = ImageView(this)

                val iconResourceId = R.drawable.ic_historylist
                iconImageView.setImageResource(iconResourceId)

                val layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                layoutParams.topMargin = 60
                iconImageView.layoutParams = layoutParams
                iconImageView.scaleType = ImageView.ScaleType.CENTER_INSIDE

                iconListLayout.addView(iconImageView)

            } else {
                "" // Handle invalid entries
            }
        }

        return allHistory
    }





}
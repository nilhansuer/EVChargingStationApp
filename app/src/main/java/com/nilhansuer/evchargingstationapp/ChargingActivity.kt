package com.nilhansuer.evchargingstationapp

import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import okhttp3.*

class ChargingActivity : AppCompatActivity() {

    private lateinit var activityArray: JSONArray
    private val allActivitiesArrray = StationActivities.allActivitiesArray
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()
    private var stationName: String? = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_charging)

        auth = FirebaseAuth.getInstance()

        val addFavButton = findViewById<ImageButton>(R.id.imageButtonAddFav)

        addFavButton.setOnClickListener {
            addFavorite()
        }

        val exitButton: ImageView = findViewById(R.id.buttonExit)
        exitButton.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        // Retrieve the station name from the intent extras
        val station = intent.getStringExtra("STATION_NAME")
        stationName = station

        // Set the station name to the TextView
        val stationNameTextView = findViewById<TextView>(R.id.textStation)
        stationNameTextView.text = station

        // Read the JSON file
        val jsonString = readJsonFile("Stations.json")

        try {
            // Parse the JSON
            val jsonObject = JSONObject(jsonString)
            val stationsArray = jsonObject.getJSONArray("stations")

            // Find the station object with the matching name
            for (i in 0 until stationsArray.length()) {
                val stationObject = stationsArray.getJSONObject(i)
                if (stationObject.getString("name") == stationName) {
                    // Retrieve the activity list for the station
                    activityArray = stationObject.getJSONArray("activity")
                    // Initialize LinearLayout to display activity list
                    val activityListLayout = findViewById<LinearLayout>(R.id.activityListLayout)

                    // Add TextView elements for each activity
                    for (j in 0 until activityArray.length()) {
                        val activity = activityArray.getString(j)
                        val textView = TextView(this)
                        textView.text = activity
                        textView.textSize = 20f // adjust text size as needed
                        textView.setTextColor(Color.BLACK)
                        textView.gravity = Gravity.CENTER
                        activityListLayout.addView(textView)
                    }

                    break // Stop loop since station is found
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        showPopup(activityArray)

        // Button click listener for Charging Done
        findViewById<Button>(R.id.buttonChargingDone).setOnClickListener {
            showActivitySelectionDialog()
        }
    }

    private fun addFavorite() {
        val userId = auth.currentUser?.uid ?: return // Exit if not logged in
        val stationName = stationName ?: return // Exit if station name not available

        val favStationsRef = db.collection("favoriteStations").document(userId)

        favStationsRef.get()
            .addOnSuccessListener { documentSnapshot ->
                val existingFavorites = if (documentSnapshot.exists()) {
                    // Safely get the list or initialize it if not present
                    (documentSnapshot.get("stations") as? List<String>)?.toMutableList()
                        ?: mutableListOf()
                } else {
                    mutableListOf()
                }

                if (!existingFavorites.contains(stationName)) { // Check if station is already a favorite
                    existingFavorites.add(stationName)
                    favStationsRef.set(hashMapOf("stations" to existingFavorites))
                        .addOnSuccessListener {
                            Toast.makeText(this, "$stationName added to favorites!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to add favorite: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "$stationName is already in favorites.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to check favorites: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }




    private fun showPopup(activityArray: JSONArray) { // Pass in the recommendations list

        // ... (Your existing dialog setup and close button code)
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.charging_popup_layout) // Your popup layout
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val userId = FirebaseAuth.getInstance().currentUser!!.uid
        // 1. Fetch User Recommendations from Firebase
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("recommendations").document(userId)

        userRef.get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val userActivities = document.getString("activity")

                // 2. Parse User Recommendations
                val userActivityList = parseUserActivities(userActivities)

                // 3. Match Recommendations with Station Activities (using activityArray)
                val matchedRecommendations = mutableListOf<String>()
                for (i in 0 until activityArray.length()) {
                    val stationActivity = activityArray.getString(i)
                    if (userActivityList.contains(stationActivity)) {
                        matchedRecommendations.add(stationActivity)
                    }
                }

                // 4. Populate listRecommendations LinearLayout
                val listRecommendationsLayout = dialog.findViewById<LinearLayout>(R.id.listRecommendations)
                listRecommendationsLayout.removeAllViews()
                for (recommendation in matchedRecommendations) {
                    val textView = TextView(this)
                    textView.text = recommendation
                    textView.textSize = 16f
                    textView.setPadding(0, 5, 0, 5)
                    listRecommendationsLayout.addView(textView)
                }
            } else {
                // Handle case where user document doesn't exist
            }
        }.addOnFailureListener { exception ->
            // Handle Firebase fetch error
        }

        val btnClosePopup = dialog.findViewById<Button>(R.id.buttonOK)
        btnClosePopup.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun parseUserActivities(activitiesString: String?): List<String> {
        if (activitiesString.isNullOrEmpty()) return emptyList()

        val activities = JSONArray(activitiesString)
        val result = mutableListOf<String>()
        for (i in 0 until activities.length()) {
            result.add(activities.getJSONArray(i).getString(0)) // Get the activity name (index 0)
        }
        return result
    }

    private fun showActivitySelectionDialog() {
        // Inflate the layout for the dialog
        val dialogView = LayoutInflater.from(this).inflate(R.layout.select_activity_layout, null)

        // Get the LinearLayout container for checkboxes
        val checkboxContainer = dialogView.findViewById<LinearLayout>(R.id.checkboxContainer)

        // Create and add checkboxes for each activity
        for (i in 0 until activityArray.length()) {
            val activity = activityArray.getString(i)
            val checkBox = CheckBox(this)
            checkBox.text = activity
            checkboxContainer.addView(checkBox)
        }

        // Set up AlertDialog.Builder with dialogView
        val dialogBuilder = AlertDialog.Builder(this, R.style.RoundedDialog)
            .setView(dialogView)
            .setTitle("Select Activities")

        // Set positive button click listener
        dialogBuilder.setPositiveButton("OK") { dialog, which ->
            // Handle OK button click
            val selectedActivities = mutableListOf<String>()

            // Iterate through all child views of the checkbox container
            for (i in 0 until checkboxContainer.childCount) {
                val view = checkboxContainer.getChildAt(i)
                if (view is CheckBox) {
                    // If the checkbox is checked, add its text to the list of selected activities
                    if (view.isChecked) {
                        selectedActivities.add(view.text.toString())
                    }
                }
            }

            // Do something with the selected activities
            val selectedActivitiesText = selectedActivities.joinToString(", ")
            Toast.makeText(this, "Selected activities: $selectedActivitiesText", Toast.LENGTH_SHORT).show()
            // Save selected activities to CSV
            saveToCsv(selectedActivities)

            val csvFileName = "preferenceDataset.csv"
            val lines = readCsvFile(this, csvFileName)
            var csvString = ""
            lines.forEach { line ->
                //Log.d("CSV Line", line)
                csvString += line
                csvString += '\n'

            }
            println(csvString)

            // Find the user's row in the CSV based on UID (if it exists)
            val currentUserUid = auth.currentUser?.uid
            val userRow = lines.find { it.startsWith("$currentUserUid,") }

            val allSelectedActivities = mutableListOf<String>()
            if (userRow != null) {
                // User row exists - get historical selections
                val activityValues = userRow.split(",").drop(1)  // Drop the UID
                for (i in activityValues.indices) {
                    if (activityValues[i] == "1.0") {
                        allSelectedActivities.add(allActivitiesArrray[i]) // Get the activity name from allActivitiesArray
                    }
                }
            }
            println("All selected activities: " + allSelectedActivities.toString())
            addStationHistory(stationName.toString(), selectedActivities)
            sendPostRequest(csvString , allSelectedActivities.toString())
        }

        // Set negative button click listener
        dialogBuilder.setNegativeButton("Cancel") { dialog, which ->
            // Handle Cancel button click
            dialog.dismiss()
        }

        // Create and show the AlertDialog
        val alertDialog = dialogBuilder.create()
        alertDialog.show()

    }

    fun addStationHistory(stationName: String, activities: List<String>) {
        val currentUserUid = auth.currentUser?.uid
        val userUid = currentUserUid.toString()

        val db = FirebaseFirestore.getInstance()
        val userDocRef = db.collection("stationHistory").document(userUid)

        userDocRef.get()
            .addOnSuccessListener { documentSnapshot ->
                val stationActivities = activities.joinToString(", ")
                if (documentSnapshot.exists()) {
                    // Document exists, update the 'history' field
                    val currentHistory = documentSnapshot.get("history") as? MutableMap<String, String> ?: mutableMapOf()
                    currentHistory[stationName] = stationActivities // Update or add the station entry
                    userDocRef.update("history", currentHistory)
                } else {
                    // Document doesn't exist, create it with initial data
                    val stationData = hashMapOf(
                        "history" to mapOf(stationName to stationActivities)
                    )
                    userDocRef.set(stationData)
                }
            }
            .addOnFailureListener { e ->
                println("Error checking or updating document: $e")
            }
    }



    fun sendPostRequest(csvData: String, currentData: String) {
        val url = "http://10.0.2.2:8000/receive_csv/"

        val client = OkHttpClient()

        val requestBody = FormBody.Builder()
            .add("csv_data", csvData)
            .add("current_data", currentData)
            .build()

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                // Handle successful response
                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    saveRecommendation(responseData)
                    println("Response: $responseData")
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                // Handle failure
                println("Failed to send POST request: ${e.message}")
            }
        })
    }

    private fun saveRecommendation(data: String?) {
        val userId = FirebaseAuth.getInstance().currentUser!!.uid

        // Update only the "activity" field
        db.collection("recommendations").document(userId)
            .update("activity", data)
            .addOnSuccessListener {
                Log.d(ContentValues.TAG, "Activity recommendation updated successfully")
            }
            .addOnFailureListener { e ->
                // If the document doesn't exist, create it with the new data
                if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.NOT_FOUND) {
                    val userMap = hashMapOf("activity" to data)
                    db.collection("recommendations").document(userId).set(userMap)
                        .addOnSuccessListener {
                            Log.d(ContentValues.TAG, "Activity recommendation created successfully")
                        }
                        .addOnFailureListener { e2 ->
                            Log.w(ContentValues.TAG, "Error creating activity recommendation", e2)
                        }
                } else {
                    Log.w(ContentValues.TAG, "Error updating activity recommendation", e)
                }
            }
    }

    private fun saveToCsv(selectedActivities: List<String>) {
        val csvFileName = "preferenceDataset.csv"
        val currentUserUid = auth.currentUser?.uid

        if (currentUserUid != null) {
            var existingCsvRows = mutableListOf<String>()
            var userRowExists = false
            var userRowIndex = -1

            // Read existing CSV, look for the user's row
            try {
                openFileInput(csvFileName).bufferedReader().useLines { lines ->
                    lines.forEachIndexed { index, line ->
                        existingCsvRows.add(line)
                        if (line.startsWith("$currentUserUid,")) {
                            userRowExists = true
                            userRowIndex = index
                        }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

            val newActivityValues = allActivitiesArrray.map { if (it in selectedActivities) 1.0 else "" }

            if (userRowExists) {
                // Update existing row
                val existingActivities = existingCsvRows[userRowIndex].split(",").drop(1) // Drop the UID
                val updatedActivities = existingActivities.zip(newActivityValues)
                    .map { (existing, new) -> if (new == 1.0) 1.0 else existing } // Keep existing "1" values
                existingCsvRows[userRowIndex] = "$currentUserUid," + updatedActivities.joinToString(",")
            } else {
                // Create new row
                val currentUserRow = "$currentUserUid," + newActivityValues.joinToString(",")
                existingCsvRows.add(currentUserRow)
            }


            // Write updated CSV
            try {
                openFileOutput(csvFileName, Context.MODE_PRIVATE).bufferedWriter().use { writer ->
                    existingCsvRows.forEach { row ->
                        writer.write(row)
                        writer.newLine()
                    }
                }
                Toast.makeText(this, "User activities saved to CSV", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to save user activities to CSV", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
        }
    }

    fun readCsvFile(context: Context, fileName: String): List<String> {
        val file = File(context.filesDir, fileName)
        val lines = mutableListOf<String>()

        try {
            file.bufferedReader().useLines { lines.addAll(it) }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return lines
    }





    private fun readJsonFile(filename: String): String {
        val stringBuilder = StringBuilder()
        val assetManager = assets
        val bufferedReader = BufferedReader(InputStreamReader(assetManager.open(filename)))
        var line: String? = bufferedReader.readLine()
        while (line != null) {
            stringBuilder.append(line)
            line = bufferedReader.readLine()
        }
        bufferedReader.close()
        return stringBuilder.toString()
    }

}

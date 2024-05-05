package com.nilhansuer.evchargingstationapp

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

class ChargingActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var station: Station
    private lateinit var activityArray: JSONArray

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_charging)

        // Retrieve the station name from the intent extras
        val stationName = intent.getStringExtra("STATION_NAME")

        // Set the station name to the TextView
        val stationNameTextView = findViewById<TextView>(R.id.textStation)
        stationNameTextView.text = stationName

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
                        textView.textSize = 30f // adjust text size as needed
                        activityListLayout.addView(textView)
                    }

                    break // Stop loop since station is found
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        // Button click listener for Charging Done
        findViewById<Button>(R.id.buttonChargingDone).setOnClickListener {
            showActivitySelectionDialog()
        }
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
        val dialogBuilder = AlertDialog.Builder(this)
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

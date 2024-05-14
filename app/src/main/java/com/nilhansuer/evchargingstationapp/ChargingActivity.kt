package com.nilhansuer.evchargingstationapp

import android.content.Context
import android.os.Bundle
import android.util.Log
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
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import okhttp3.*

class ChargingActivity : AppCompatActivity() {

    private lateinit var activityArray: JSONArray
    private val allActivitiesArrray = StationActivities.allActivitiesArray
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_charging)

        auth = FirebaseAuth.getInstance()

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
            // Save selected activities to CSV
            saveToCsv(selectedActivities)

            val csvFileName = "preferenceDataset.csv"
            val lines = readCsvFile(this, csvFileName)
            var csvString = ""
            lines.forEach { line ->
                Log.d("CSV Line", line)
                csvString += line
            }
            sendPostRequest(csvString)
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

    fun sendPostRequest(csvData: String) {
        val url = "http://localhost:8000/receive_csv/"

        val client = OkHttpClient()

        val requestBody = FormBody.Builder()
            .add("csv_data", csvData)
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
                    println("Response: $responseData")
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                // Handle failure
                println("Failed to send POST request: ${e.message}")
            }
        })
    }

    private fun saveToCsv(selectedActivities: List<String>) {
        val csvFileName = "preferenceDataset.csv"

        // Get the UID of the current user from Firebase Authentication
        val currentUser = auth.currentUser
        val currentUserUid = currentUser?.uid

        // Check if the user is authenticated
        if (currentUserUid != null) {

            // Read existing content of the CSV file
            val existingCsvRows = mutableListOf<String>()
            try {
                val inputStream = openFileInput(csvFileName)
                val reader = BufferedReader(InputStreamReader(inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    existingCsvRows.add(line!!)
                }
                reader.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            // Construct the row for the current user
            val currentUserRow = "$currentUserUid," +
                    allActivitiesArrray.joinToString(",") { if (it in selectedActivities) "1" else "" }
            existingCsvRows.add(currentUserRow)

            // Write the CSV row to internal storage
            try {
                val fileOutputStream = openFileOutput(csvFileName, Context.MODE_PRIVATE)
                val writer = BufferedWriter(OutputStreamWriter(fileOutputStream))
                for (row in existingCsvRows) {
                    writer.write(row)
                    writer.newLine()
                }
                writer.close()

                val filePath = File(filesDir, csvFileName).absolutePath
                Log.d("File Path", filePath)

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

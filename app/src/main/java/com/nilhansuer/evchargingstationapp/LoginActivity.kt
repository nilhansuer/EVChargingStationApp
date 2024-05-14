package com.nilhansuer.evchargingstationapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Call
import java.io.IOException

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val signupButton = findViewById<Button>(R.id.signupButton)

        signupButton.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
            finish()
        }

        auth = FirebaseAuth.getInstance()
        val loginButton = findViewById<Button>(R.id.registerButton)

        val testButton = findViewById<Button>(R.id.testButton)
        testButton.setOnClickListener {
            // sendPostRequest("data")
            sendGetRequest()
        }


        val emailText = findViewById<EditText>(R.id.emailText)
        val passwordText = findViewById<EditText>(R.id.passwordText)

        loginButton.setOnClickListener {

            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
            finish()

            val email = emailText.text.toString()
            val password = passwordText.text.toString()

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val intent = Intent(this, ProfileActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        val builder = AlertDialog.Builder(this)
                        val customView = LayoutInflater.from(this).inflate(R.layout.custom_layout_dialog, null)
                        builder.setView(customView)

                        val dialog = builder.create()
                        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                        val buttonOK = customView.findViewById<Button>(R.id.buttonOK)

                        buttonOK.setOnClickListener {
                            dialog.dismiss()
                        }
                        dialog.show()
                    }
                }
        }
    }

    fun sendGetRequest() {
        val url = "http://10.0.2.2:8000/"

        val client = OkHttpClient()

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                // Handle successful response
                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    println("Response: $responseData")
                }
                println("Response: $response")
            }

            override fun onFailure(call: okhttp3.Call, e: IOException) {
                // Handle failure
                println("Failed to send POST request: ${e.message}")
            }
        })
    }

    fun sendPostRequest(csvData: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8000/receive_csv/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)

        val call = apiService.sendCsvData(csvData)
        call.enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                if (response.isSuccessful) {
                    val responseData = response.body()
                    println("Response: $responseData")
                } else {
                    println("Failed to send POST request: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<String>, t: Throwable) {
                println("Failed to send POST request: ${t.message}")
            }
        })
    }
}
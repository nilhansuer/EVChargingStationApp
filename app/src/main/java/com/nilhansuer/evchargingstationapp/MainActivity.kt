package com.nilhansuer.evchargingstationapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.nio.charset.Charset

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private var mGoogleMap: GoogleMap? = null
    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    private var lat_search: Double = 0.0
    private var long_search: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val findStationButton = findViewById<ImageButton>(R.id.findStationButton)
        findStationButton.setOnClickListener {
            displayChargingStations()
        }
    }

    fun onClick(v: View) {

        when (v.id) {
            R.id.searchButton -> {
                mGoogleMap?.clear()

                val addressField = findViewById<View>(R.id.searchEditText) as EditText
                val address = addressField.text.toString()
                var addressList: List<Address>? = null
                val userMarkerOptions = MarkerOptions()

                if (!TextUtils.isEmpty(address)) {
                    val geocoder = Geocoder(this)

                    try {
                        addressList = geocoder.getFromLocationName(address, 6)

                        if (addressList != null) {
                            for (i in addressList.indices) {
                                val userAddress = addressList[i]
                                val latLng = LatLng(userAddress.latitude, userAddress.longitude)
                                println(latLng)

                                val pattern = Regex("""\((-?\d+\.\d+),(-?\d+\.\d+)\)""")

                                val matchResult = pattern.find(latLng.toString())

                                val (lat, lng) = matchResult?.destructured ?: throw IllegalArgumentException("Invalid input")

                                lat_search = lat.toDouble()
                                long_search = lng.toDouble()

                                userMarkerOptions.position(latLng)
                                userMarkerOptions.title(address)
                                userMarkerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                                mGoogleMap?.addMarker(userMarkerOptions)
                                mGoogleMap?.moveCamera(CameraUpdateFactory.newLatLng(latLng))
                                mGoogleMap?.animateCamera(CameraUpdateFactory.zoomTo(10f))
                            }
                        } else {
                            Toast.makeText(this, "Location not found...", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                } else {
                    Toast.makeText(this, "please write any location name...", Toast.LENGTH_SHORT).show()
                }
            }

        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mGoogleMap = googleMap
        mGoogleMap?.setOnMarkerClickListener { marker ->
            // Get the station name from the marker's title
            val stationName = marker.title

            // Create an intent to start ChargingActivity and pass the station name as an extra
            val intent = Intent(this, ChargingActivity::class.java).apply {
                putExtra("STATION_NAME", stationName)
            }
            startActivity(intent)
            true // return true to indicate that the click event has been consumed
        }
        enableMyLocation()
    }



    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            mGoogleMap?.isMyLocationEnabled = true
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation()
            }
        }
    }

    private fun displayChargingStations() {
        println("Latitude: $lat_search")
        println("Longitude: $long_search")
        try {
            val inputStream = assets.open("Stations.json")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            val json = String(buffer, Charset.forName("UTF-8"))
            val jsonObject = JSONObject(json)
            val stationsArray = jsonObject.getJSONArray("stations")

            for (i in 0 until stationsArray.length()) {
                val stationObject = stationsArray.getJSONObject(i)
                val name = stationObject.getString("name")
                val locationObject = stationObject.getJSONObject("location")
                val latitude = locationObject.getDouble("latitude")
                val longitude = locationObject.getDouble("longitude")

                if(((lat_search <= latitude + 0.02) && (lat_search >= latitude - 0.02)) && ((long_search <= longitude + 0.02) && (long_search >= longitude - 0.02))){
                    addMarkerToMap(name, latitude, longitude)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun addMarkerToMap(name: String, latitude: Double, longitude: Double) {
        val markerIcon = BitmapFactory.decodeResource(resources, R.drawable.ic_station_location)
        val resizedMarkerIcon = Bitmap.createScaledBitmap(markerIcon, 200, 200, false)
        val markerOptions = MarkerOptions().position(LatLng(latitude, longitude))
            .icon(BitmapDescriptorFactory.fromBitmap(resizedMarkerIcon))
            .title(name)
        mGoogleMap?.addMarker(markerOptions)
    }
}

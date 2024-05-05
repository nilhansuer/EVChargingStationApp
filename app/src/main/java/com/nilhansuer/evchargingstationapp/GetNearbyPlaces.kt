package com.nilhansuer.evchargingstationapp

import android.os.AsyncTask
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.io.IOException

class GetNearbyPlaces : AsyncTask<Any, String, String>() {
    private var googlePlaceData: String? = null
    private var url: String? = null
    private lateinit var mMap: GoogleMap

    override fun doInBackground(vararg objects: Any): String? {
        mMap = objects[0] as GoogleMap
        url = objects[1] as String

        val downloadUrl = DownloadUrl()
        try {
            googlePlaceData = downloadUrl.ReadTheURL(url!!)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return googlePlaceData
    }

    override fun onPostExecute(s: String?) {
        var nearByPlacesList: List<HashMap<String, String>>? = null
        val dataParser = DataParser()
        nearByPlacesList = dataParser.parse(s)

        DisplayNearbyPlaces(nearByPlacesList)
    }

    private fun DisplayNearbyPlaces(nearByPlacesList: List<HashMap<String, String>>?) {
        for (i in nearByPlacesList!!.indices) {
            val markerOptions = MarkerOptions()

            val googleNearbyPlace = nearByPlacesList[i]
            val nameOfPlace = googleNearbyPlace["place_name"]
            val vicinity = googleNearbyPlace["vicinity"]
            val lat = googleNearbyPlace["lat"]!!.toDouble()
            val lng = googleNearbyPlace["lng"]!!.toDouble()

            val latLng = LatLng(lat, lng)
            markerOptions.position(latLng)
            markerOptions.title("$nameOfPlace : $vicinity")
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
            mMap.addMarker(markerOptions)
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
            mMap.animateCamera(CameraUpdateFactory.zoomTo(10f))
        }
    }
}

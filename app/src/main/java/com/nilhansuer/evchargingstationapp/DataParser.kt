package com.nilhansuer.evchargingstationapp

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class DataParser {
    private fun getSingleNearbyPlace(googlePlaceJSON: JSONObject): HashMap<String, String> {
        val googlePlaceMap = HashMap<String, String>()
        var nameOfPlace = "-NA-"
        var vicinity = "-NA-"
        var latitude = ""
        var longitude = ""
        var reference = ""

        try {
            if (!googlePlaceJSON.isNull("name")) {
                nameOfPlace = googlePlaceJSON.getString("name")
            }
            if (!googlePlaceJSON.isNull("vicinity")) {
                vicinity = googlePlaceJSON.getString("vicinity")
            }
            latitude = googlePlaceJSON.getJSONObject("geometry").getJSONObject("location").getString("lat")
            longitude = googlePlaceJSON.getJSONObject("geometry").getJSONObject("location").getString("lng")
            reference = googlePlaceJSON.getString("reference")

            googlePlaceMap["place_name"] = nameOfPlace
            googlePlaceMap["vicinity"] = vicinity
            googlePlaceMap["lat"] = latitude
            googlePlaceMap["lng"] = longitude
            googlePlaceMap["reference"] = reference
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return googlePlaceMap
    }

    private fun getAllNearbyPlaces(jsonArray: JSONArray): List<HashMap<String, String>> {
        val counter = jsonArray.length()
        val nearbyPlacesList = ArrayList<HashMap<String, String>>()

        var nearbyPlaceMap: HashMap<String, String>?

        for (i in 0 until counter) {
            try {
                nearbyPlaceMap = getSingleNearbyPlace(jsonArray[i] as JSONObject)
                nearbyPlacesList.add(nearbyPlaceMap)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        return nearbyPlacesList
    }

    fun parse(jSONdata: String?): List<HashMap<String, String>> {
        var jsonArray: JSONArray? = null
        var jsonObject: JSONObject?

        try {
            jsonObject = JSONObject(jSONdata)
            jsonArray = jsonObject.getJSONArray("results")
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return getAllNearbyPlaces(jsonArray!!)
    }
}

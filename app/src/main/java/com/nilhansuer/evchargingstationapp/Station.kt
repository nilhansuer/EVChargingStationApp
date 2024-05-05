package com.nilhansuer.evchargingstationapp

import android.location.Location

data class Station(
    val id: Int,
    val name: String,
    val location: Location,
    val activity: List<String>
)

data class Location(
    val latitude: Double,
    val longitude: Double
)

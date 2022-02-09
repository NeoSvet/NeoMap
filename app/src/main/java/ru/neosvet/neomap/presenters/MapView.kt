package ru.neosvet.neomap.presenters

import com.google.android.gms.maps.model.LatLng

interface MapView {
    fun getMyLocation(): LatLng?
    fun post(function: () -> Unit): Boolean?
    fun showMessage(resource: Int)
    fun showStatus(text: String)
}
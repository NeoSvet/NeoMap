package ru.neosvet.neomap.presenters

import androidx.annotation.StringRes
import com.google.android.gms.maps.model.LatLng

interface MapView {
    fun getMyLocation(): LatLng?
    fun post(function: () -> Unit): Boolean?
    fun showMessage(@StringRes resource: Int)
    fun showStatus(text: String)
}
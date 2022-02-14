package ru.neosvet.neomap.presenters

import androidx.annotation.StringRes
import ru.neosvet.neomap.data.NeoMarker

interface MarkersView {
    fun post(function: () -> Unit): Boolean?
    fun updateList(list: List<NeoMarker>)
    fun showMessage(@StringRes resource: Int)
    fun sendText(text: String)
}
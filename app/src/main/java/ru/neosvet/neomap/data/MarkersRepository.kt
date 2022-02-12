package ru.neosvet.neomap.data

interface MarkersRepository {
    fun addMarker(marker: NeoMarker)
    fun updateMarker(oldName: String, marker: NeoMarker)
    fun containsMarker(name: String): Boolean
    fun deleteMarker(name: String)
    fun getListMarkers() : List<NeoMarker>
}
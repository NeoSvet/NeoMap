package ru.neosvet.neomap.data

interface MarkersRepository {
    val isChanged: Boolean
    fun fixChanges()
    fun addMarker(marker: NeoMarker)
    fun updateMarker(oldName: String, marker: NeoMarker)
    fun containsMarker(name: String): Boolean
    fun deleteMarker(name: String)
    fun getListMarkers() : List<NeoMarker>
}
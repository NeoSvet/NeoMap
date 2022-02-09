package ru.neosvet.neomap.presenters

import android.content.ContentValues
import android.location.Address
import android.location.Geocoder
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import ru.neosvet.neomap.DataBase
import ru.neosvet.neomap.R
import java.io.IOException

class MapPresenter(
    private val view: MapView,
    private val db: DataBase,
    private val formatLocation: String
) {
    companion object {
        private const val ZOOM_LARGE = 14f
        private const val ZOOM_FINELY = 9f
    }

    var containsResult = false
        private set
    val countMarkers: Int
        get() = markers.size
    private val markers = HashMap<String, LatLng>()
    lateinit var map: GoogleMap

    fun clearResult() {
        containsResult = false
        map.clear()
        for (marker in markers) {
            val loc = LatLng(marker.value.latitude, marker.value.longitude)
            markers[marker.key] = loc
            addMarker(loc, marker.key, false)
        }
    }

    fun showMyLocation(): Boolean {
        view.getMyLocation()?.let {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(it, ZOOM_LARGE))
            return true
        }
        return false
    }

    fun setNormalMap() {
        map.mapType = GoogleMap.MAP_TYPE_NORMAL
    }

    fun setSatelliteMap() {
        map.mapType = GoogleMap.MAP_TYPE_SATELLITE
    }

    fun setHybridMap() {
        map.mapType = GoogleMap.MAP_TYPE_HYBRID
    }

    fun switchTraffic() {
        map.isTrafficEnabled = !map.isTrafficEnabled
    }

    fun loadMarkers() {
        val sq = db.readableDatabase
        val cursor = sq.query(DataBase.TABLE, null, null, null, null, null, null)
        if (cursor.moveToFirst()) {
            val iName: Int = cursor.getColumnIndex(DataBase.NAME)
            val iLat: Int = cursor.getColumnIndex(DataBase.LAT)
            val iLng: Int = cursor.getColumnIndex(DataBase.LNG)
            do {
                val loc = LatLng(cursor.getDouble(iLat), cursor.getDouble(iLng))
                markers[cursor.getString(iName)] = loc
                addMarker(loc, cursor.getString(iName), false)
            } while (cursor.moveToNext())
        }
        cursor.close()
        sq.close()
    }

    private fun addMarker(loc: LatLng, name: String, isSearch: Boolean) {
        val m = MarkerOptions()
        m.position(loc)
        m.title(name)
        m.snippet(locToString(loc))
        if (isSearch) {
            m.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            map.addMarker(m)?.tag = "s" //tag for search marker
        } else map.addMarker(m)
    }

    private fun locToString(loc: LatLng) =
        String.format(formatLocation, loc.latitude, loc.longitude)

    fun createMarker(name: String) {
        if (markers.containsKey(name)) {
            view.showMessage(R.string.name_exist)
        } else {
            val loc: LatLng = map.cameraPosition.target
            markers[name] = loc
            addMarker(loc, name, false)
        }
    }

    fun showPlace(place: String, geocoder: Geocoder) {
        Thread {
            try {
                val addresses = geocoder.getFromLocationName(place, 1)
                if (addresses.size == 0)
                    return@Thread

                val location = LatLng(
                    addresses[0].latitude,
                    addresses[0].longitude
                )
                view.post {
                    map.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(location, ZOOM_FINELY)
                    )
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.start()
    }

    fun save() {
        val sq = db.writableDatabase
        for (marker in markers) {
            val cv = ContentValues()
            cv.put(DataBase.NAME, marker.key)
            cv.put(DataBase.LAT, marker.value.latitude)
            cv.put(DataBase.LNG, marker.value.longitude)
            val r = sq.update(DataBase.TABLE, cv, DataBase.NAME + " = ?", arrayOf(marker.key))
            if (r == 0) // no update
                sq.insert(DataBase.TABLE, null, cv)
        }
        sq.close()
    }

    fun deleteMarker(marker: Marker) {
        val sq = db.writableDatabase
        sq.delete(DataBase.TABLE, DataBase.NAME + " = ?", arrayOf(marker.title))
        sq.close()
        markers.remove(marker.title)
    }

    fun search(request: String, geocoder: Geocoder) {
        //TODO fix search: why always only one result?
        val target = map.cameraPosition.target
        Thread {
            try {
                val lat1 = target.latitude - 0.2
                val lat2 = target.latitude + 0.2
                val lng1 = target.longitude - 0.2
                val lng2 = target.longitude + 0.2
                val list = geocoder.getFromLocationName(request, 10, lat1, lng1, lat2, lng2)
                view.post {
                    parseSearchResult(list)
                }
            } catch (e: IOException) {
                e.printStackTrace()
                view.post {
                    view.showStatus("Error: " + e.localizedMessage)
                }
            }
        }.start()
    }

    private fun parseSearchResult(list: List<Address>) {
        view.showStatus("")
        if (list.isEmpty()) {
            containsResult = false
            view.showMessage(R.string.no_results)
            return
        }
        for (place in list) {
            val loc = LatLng(place.latitude, place.longitude)
            addMarker(loc, place.featureName, true)
        }
        containsResult = true
    }

    fun addLine(lineOpt: PolylineOptions) = map.addPolyline(lineOpt)
}
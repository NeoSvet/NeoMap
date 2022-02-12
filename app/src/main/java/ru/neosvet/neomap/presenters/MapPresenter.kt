package ru.neosvet.neomap.presenters

import android.location.Address
import android.location.Geocoder
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.*
import ru.neosvet.neomap.R
import ru.neosvet.neomap.data.MarkersRepository
import ru.neosvet.neomap.data.NeoMarker

class MapPresenter(
    private val view: MapView,
    private val repository: MarkersRepository,
    private val formatLocation: String
) {
    companion object {
        private const val TAG_SEARCH = "search"
        private const val ZOOM_LARGE = 14f
        private const val ZOOM_FINELY = 9f
        private const val MIN_REQUEST_LEN = 4
        private val streamMarkers: MutableLiveData<NeoMarker> = MutableLiveData()

        fun showMarker(marker: NeoMarker) {
            streamMarkers.value = marker
        }
    }

    var containsResult = false
        private set
    var countMarkers: Int = 0
        private set
    private lateinit var map: GoogleMap
    private var showMarker = false
    private val markers = arrayListOf<Marker>()

    private val scope = CoroutineScope(
        Dispatchers.IO
                + SupervisorJob()
                + CoroutineExceptionHandler { _, throwable ->
            handleError(throwable)
        })

    private fun handleError(error: Throwable) {
        error.printStackTrace()
        view.post {
            view.showStatus(error.localizedMessage ?: "Error")
        }
    }

    fun init(map: GoogleMap) {
        this.map = map
        streamMarkers.observeForever(markersObserver)
    }

    fun onDestroy() {
        streamMarkers.removeObserver(markersObserver)
        scope.cancel()
    }

    private val markersObserver = Observer<NeoMarker> {
        showMarker = true
        val loc = LatLng(it.lat, it.lng)
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, ZOOM_LARGE))
    }

    fun clearResult() {
        if (containsResult.not())
            return
        containsResult = false
        var i = 0
        while (i < markers.size) {
            if (markers[i].tag == TAG_SEARCH) {
                markers[i].remove()
                markers.removeAt(i)
            } else i++
        }
    }

    fun showMyLocation(): Boolean {
        if (showMarker) {
            showMarker = false
            return true
        }
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
        countMarkers = 0
        for (marker in repository.getListMarkers()) {
            val loc = LatLng(marker.lat, marker.lng)
            addMarker(loc, marker.name, false)
            countMarkers++
        }
    }

    private fun addMarker(loc: LatLng, name: String, isSearch: Boolean) {
        val m = MarkerOptions()
        m.position(loc)
        m.title(name)
        m.snippet(locToString(loc))
        val marker: Marker?
        if (isSearch) {
            m.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            marker = map.addMarker(m)
            marker?.tag = TAG_SEARCH
        } else
            marker = map.addMarker(m)
        marker?.let {
            markers.add(it)
        }
    }

    private fun locToString(loc: LatLng) =
        String.format(formatLocation, loc.latitude, loc.longitude)

    fun createMarker(name: String) {
        if (repository.containsMarker(name)) {
            view.showMessage(R.string.name_exist)
        } else {
            val loc: LatLng = map.cameraPosition.target
            repository.addMarker(
                NeoMarker(
                    name = name,
                    lat = loc.latitude,
                    lng = loc.longitude
                )
            )
            addMarker(loc, name, false)
        }
    }

    fun showPlace(place: String, geocoder: Geocoder) {
        if (showMarker) {
            showMarker = false
            return
        }
        scope.launch {
            val addresses = geocoder.getFromLocationName(place, 1)
            if (addresses.size == 0)
                return@launch

            val location = LatLng(
                addresses[0].latitude,
                addresses[0].longitude
            )
            view.post {
                map.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(location, ZOOM_FINELY)
                )
            }
        }
    }

    fun deleteMarker(marker: Marker) {
        marker.title?.let {
            repository.deleteMarker(it)
        }
    }

    fun search(request: String, geocoder: Geocoder) {
        if (request.length < MIN_REQUEST_LEN) {
            view.showMessage(R.string.error_short_request)
            return
        }
        //TODO fix search: why always only one result?
        val target = map.cameraPosition.target
        scope.launch {
            val lat1 = target.latitude - 0.2
            val lat2 = target.latitude + 0.2
            val lng1 = target.longitude - 0.2
            val lng2 = target.longitude + 0.2
            val list = geocoder.getFromLocationName(request, 10, lat1, lng1, lat2, lng2)
            view.post {
                parseSearchResult(list)
            }
        }
    }

    private fun parseSearchResult(list: List<Address>) {
        view.showStatus("")
        if (list.isEmpty()) {
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
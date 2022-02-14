package ru.neosvet.neomap.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context.LOCATION_SERVICE
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.SphericalUtil
import ru.neosvet.neomap.App
import ru.neosvet.neomap.BackEvent
import ru.neosvet.neomap.R
import ru.neosvet.neomap.databinding.FragmentMapBinding
import ru.neosvet.neomap.presenters.MapPresenter
import ru.neosvet.neomap.presenters.MapView

class MapFragment : Fragment(), MapView, BackEvent {
    private var binding: FragmentMapBinding? = null
    private val presenter: MapPresenter by lazy {
        MapPresenter(
            view = this,
            repository = App.repository,
            formatLocation = getString(R.string.format_location),
            searchTip = getString(R.string.search_tip)
        )
    }
    private var locManager: LocationManager? = null
    private var selectedMaker: Marker? = null
    private var boolMe = false
    private var distance = 0.0
    private val line = ArrayList<Polyline>()
    private lateinit var lastSearch: String
    private lateinit var searchView: SearchView

    //TODO return speedometer?

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = FragmentMapBinding.inflate(inflater, container, false).also {
        setHasOptionsMenu(true)
        binding = it
    }.root

    override fun onDestroyView() {
        presenter.onDestroy()
        binding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().setTitle(R.string.app_name)
        lastSearch = getString(R.string.default_search)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(onMapReady)
        initButtons()
        if (permissionAccessed()) {
            locManager = requireContext().getSystemService(LOCATION_SERVICE) as LocationManager
        }
    }

    private val onMapReady = OnMapReadyCallback { googleMap ->
        initMap(googleMap)
        if (!permissionAccessed() || !presenter.showMyLocation()) {
            presenter.showPlace(getString(R.string.default_location), Geocoder(requireContext()))
        }
    }

    private fun permissionAccessed(): Boolean {
        return ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onBack(): Boolean {
        binding?.run {
            return when {
                searchView.isIconified.not() -> {
                    searchView.onActionViewCollapsed()
                    true
                }
                fabOk.visibility == View.VISIBLE -> {
                    fabMarker.visibility = View.VISIBLE
                    fabOk.visibility = View.GONE
                    ivPointer.visibility = View.GONE
                    true
                }
                fabDelete.visibility == View.VISIBLE -> {
                    clearSelect()
                    true
                }
                presenter.containsResult -> {
                    presenter.clearResult()
                    true
                }
                else -> false
            }
        }
        return false
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_menu, menu)
        menu.getItem(1).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.getItem(2).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

        searchView = menu.findItem(R.id.menu_search).actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                clearSelect()
                presenter.clearResult()
                if (query.isEmpty())
                    return false
                searchView.onActionViewCollapsed()
                lastSearch = query
                showStatus(getString(R.string.search))
                presenter.search(query, Geocoder(requireContext()))
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                return true
            }
        })
        searchView.setQuery(lastSearch, false)

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_map_mode_normal ->
                presenter.setNormalMap()
            R.id.menu_map_mode_satellite ->
                presenter.setSatelliteMap()
            R.id.menu_map_mode_hybrid ->
                presenter.setHybridMap()
            R.id.menu_map_traffic ->
                presenter.switchTraffic()
            else ->
                return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun initMap(map: GoogleMap) {
        map.uiSettings.isMyLocationButtonEnabled = false
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isRotateGesturesEnabled = false
        map.setOnMapClickListener {
            if (selectedMaker != null)
                clearSelect()
        }
        map.setOnPolylineClickListener { polyline ->
            distance -= SphericalUtil.computeDistanceBetween(polyline.points[0], polyline.points[1])
            if (line[0] == polyline) boolMe = false
            line.remove(polyline)
            polyline.remove()
            if (distance <= 0.0)
                clearDistance()
            else
                changeDistance()
        }
        map.setOnMarkerClickListener { marker ->
            if (binding?.fabOk?.visibility == View.GONE)
                clickOnMarker(marker)
            false
        }
        presenter.init(map)
    }

    private fun clearSelect() {
        clearDistance()
        selectedMaker?.hideInfoWindow()
        selectedMaker = null
        binding?.run {
            fabDelete.visibility = View.GONE
            fabMarker.visibility = View.VISIBLE
        }
    }

    private fun clickOnMarker(marker: Marker) = binding?.run {
        boolMe = false
        val loc: LatLng? =
            if (selectedMaker == null) {
                boolMe = true
                fabMarker.visibility = View.INVISIBLE
                fabDelete.visibility = View.VISIBLE
                getMyLocation()
            } else
                selectedMaker?.position

        if (loc != null) {
            val lineOpt = PolylineOptions()
            lineOpt.add(loc).add(marker.position)
            lineOpt.color(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
            lineOpt.clickable(true)
            line.add(presenter.addLine(lineOpt))
            distance += SphericalUtil.computeDistanceBetween(loc, marker.position)
            tvInfo.visibility = View.VISIBLE
            changeDistance()
        }
        selectedMaker = marker
    }

    private fun initButtons() = binding?.run {
        fabMarker.setOnClickListener {
            clearDistance()
            fabMarker.visibility = View.INVISIBLE
            fabOk.visibility = View.VISIBLE
            ivPointer.visibility = View.VISIBLE
        }
        fabMarker.setOnLongClickListener {
            showStatus("")
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, MarkersFragment())
                .addToBackStack(MainActivity.MAIN_STACK).commit()
            false
        }
        fabOk.setOnClickListener {
            clearDistance()
            fabMarker.visibility = View.VISIBLE
            fabOk.visibility = View.GONE
            ivPointer.visibility = View.GONE
            pasteMarker()
        }
        fabDelete.setOnClickListener {
            selectedMaker?.let {
                clearSelect()
                presenter.deleteMarker(it)
                it.remove()
            }
        }
        fabPosition.setOnClickListener {
            presenter.showMyLocation()
        }
    }

    private fun clearDistance() {
        boolMe = false
        while (line.size > 0) {
            line[0].remove()
            line.removeAt(0)
        }
        distance = 0.0
        binding?.tvInfo?.visibility = View.GONE
    }

    private fun changeDistance() = binding?.run {
        tvInfo.text = String.format(getString(R.string.format_distance), distance)
    }

    private fun pasteMarker() {
        App.dialog.show(null, presenter::createMarker)
    }

    @SuppressLint("MissingPermission")
    override fun getMyLocation(): LatLng? = locManager?.let {
        var loc = it.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (loc == null) {
            loc = it.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (loc == null)
                loc = it.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
        }
        if (loc != null)
            return LatLng(loc.latitude, loc.longitude)
        return null
    }

    override fun post(function: () -> Unit) = view?.post(function)

    override fun showMessage(resource: Int) {
        showStatus("")
        Toast.makeText(
            requireContext(),
            getString(resource),
            Toast.LENGTH_LONG
        ).show()
    }

    override fun showStatus(text: String) {
        if (activity == null)
            return
        val act = activity as AppCompatActivity
        act.supportActionBar?.subtitle = text
    }
}
package ru.neosvet.neomap.view

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import ru.neosvet.neomap.BackEvent
import ru.neosvet.neomap.R
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
    companion object {
        const val MAIN_STACK = "main"
        const val TAG_MAP = "map"
        private const val MY_PERMISSIONS_REQUEST_LOCATION = 99
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkLocationPermission()
        openMap()
    }

    override fun onBackPressed() {
        supportFragmentManager.findFragmentByTag(TAG_MAP)?.let {
            if (it is BackEvent && it.onBack())
                return
        }
        super.onBackPressed()
        if (supportFragmentManager.findFragmentByTag(TAG_MAP) == null)
            exitProcess(0)
    }

    private fun openMap() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, MapFragment(), TAG_MAP)
            .addToBackStack(MAIN_STACK).commit()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    if (ContextCompat.checkSelfPermission(this,
//                                    Manifest.permission.ACCESS_FINE_LOCATION)
//                            == PackageManager.PERMISSION_GRANTED
//                    ) {
                    //permission accessed
                } else {
                    //permission denied
                }
            }
        }
    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) return

        // Should we show an explanation?
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            // Show an explanation to the user *asynchronously* -- don't block
            // this thread waiting for the user's response! After the user
            // sees the explanation, try again to request the permission.
            AlertDialog.Builder(this)
                .setTitle("Location Permission Needed")
                .setMessage("This app needs the Location permission, please accept to use location functionality")
                .setPositiveButton(
                    "OK"
                ) { _, _ ->
                    //Prompt the user once explanation has been shown
                    requestLocationPermission()
                }
                .create()
                .show()
        } else {
            // No explanation needed, we can request the permission.
            requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            MY_PERMISSIONS_REQUEST_LOCATION
        )
    }
}
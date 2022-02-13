package ru.neosvet.neomap.view

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import ru.neosvet.neomap.App
import ru.neosvet.neomap.BackEvent
import ru.neosvet.neomap.R
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
    companion object {
        const val MAIN_STACK = "main"
        const val TAG_MAP = "map"
        private const val MY_PERMISSIONS_REQUEST = 99
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initDialog()
        if (savedInstanceState != null)
            return
        checkPermission()
        openMap()
    }

    private fun initDialog() {
        val bottomSheet: BottomSheetBehavior<FrameLayout> =
            BottomSheetBehavior.from(findViewById(R.id.bottom_container))
        App.dialog = MarkerDialog(bottomSheet)
        supportFragmentManager.beginTransaction()
            .replace(R.id.bottom_container, App.dialog).commit()
        bottomSheet.state = BottomSheetBehavior.STATE_HIDDEN
    }

    override fun onBackPressed() {
        if (App.dialog.isHide.not()) {
            App.dialog.hide()
            return
        }
        supportFragmentManager.findFragmentByTag(TAG_MAP)?.let {
            if (it is BackEvent && it.onBack())
                return
        }
        super.onBackPressed()
        if (supportFragmentManager.fragments.size == 1) //only dialog
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
            MY_PERMISSIONS_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //permission accessed
                } else {
                    //permission denied
                }
            }
        }
    }

    private fun checkPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) return

        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) || ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        ) {
            showAboutPermission()
        } else {
            requestPermission()
        }
    }

    private fun showAboutPermission() {
        AlertDialog.Builder(this)
            .setTitle(R.string.about_permission_title)
            .setMessage(R.string.about_permission_description)
            .setPositiveButton(
                android.R.string.ok
            ) { _, _ ->
                requestPermission()
            }
            .create()
            .show()
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            MY_PERMISSIONS_REQUEST
        )
    }
}
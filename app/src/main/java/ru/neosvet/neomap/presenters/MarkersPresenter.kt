package ru.neosvet.neomap.presenters

import android.os.Environment
import kotlinx.coroutines.*
import ru.neosvet.neomap.R
import ru.neosvet.neomap.data.MarkersRepository
import ru.neosvet.neomap.data.NeoMarker
import java.io.*

class MarkersPresenter(
    private val view: MarkersView,
    private val repository: MarkersRepository
) {
    private val file: File by lazy {
        File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .absolutePath + "/neo_markers.txt"
        )
    }

    private val scope = CoroutineScope(
        Dispatchers.IO
                + SupervisorJob()
                + CoroutineExceptionHandler { _, throwable ->
            handleError(throwable)
        })

    private fun handleError(error: Throwable) {
        error.printStackTrace()
        view.post {
            view.showMessage(R.string.error_storage)
        }
    }

    fun loadList() {
        scope.launch {
            val markers = repository.getListMarkers()
            view.post {
                view.updateList(markers)
            }
        }
    }

    fun exportMarkers() {
        scope.launch {
            val bw = BufferedWriter(FileWriter(file))
            for (marker in repository.getListMarkers()) {
                bw.write(marker.toLine())
                bw.flush()
            }
            bw.close()
            view.post {
                view.showMessage(R.string.export_done)
            }
        }
    }

    fun importMarkers() {
        if (!file.exists()) return
        scope.launch {
            val list = arrayListOf<NeoMarker>()
            val br = BufferedReader(FileReader(file))
            var s = br.readLine()
            while (s != null) {
                val marker = NeoMarker(s)
                list.add(marker)
                repository.addMarker(marker)
                s = br.readLine()
            }
            br.close()
            view.post {
                view.updateList(list)
                view.showMessage(R.string.ready)
            }
        }
    }

    fun onDestroy() {
        scope.cancel()
    }
}
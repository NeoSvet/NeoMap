package ru.neosvet.neomap.presenters

import kotlinx.coroutines.*
import ru.neosvet.neomap.R
import ru.neosvet.neomap.data.MarkersRepository
import ru.neosvet.neomap.data.NeoMarker
import java.io.InputStream
import java.io.OutputStream

class MarkersPresenter(
    private val view: MarkersView,
    private val repository: MarkersRepository
) {
    private val scope = CoroutineScope(
        Dispatchers.IO
                + SupervisorJob()
                + CoroutineExceptionHandler { _, throwable ->
            handleError(throwable)
        })

    private fun handleError(error: Throwable) {
        error.printStackTrace()
        view.post {
            view.showMessage(R.string.error)
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

    fun exportMarkers(stream: OutputStream) {
        scope.launch {
            val bw = stream.bufferedWriter()
            for (marker in repository.getListMarkers()) {
                bw.write(marker.toLine())
                bw.flush()
            }
            bw.close()
            view.post {
                view.showMessage(R.string.ready)
            }
        }
    }

    fun importMarkers(stream: InputStream) {
        scope.launch {
            val list = arrayListOf<NeoMarker>()
            val br = stream.bufferedReader()
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

    fun deleteMarker(marker: NeoMarker) {
        scope.launch {
            repository.deleteMarker(marker.name)
        }
    }

    fun editMarker(oldName: String, marker: NeoMarker) {
        scope.launch {
            repository.updateMarker(oldName, marker)
        }
    }

    fun onDestroy() {
        scope.cancel()
    }
}
package ru.neosvet.neomap.presenters

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.neosvet.neomap.R
import ru.neosvet.neomap.data.MarkersRepository

class DialogPresenter(
    private val view: DialogView,
    private val repository: MarkersRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun requestName(context: Context) {
        scope.launch {
            val count = repository.getCountMarkers()
            val name = String.format(context.getString(R.string.format_marker), count + 1)
            view.postName(name)
        }
    }

    fun checkName(oldName: String?, newName: String) {
        if(oldName == newName) {
            view.postResult(false)
            return
        }
        scope.launch {
            val result = repository.containsMarker(newName)
            view.postResult(result)
        }
    }
}
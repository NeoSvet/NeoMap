package ru.neosvet.neomap.presenters

import android.content.ContentValues
import android.os.Environment
import kotlinx.coroutines.*
import ru.neosvet.neomap.DataBase
import ru.neosvet.neomap.R
import java.io.*

class MarkersPresenter(
    private val view: MarkersView,
    private val db: DataBase
) {
    private val markers = ArrayList<String>()
    private val mLat = ArrayList<Double>()
    private val mLng = ArrayList<Double>()
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
            markers.clear()
            mLat.clear()
            mLng.clear()
            val sq = db.writableDatabase
            val cursor = sq.query(DataBase.TABLE, null, null, null, null, null, null)
            if (cursor.moveToFirst()) {
                val iName = cursor.getColumnIndex(DataBase.NAME)
                val iLat = cursor.getColumnIndex(DataBase.LAT)
                val iLng = cursor.getColumnIndex(DataBase.LNG)
                do {
                    markers.add(cursor.getString(iName))
                    mLat.add(cursor.getDouble(iLat))
                    mLng.add(cursor.getDouble(iLng))
                } while (cursor.moveToNext())
            }
            cursor.close()
            sq.close()
            view.post {
                view.updateList(markers)
            }
        }
    }

    fun exportMarkers() {
        scope.launch {
            val bw = BufferedWriter(FileWriter(file))
            for (i in markers.indices) {
                bw.write("[")
                bw.write(mLng[i].toString())
                bw.write(",")
                bw.write(mLat[i].toString())
                bw.write(",\"")
                bw.write(markers[i])
                bw.write("\",\"\"]")
                bw.newLine()
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
            val br = BufferedReader(FileReader(file))
            val sq = db.writableDatabase
            var i: Int
            var s = br.readLine()
            while (s != null) {
                i = s.indexOf(",")
                val cv = ContentValues()
                cv.put(DataBase.LNG, s.substring(1, i).toDouble())
                i++
                cv.put(DataBase.LAT, s.substring(i, s.indexOf(",", i)).toDouble())
                i = s.indexOf("\"") + 1
                s = s.substring(i, s.indexOf("\"", i))
                cv.put(DataBase.NAME, s)
                val r = sq.update(DataBase.TABLE, cv, DataBase.NAME + " = ?", arrayOf(s))
                if (r == 0) // no update
                    sq.insert(DataBase.TABLE, null, cv)
                s = br.readLine()
            }
            br.close()
            sq.close()
            view.post {
                loadList()
                view.showMessage(R.string.ready)
            }
        }
    }

    fun onDestroy() {
        scope.cancel()
    }
}
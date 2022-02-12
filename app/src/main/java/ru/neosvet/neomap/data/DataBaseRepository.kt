package ru.neosvet.neomap.data

import android.content.ContentValues

class DataBaseRepository(
    private val db: DataBase
) : MarkersRepository {

    override fun addMarker(marker: NeoMarker) {
        val sq = db.writableDatabase
        val cv = ContentValues()
        cv.put(DataBase.NAME, marker.name)
        cv.put(DataBase.LAT, marker.lat)
        cv.put(DataBase.LNG, marker.lng)
        val r = sq.update(DataBase.TABLE, cv, DataBase.NAME + " = ?", arrayOf(marker.name))
        if (r == 0) // no update
            sq.insert(DataBase.TABLE, null, cv)
        sq.close()
    }

    override fun containsMarker(name: String): Boolean {
        val sq = db.readableDatabase
        val cursor = sq.query(
            DataBase.TABLE, arrayOf(DataBase.NAME),
            DataBase.NAME + " = ?", arrayOf(name),
            null, null, null
        )
        val result = cursor.moveToFirst()
        sq.close()
        return result
    }

    override fun deleteMarker(name: String) {
        val sq = db.writableDatabase
        sq.delete(DataBase.TABLE, DataBase.NAME + " = ?", arrayOf(name))
        sq.close()
    }

    override fun getListMarkers(): List<NeoMarker> {
        val list = ArrayList<NeoMarker>();
        val sq = db.readableDatabase
        val cursor = sq.query(DataBase.TABLE, null, null, null, null, null, null)
        if (cursor.moveToFirst()) {
            val iName: Int = cursor.getColumnIndex(DataBase.NAME)
            val iLat: Int = cursor.getColumnIndex(DataBase.LAT)
            val iLng: Int = cursor.getColumnIndex(DataBase.LNG)
            do {
                val marker = NeoMarker(
                    name = cursor.getString(iName),
                    lat = cursor.getDouble(iLat),
                    lng = cursor.getDouble(iLng)
                )
                list.add(marker)
            } while (cursor.moveToNext())
        }
        cursor.close()
        sq.close()
        return list
    }
}
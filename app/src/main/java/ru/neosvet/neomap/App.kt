package ru.neosvet.neomap

import android.app.Application
import ru.neosvet.neomap.data.DataBase
import ru.neosvet.neomap.data.DataBaseRepository
import ru.neosvet.neomap.data.MarkersRepository

class App : Application() {
    companion object {
        lateinit var repository: MarkersRepository
            private set
    }

    override fun onCreate() {
        super.onCreate()
        repository = DataBaseRepository(DataBase(applicationContext))
    }
}
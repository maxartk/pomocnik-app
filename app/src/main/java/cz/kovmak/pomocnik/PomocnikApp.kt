package cz.kovmak.pomocnik

import android.app.Application
import cz.kovmak.pomocnik.data.database.AppDatabase

class PomocnikApp : Application() {
    val database: AppDatabase by lazy {
        AppDatabase.getInstance(applicationContext)
    }
}

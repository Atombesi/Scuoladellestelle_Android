package com.example.scuoladellestelle_android

import android.content.Context
import android.content.res.Configuration

object AppVersion {
    const val CODICE = "1.01"

    // 🎯 FUNZIONE GLOBALE: Rileva il tipo di dispositivo ovunque nel progetto
    fun isTablet(context: Context): Boolean {
        val screenLayout = context.resources.configuration.screenLayout
        return (screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE
    }
}
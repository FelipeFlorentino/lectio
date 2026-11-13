package com.felipe.palavradehoje.data

import android.content.Context
import androidx.core.content.edit

/** Canais do YouTube (padres) marcados como favoritos: aparecem primeiro na aba Homilias. */
object Favoritos {
    private const val PREFS = "favoritos"

    fun canais(context: Context): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getStringSet("canais", emptySet()) ?: emptySet()

    fun alternar(context: Context, canal: String): Set<String> {
        val novos = canais(context).toMutableSet().apply { if (!remove(canal)) add(canal) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putStringSet("canais", novos) }
        return novos
    }
}

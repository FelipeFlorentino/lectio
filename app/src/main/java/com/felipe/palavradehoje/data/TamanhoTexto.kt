package com.felipe.palavradehoje.data

import android.content.Context
import androidx.core.content.edit

/** Tamanho escolhido para os textos de leitura, guardado entre uma abertura e outra. */
object TamanhoTexto {
    private const val PREFS = "leitura"
    val opcoes = listOf(0.85f, 1f, 1.15f, 1.3f, 1.45f)

    fun atual(context: Context): Float =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getFloat("escala", 1f)

    /** Anda [passo] posições na lista de tamanhos (−1 diminui, +1 aumenta). */
    fun mudar(context: Context, passo: Int): Float {
        val i = opcoes.indexOfFirst { it >= atual(context) - 0.01f }.coerceAtLeast(0)
        val nova = opcoes[(i + passo).coerceIn(0, opcoes.lastIndex)]
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putFloat("escala", nova) }
        return nova
    }
}

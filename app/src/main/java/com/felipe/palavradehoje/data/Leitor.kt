package com.felipe.palavradehoje.data

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/** Lê as leituras do dia em voz alta com a voz do próprio Android (sem internet, sem custo). */
class Leitor(context: Context) {
    private val _lendo = MutableStateFlow(false)
    val lendo: StateFlow<Boolean> = _lendo

    private val _disponivel = MutableStateFlow(true)
    val disponivel: StateFlow<Boolean> = _disponivel

    private var pronto = false
    private var ultimoId: String? = null

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        pronto = status == TextToSpeech.SUCCESS && configurarVoz()
        _disponivel.value = pronto
    }

    private fun configurarVoz(): Boolean {
        val r = tts.setLanguage(Locale.forLanguageTag("pt-BR"))
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) return false
        tts.setSpeechRate(0.95f)
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String) {}
            override fun onDone(id: String) { if (id == ultimoId) _lendo.value = false }
            @Deprecated("Deprecated in Java")
            override fun onError(id: String) { _lendo.value = false }
            override fun onStop(id: String, interrupted: Boolean) { _lendo.value = false }
        })
        return true
    }

    fun ler(liturgia: Liturgia) {
        if (!pronto) return
        tts.stop()
        val trechos = liturgia.leituras.flatMap { l ->
            val abertura = buildString {
                append(l.tipo.substringBefore(" (")).append(". ")
                if (l.titulo.isNotBlank()) append(l.titulo).append(". ")
                l.refrao?.let { append("Refrão: ").append(it).append(". ") }
            }
            listOf(abertura) + dividir(limpar(l.texto))
        }
        trechos.forEachIndexed { i, trecho ->
            val id = "lectio-$i"
            if (i == trechos.lastIndex) ultimoId = id
            tts.speak(trecho, if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, id)
            tts.playSilentUtterance(350, TextToSpeech.QUEUE_ADD, null)
        }
        _lendo.value = trechos.isNotEmpty()
    }

    fun parar() {
        tts.stop()
        _lendo.value = false
    }

    fun encerrar() = tts.shutdown()

    // Tira os números de versículo ("20os fariseus") e os travessões do salmo.
    private fun limpar(texto: String) = texto
        .replace(Regex("""(?<=^|[\s“"‘(\[])\d{1,3}(?=[A-Za-zÀ-ÿ“"‘'(—–-])"""), "")
        .replace(Regex("""(?m)^\s*[–-]\s*"""), "")

    /** O Android limita o tamanho de cada fala; corta por frases. */
    private fun dividir(texto: String, max: Int = 3000): List<String> {
        if (texto.length <= max) return listOf(texto)
        val partes = mutableListOf<String>()
        val atual = StringBuilder()
        for (frase in texto.split(Regex("""(?<=[.!?;:])\s+"""))) {
            if (atual.length + frase.length > max && atual.isNotEmpty()) {
                partes += atual.toString(); atual.clear()
            }
            atual.append(frase).append(' ')
        }
        if (atual.isNotBlank()) partes += atual.toString()
        return partes
    }
}

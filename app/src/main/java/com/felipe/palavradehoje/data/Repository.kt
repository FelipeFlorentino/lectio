package com.felipe.palavradehoje.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.net.URLEncoder
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Busca as três partes do dia e guarda o texto bruto em cache,
 * para o app abrir sem internet depois que a notificação diária já baixou tudo.
 */
class Repository(context: Context) {
    private val app = context.applicationContext
    private val cacheDir = File(context.filesDir, "dia").apply { mkdirs() }

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // A IA pode levar mais de 30 s para compor a resposta.
    private val httpIa = http.newBuilder().readTimeout(120, TimeUnit.SECONDS).build()

    /** Momento da manhã do dia, ou null se ainda não há chave da API do Magisterium. */
    suspend fun momentoManha(data: LocalDate, forcar: Boolean = false): MomentoManha? {
        val chave = Magisterium.chave(app) ?: return null
        val liturgia = liturgia(data)
        val json = comCache("manha-$data.json", maxIdadeMin = null, forcar = forcar) {
            Magisterium.gerar(httpIa, chave, data, liturgia)
        }
        return Magisterium.parse(json)
    }

    suspend fun liturgia(data: LocalDate, forcar: Boolean = false): Liturgia {
        val url = "https://liturgia.up.railway.app/v2/?dia=${data.dayOfMonth}&mes=${data.monthValue}&ano=${data.year}"
        val json = comCache("liturgia-$data.json", maxIdadeMin = null, forcar = forcar) { baixar(url) }
        return LiturgiaParser.parse(json)
    }

    suspend fun homilias(data: LocalDate, forcar: Boolean = false): List<Homilia> {
        val url = buscaHomiliasUrl(data) + "&hl=pt-BR&gl=BR"
        // Novos vídeos saem ao longo do dia, então o cache vale só 2 horas.
        val html = comCache("homilias-$data.html", maxIdadeMin = 120, forcar = forcar) { baixar(url) }
        val videos = YoutubeParser.parse(html)
        if (videos.isEmpty()) throw IOException("Nenhum vídeo encontrado")
        return YoutubeParser.filtrarPorData(videos, data).take(20)
    }

    suspend fun reflexao(data: LocalDate, forcar: Boolean = false): Reflexao? {
        val url = vaticanUrl(data)
        val nome = "reflexao-$data.html"
        val html = comCache(nome, maxIdadeMin = null, forcar = forcar) { baixar(url) }
        // Página ainda sem a reflexão: não guarda, para tentar de novo mais tarde.
        return VaticanParser.parse(html, url).also { if (it == null) File(cacheDir, nome).delete() }
    }

    private suspend fun comCache(
        nome: String,
        maxIdadeMin: Long?,
        forcar: Boolean,
        rede: () -> String,
    ): String = withContext(Dispatchers.IO) {
        val arquivo = File(cacheDir, nome)
        val idadeMin = (System.currentTimeMillis() - arquivo.lastModified()) / 60_000
        val valido = arquivo.exists() && (maxIdadeMin == null || idadeMin < maxIdadeMin)
        if (valido && !forcar) return@withContext arquivo.readText()
        try {
            rede().also { arquivo.writeText(it) }
        } catch (e: IOException) {
            if (arquivo.exists()) arquivo.readText() else throw e
        }
    }

    private fun baixar(url: String): String {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Safari/537.36")
            .header("Accept-Language", "pt-BR,pt;q=0.9")
            .build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
            return resp.body?.string() ?: throw IOException("Resposta vazia")
        }
    }

    /** Apaga caches com mais de uma semana. */
    fun limparAntigos() {
        val limite = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        cacheDir.listFiles()?.filter { it.lastModified() < limite }?.forEach { it.delete() }
    }

    companion object {
        fun buscaHomiliasUrl(data: LocalDate): String {
            val termo = "homilia diária %02d/%02d/%d".format(data.dayOfMonth, data.monthValue, data.year)
            return "https://www.youtube.com/results?search_query=" + URLEncoder.encode(termo, "UTF-8")
        }

        fun vaticanUrl(data: LocalDate): String =
            "https://www.vaticannews.va/pt/palavra-do-dia/%d/%02d/%02d.html".format(data.year, data.monthValue, data.dayOfMonth)
    }
}

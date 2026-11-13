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

    fun temChave(): Boolean = Magisterium.chave(app) != null

    /** Santo do dia já guardado, sem chamar a API (null se ainda não foi pedido). */
    fun santoGuardado(data: LocalDate): Santo? =
        File(cacheDir, "santo-$data.json").takeIf { it.exists() }?.let { runCatching { Magisterium.parseSanto(it.readText()) }.getOrNull() }

    /** Pede ao Magisterium a biografia do santo do dia; só quando a pessoa toca no botão. */
    suspend fun santo(data: LocalDate): Santo {
        val chave = Magisterium.chave(app) ?: throw MagisteriumException("Ative a aba Manhã com sua chave do Magisterium para ver o santo do dia.")
        val celebracao = liturgia(data).celebracao
        val json = comCache("santo-$data.json", maxIdadeMin = null, forcar = false) {
            Magisterium.gerarSanto(httpIa, chave, data, celebracao)
        }
        return Magisterium.parseSanto(json)
    }

    suspend fun liturgia(data: LocalDate, forcar: Boolean = false): Liturgia {
        val url = "https://liturgia.up.railway.app/v2/?dia=${data.dayOfMonth}&mes=${data.monthValue}&ano=${data.year}"
        val json = comCache("liturgia-$data.json", maxIdadeMin = null, forcar = forcar) { baixar(url) }
        return LiturgiaParser.parse(json)
    }

    suspend fun homilias(data: LocalDate, forcar: Boolean = false, favoritos: Set<String> = emptySet()): List<Homilia> {
        val geral = YoutubeParser.filtrarPorData(buscar(buscaHomiliasUrl(data), "homilias-$data.html", forcar), data).take(20)
        // A busca geral nem sempre traz os padres favoritos: cada um ganha uma busca própria.
        val dosFavoritos = favoritos.take(5).flatMap { canal ->
            val termo = "homilia $canal %02d/%02d/%d".format(data.dayOfMonth, data.monthValue, data.year)
            val url = "https://www.youtube.com/results?search_query=" + URLEncoder.encode(termo, "UTF-8")
            val arquivo = "homilias-$data-${Integer.toHexString(canal.hashCode())}.html"
            val videos = runCatching { buscar(url, arquivo, forcar) }.getOrDefault(emptyList())
            // Só homilias e comentários do Evangelho: o mesmo canal publica terços, lives etc.
            YoutubeParser.comData(videos.filter { it.canal == canal }, data)
                .filter { v -> listOf("homilia", "evangelho", "liturgia", "palavra").any { v.titulo.contains(it, ignoreCase = true) } }
                .take(2)
        }
        val todos = (dosFavoritos + geral).distinctBy { it.videoId }
        if (todos.isEmpty()) throw IOException("Nenhum vídeo encontrado")
        return todos
    }

    private suspend fun buscar(url: String, arquivo: String, forcar: Boolean): List<Homilia> {
        // Novos vídeos saem ao longo do dia, então o cache vale só 2 horas.
        val html = comCache(arquivo, maxIdadeMin = 120, forcar = forcar) { baixar("$url&hl=pt-BR&gl=BR") }
        return YoutubeParser.parse(html)
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

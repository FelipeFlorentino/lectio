package com.felipe.palavradehoje.data

import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Converte o JSON de liturgia.up.railway.app/v2 no nosso modelo. */
object LiturgiaParser {
    private val secoes = listOf(
        "primeiraLeitura" to "Primeira Leitura",
        "salmo" to "Salmo Responsorial",
        "segundaLeitura" to "Segunda Leitura",
        "evangelho" to "Evangelho",
        "extras" to "Leitura",
    )

    fun parse(json: String): Liturgia {
        val root = JSONObject(json)
        val leiturasObj = root.optJSONObject("leituras") ?: JSONObject()
        val leituras = mutableListOf<Leitura>()
        for ((chave, nome) in secoes) {
            val lista = leiturasObj.optJSONArray(chave) ?: continue
            for (i in 0 until lista.length()) {
                val item = lista.optJSONObject(i) ?: continue
                val texto = item.optString("texto").trim()
                if (texto.isEmpty()) continue
                // Mais de uma opção para a mesma leitura (ex.: forma longa e breve).
                val tipo = if (lista.length() > 1 && chave != "extras") "$nome (opção ${i + 1})" else nome
                leituras += Leitura(
                    tipo = tipo,
                    titulo = item.optString("titulo").trim(),
                    referencia = item.optString("referencia").trim(),
                    texto = texto,
                    refrao = item.optString("refrao").trim().ifEmpty { null },
                )
            }
        }
        val oracoes = root.optJSONObject("oracoes")
        return Liturgia(
            celebracao = root.optString("liturgia").trim(),
            cor = root.optString("cor").trim(),
            leituras = leituras,
            oracaoDoDia = oracoes?.optString("coleta")?.trim()?.ifEmpty { null },
        )
    }
}

/** Extrai os vídeos da página de resultados de busca do YouTube (dados em ytInitialData). */
object YoutubeParser {
    private val initialData = Regex("""var ytInitialData = (\{.*?\});</script>""", RegexOption.DOT_MATCHES_ALL)

    fun parse(html: String): List<Homilia> {
        val json = initialData.find(html)?.groupValues?.get(1) ?: return emptyList()
        val out = LinkedHashMap<String, Homilia>()
        walk(JSONObject(json)) { v ->
            val id = v.optString("videoId")
            if (id.isEmpty() || out.containsKey(id)) return@walk
            val titulo = v.optJSONObject("title")?.optJSONArray("runs")?.joinRuns().orEmpty()
            if (titulo.isEmpty()) return@walk
            out[id] = Homilia(
                videoId = id,
                titulo = titulo,
                canal = v.optJSONObject("ownerText")?.optJSONArray("runs")?.joinRuns().orEmpty(),
                duracao = v.optJSONObject("lengthText")?.optString("simpleText")?.ifEmpty { null },
                thumbnail = "https://i.ytimg.com/vi/$id/hqdefault.jpg",
            )
        }
        return out.values.toList()
    }

    private fun JSONArray.joinRuns(): String =
        (0 until length()).joinToString("") { optJSONObject(it)?.optString("text").orEmpty() }.trim()

    private fun walk(node: Any?, onVideo: (JSONObject) -> Unit) {
        when (node) {
            is JSONObject -> {
                node.optJSONObject("videoRenderer")?.let(onVideo)
                for (key in node.keys()) walk(node.opt(key), onVideo)
            }
            is JSONArray -> for (i in 0 until node.length()) walk(node.opt(i), onVideo)
        }
    }

    /** Prefere vídeos cujo título cita a data; se forem poucos, devolve tudo. */
    fun filtrarPorData(videos: List<Homilia>, data: LocalDate): List<Homilia> =
        comData(videos, data).takeIf { it.size >= 3 } ?: videos

    /** Só os vídeos cujo título cita a data (para não trazer homilias antigas de um padre favorito). */
    fun comData(videos: List<Homilia>, data: LocalDate): List<Homilia> {
        val br = Locale.forLanguageTag("pt-BR")
        val marcas = listOf(
            data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
            data.format(DateTimeFormatter.ofPattern("dd/MM/yy")),
            data.format(DateTimeFormatter.ofPattern("dd/MM")),
            data.format(DateTimeFormatter.ofPattern("dd.MM")),
            data.format(DateTimeFormatter.ofPattern("d 'de' MMMM", br)),
            data.format(DateTimeFormatter.ofPattern("dd 'de' MMMM", br)),
        ).map { it.lowercase(br) }
        return videos.filter { v -> marcas.any { v.titulo.lowercase(br).contains(it) } }
    }
}

/** Lê a seção "As palavras dos Papas" da página Palavra do Dia do Vatican News. */
object VaticanParser {
    fun parse(html: String, url: String): Reflexao? {
        val doc = Jsoup.parse(html)
        for (secao in doc.select("section")) {
            val titulo = secao.selectFirst("h2")?.text()?.trim() ?: continue
            if (!titulo.contains("palavras do", ignoreCase = true)) continue
            val paragrafos = secao.select(".section__content p")
                .map { it.text().replace(' ', ' ').trim() }
                .filter { it.isNotEmpty() }
            if (paragrafos.isEmpty()) return null
            return Reflexao(titulo = titulo, paragrafos = paragrafos, fonte = "Vatican News", url = url)
        }
        return null
    }
}

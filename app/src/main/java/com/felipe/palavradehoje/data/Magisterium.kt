package com.felipe.palavradehoje.data

import android.content.Context
import androidx.core.content.edit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Erro com mensagem pronta para mostrar na tela. */
class MagisteriumException(mensagem: String) : IOException(mensagem)

/** Gera o Momento da manhã pela API de chat do Magisterium AI (compatível com OpenAI). */
object Magisterium {
    const val CONSOLE_URL = "https://www.magisterium.com/pt/developers/api"
    private const val URL = "https://www.magisterium.com/api/v1/chat/completions"
    private const val PREFS = "magisterium"
    private val BR = Locale.forLanguageTag("pt-BR")

    fun chave(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("chave", null)?.ifBlank { null }

    fun salvarChave(context: Context, chave: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putString("chave", chave.trim()) }
    }

    /** Chama a API e devolve o JSON do roteiro já limpo (é ele que vai para o cache). */
    fun gerar(http: OkHttpClient, chave: String, data: LocalDate, liturgia: Liturgia): String {
        val corpo = JSONObject()
            .put("model", "magisterium-1")
            .put("stream", false)
            .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt(data, liturgia))))
        val req = Request.Builder()
            .url(URL)
            .header("Authorization", "Bearer $chave")
            .post(corpo.toString().toRequestBody("application/json".toMediaType()))
            .build()
        http.newCall(req).execute().use { resp ->
            when (resp.code) {
                401, 403 -> throw MagisteriumException("A chave da API do Magisterium não foi aceita. Confira se ela foi copiada inteira.")
                429 -> throw MagisteriumException("O limite de pedidos do plano gratuito foi atingido. Tente de novo em um minuto.")
            }
            if (!resp.isSuccessful) throw MagisteriumException("O Magisterium respondeu com erro ${resp.code}.")
            val conteudo = JSONObject(resp.body!!.string())
                .getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").getString("content")
            return extrairJson(conteudo).also { parse(it) }
        }
    }

    fun parse(json: String): MomentoManha {
        val o = JSONObject(json)
        val pontos = o.optJSONArray("pontos") ?: JSONArray()
        val versiculo = o.optJSONObject("versiculo")
        return MomentoManha(
            tema = o.getString("tema"),
            resumo = o.optString("resumo"),
            pontos = (0 until pontos.length()).map { i ->
                val p = pontos.getJSONObject(i)
                MomentoManha.Ponto(p.optString("leitura"), p.optString("referencia"), p.optString("ideia"))
            },
            versiculo = versiculo?.optString("texto").orEmpty(),
            versiculoRef = versiculo?.optString("referencia").orEmpty(),
            pergunta = o.optString("pergunta"),
            gesto = o.optString("gesto"),
            oracao = o.optString("oracao"),
            santo = o.optString("santo").ifBlank { null },
        )
    }

    /** Tira marcas de citação ("[^1]", "[2]") e qualquer texto fora do objeto JSON. */
    private fun extrairJson(texto: String): String {
        val limpo = texto.replace(Regex("""\s*\[\^?\d+]"""), "")
        val ini = limpo.indexOf('{')
        val fim = limpo.lastIndexOf('}')
        if (ini < 0 || fim <= ini) throw MagisteriumException("Resposta do Magisterium em formato inesperado.")
        return limpo.substring(ini, fim + 1)
    }

    private fun prompt(data: LocalDate, liturgia: Liturgia): String {
        val dia = data.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", BR))
        val leituras = liturgia.leituras.joinToString("\n\n") { l ->
            val texto = if (l.texto.length > 1800) l.texto.take(1800) + "…" else l.texto
            "${l.tipo} (${l.referencia}):${l.refrao?.let { " Refrão: \"$it\"." } ?: ""}\n$texto"
        }
        return """
            Você prepara um momento de oração matinal curto (leitura de 3 a 5 minutos) para um católico leigo, a partir das leituras da missa do dia. O objetivo é levar a Palavra para a prática ao longo do dia. Seja fiel ao Magistério, simples e concreto, em português do Brasil. Não use citações numéricas entre colchetes.

            Dia: $dia — ${liturgia.celebracao}.

            $leituras

            Responda SOMENTE com um objeto JSON válido, sem texto antes ou depois, neste formato:
            {
             "tema": "título curto do dia, até 6 palavras",
             "resumo": "uma frase que liga as leituras, até 30 palavras",
             "pontos": [
               {"leitura": "Primeira Leitura", "referencia": "...", "ideia": "até 25 palavras"},
               {"leitura": "Salmo", "referencia": "...", "ideia": "..."},
               {"leitura": "Evangelho", "referencia": "...", "ideia": "..."}
             ],
             "versiculo": {"texto": "frase curta das leituras para repetir durante o dia", "referencia": "..."},
             "pergunta": "uma pergunta pessoal para meditar, até 25 palavras",
             "gesto": "um gesto concreto e simples para fazer hoje, até 25 palavras",
             "oracao": "oração breve em primeira pessoa, até 45 palavras",
             "santo": "uma frase sobre o santo do dia e o que ele ensina hoje, ou vazio se não houver"
            }
            Inclua um item em "pontos" para cada leitura do dia (se houver Segunda Leitura, inclua também).
        """.trimIndent()
    }
}

/** Registro dos dias em que o gesto foi vivido. */
object Pratica {
    private const val PREFS = "pratica"

    fun feito(context: Context, data: LocalDate): Boolean = dias(context).contains(data.toString())

    fun marcar(context: Context, data: LocalDate, feito: Boolean) {
        val atual = dias(context).toMutableSet()
        if (feito) atual += data.toString() else atual -= data.toString()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putStringSet("dias", atual) }
    }

    /** Dias seguidos com o gesto vivido, contando para trás a partir de [ate]. */
    fun sequencia(context: Context, ate: LocalDate): Int {
        val dias = dias(context)
        var d = ate
        var n = 0
        while (dias.contains(d.toString())) { n++; d = d.minusDays(1) }
        return n
    }

    private fun dias(context: Context): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getStringSet("dias", emptySet()) ?: emptySet()
}

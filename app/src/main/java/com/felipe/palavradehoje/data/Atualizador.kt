package com.felipe.palavradehoje.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.felipe.palavradehoje.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class NovaVersao(
    val versionCode: Int,
    val versionName: String,
    val notas: String,
    val apkUrl: String,
)

/**
 * Atualização pelo GitHub Releases: cada versão publicada traz um anexo
 * "Lectio-<versionCode>.apk", assinado com a mesma chave do app.
 *
 * O app só avisa e abre o download no navegador. Ele não baixa nem instala APKs
 * sozinho: o Play Protect bloqueia como nocivo um app de fora da loja que faz isso.
 */
class Atualizador {
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /** Devolve a versão publicada se ela for mais nova que a instalada. */
    suspend fun verificar(): NovaVersao? = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("https://api.github.com/repos/${BuildConfig.REPO_ATUALIZACAO}/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "Lectio/${BuildConfig.VERSION_NAME}")
            .build()
        val json = http.newCall(req).execute().use { resp ->
            if (resp.code == 404) return@withContext null // nenhuma versão publicada ainda
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
            JSONObject(resp.body!!.string())
        }
        val anexos = json.optJSONArray("assets") ?: return@withContext null
        val nomeApk = Regex("""Lectio-(\d+)\.apk""")
        for (i in 0 until anexos.length()) {
            val anexo = anexos.getJSONObject(i)
            val codigo = nomeApk.matchEntire(anexo.optString("name"))?.groupValues?.get(1)?.toInt() ?: continue
            if (codigo <= BuildConfig.VERSION_CODE) return@withContext null
            return@withContext NovaVersao(
                versionCode = codigo,
                versionName = json.optString("name").ifBlank { json.optString("tag_name") }.removePrefix("v"),
                notas = json.optString("body").trim(),
                apkUrl = anexo.getString("browser_download_url"),
            )
        }
        null
    }

    /** O navegador baixa o APK; a pessoa toca no arquivo baixado para instalar. */
    fun abrirDownload(context: Context, versao: NovaVersao) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(versao.apkUrl)))
    }
}

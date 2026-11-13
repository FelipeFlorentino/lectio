package com.felipe.palavradehoje.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import com.felipe.palavradehoje.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
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
 */
class Atualizador(context: Context) {
    private val app = context.applicationContext
    private val pasta = File(app.cacheDir, "atualizacao")
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
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

    suspend fun baixar(versao: NovaVersao, progresso: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        pasta.mkdirs()
        pasta.listFiles()?.forEach { it.delete() }
        val destino = File(pasta, "Lectio-${versao.versionCode}.apk")
        http.newCall(Request.Builder().url(versao.apkUrl).build()).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
            val corpo = resp.body!!
            val total = corpo.contentLength().toFloat()
            var lidos = 0L
            corpo.byteStream().use { entrada ->
                destino.outputStream().use { saida ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val n = entrada.read(buffer)
                        if (n < 0) break
                        saida.write(buffer, 0, n)
                        lidos += n
                        if (total > 0) progresso(lidos / total)
                    }
                }
            }
        }
        destino
    }

    /** O Android pede uma vez a permissão "instalar apps desta fonte" para o próprio app. */
    fun podeInstalar(): Boolean = app.packageManager.canRequestPackageInstalls()

    fun abrirPermissao(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${app.packageName}"))
        )
    }

    /** Abre o instalador do Android; a pessoa confirma tocando em "Atualizar". */
    fun instalar(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${app.packageName}.arquivos", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

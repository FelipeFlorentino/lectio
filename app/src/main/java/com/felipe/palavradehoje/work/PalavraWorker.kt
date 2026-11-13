package com.felipe.palavradehoje.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.felipe.palavradehoje.MainActivity
import com.felipe.palavradehoje.R
import com.felipe.palavradehoje.data.Favoritos
import com.felipe.palavradehoje.data.Repository
import java.time.LocalDate

/** Roda uma vez por dia: baixa leituras, homilias e reflexão e avisa por notificação. */
class PalavraWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repo = Repository(applicationContext)
        val hoje = LocalDate.now()
        val liturgia = try {
            repo.liturgia(hoje, forcar = true)
        } catch (e: Exception) {
            return if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
        // Homilias e reflexão são extras: se falharem, o app tenta de novo ao abrir.
        runCatching { repo.homilias(hoje, forcar = true, favoritos = Favoritos.canais(applicationContext)) }
        runCatching { repo.reflexao(hoje, forcar = true) }
        val manha = runCatching { repo.momentoManha(hoje) }.getOrNull()
        repo.limparAntigos()

        if (manha != null) {
            notificar(applicationContext, "Bom dia! ${manha.tema}", "Gesto de hoje: ${manha.gesto}")
        } else {
            val evangelho = liturgia.evangelho?.referencia
            val texto = listOfNotNull(liturgia.celebracao.ifEmpty { null }, evangelho?.let { "Evangelho: $it" })
                .joinToString(" · ")
            notificar(applicationContext, "Leituras, homilias e reflexão de hoje", texto)
        }
        return Result.success()
    }

    companion object {
        const val CANAL = "palavra-diaria"

        fun criarCanal(context: Context) {
            val canal = NotificationChannel(CANAL, "Palavra do dia", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Aviso diário com as leituras, homilias e reflexão" }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
        }

        fun notificar(context: Context, titulo: String, texto: String) {
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) return

            val abrir = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val notificacao = NotificationCompat.Builder(context, CANAL)
                .setSmallIcon(R.drawable.ic_notificacao)
                .setContentTitle(titulo)
                .setContentText(texto)
                .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
                .setContentIntent(abrir)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(1, notificacao)
        }
    }
}

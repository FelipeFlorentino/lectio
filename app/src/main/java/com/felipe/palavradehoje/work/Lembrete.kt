package com.felipe.palavradehoje.work

import android.content.Context
import androidx.core.content.edit
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** Preferências do lembrete diário e o agendamento no WorkManager. */
object Lembrete {
    private const val PREFS = "lembrete"
    private const val TRABALHO = "palavra-diaria"

    fun ativo(context: Context): Boolean = prefs(context).getBoolean("ativo", true)

    fun horario(context: Context): LocalTime =
        LocalTime.of(prefs(context).getInt("hora", 6), prefs(context).getInt("minuto", 0))

    fun salvar(context: Context, ativo: Boolean, horario: LocalTime) {
        prefs(context).edit {
            putBoolean("ativo", ativo)
            putInt("hora", horario.hour)
            putInt("minuto", horario.minute)
        }
        agendar(context, substituir = true)
    }

    fun agendar(context: Context, substituir: Boolean = false) {
        val wm = WorkManager.getInstance(context)
        if (!ativo(context)) {
            wm.cancelUniqueWork(TRABALHO)
            return
        }
        val agora = LocalDateTime.now()
        var proximo = agora.toLocalDate().atTime(horario(context))
        if (!proximo.isAfter(agora)) proximo = proximo.plusDays(1)
        val atraso = Duration.between(agora, proximo).toMinutes()

        val pedido = PeriodicWorkRequestBuilder<PalavraWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(atraso, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        wm.enqueueUniquePeriodicWork(
            TRABALHO,
            // UPDATE contaria o atraso a partir do agendamento antigo; recriar respeita o novo horário.
            if (substituir) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE else ExistingPeriodicWorkPolicy.KEEP,
            pedido,
        )
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

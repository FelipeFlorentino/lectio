package com.felipe.palavradehoje

import android.app.Application
import com.felipe.palavradehoje.work.Lembrete
import com.felipe.palavradehoje.work.PalavraWorker

class PalavraApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PalavraWorker.criarCanal(this)
        Lembrete.agendar(this)
    }
}

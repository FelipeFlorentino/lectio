package com.felipe.palavradehoje

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.felipe.palavradehoje.ui.MainViewModel
import com.felipe.palavradehoje.ui.PalavraTheme
import com.felipe.palavradehoje.ui.TelaPrincipal
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()
    private var ultimoHoje = LocalDate.now()

    private val pedirNotificacao = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= 33) pedirNotificacao.launch(Manifest.permission.POST_NOTIFICATIONS)
        setContent {
            PalavraTheme { TelaPrincipal(vm) }
        }
    }

    override fun onResume() {
        super.onResume()
        ultimoHoje = vm.verificarDia(ultimoHoje)
    }
}

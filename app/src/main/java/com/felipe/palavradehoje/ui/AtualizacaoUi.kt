package com.felipe.palavradehoje.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Aviso no topo do app quando existe uma versão nova publicada. */
@Composable
fun BannerAtualizacao(estado: Atualizacao, onAtualizar: () -> Unit, onDispensar: () -> Unit) {
    val versao = when (estado) {
        is Atualizacao.Disponivel -> estado.versao
        is Atualizacao.Baixando -> estado.versao
        is Atualizacao.Pronta -> estado.versao
        is Atualizacao.Erro -> estado.versao
        else -> null
    } ?: return

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.SystemUpdate, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text(
                    "Nova versão ${versao.versionName} disponível",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (estado !is Atualizacao.Baixando) {
                    IconButton(onClick = onDispensar) {
                        Icon(Icons.Filled.Close, contentDescription = "Agora não", modifier = Modifier.size(18.dp))
                    }
                }
            }
            if (versao.notas.isNotBlank()) {
                Text(
                    versao.notas,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 30.dp, end = 12.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Column(Modifier.padding(start = 30.dp, end = 12.dp)) {
                when (estado) {
                    is Atualizacao.Baixando -> {
                        LinearProgressIndicator(progress = { estado.progresso }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(6.dp))
                        Text("Baixando… ${(estado.progresso * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
                    }
                    is Atualizacao.Erro -> {
                        Text(estado.mensagem, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(6.dp))
                        Button(onClick = onAtualizar) { Text("Tentar de novo") }
                    }
                    is Atualizacao.Pronta -> Button(onClick = onAtualizar) { Text("Instalar") }
                    else -> Button(onClick = onAtualizar) { Text("Atualizar") }
                }
            }
        }
    }
}

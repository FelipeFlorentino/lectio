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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.felipe.palavradehoje.data.Santo

/** Botão para ouvir as leituras em voz alta (voz do próprio Android, funciona sem internet). */
@Composable
fun CartaoOuvir(lendo: Boolean, disponivel: Boolean, onOuvir: () -> Unit, onParar: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (lendo) "Lendo as leituras…" else "Ouvir as leituras", style = MaterialTheme.typography.titleMedium)
                Text(
                    when {
                        !disponivel -> "Instale a voz em português nas configurações de \"Saída de texto para fala\" do Android."
                        lendo -> "Toque para parar."
                        else -> "Para ouvir no carro, numa caminhada ou de olhos fechados."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Spacer(Modifier.width(12.dp))
            FilledIconButton(onClick = if (lendo) onParar else onOuvir, enabled = disponivel, modifier = Modifier.size(52.dp)) {
                Icon(
                    if (lendo) Icons.Filled.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = if (lendo) "Parar" else "Ouvir",
                )
            }
        }
    }
}

/** Santo do dia: a biografia só é pedida ao Magisterium quando a pessoa toca no botão. */
@Composable
fun CartaoSanto(estado: Estado<Santo?>, onConhecer: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("SANTO DO DIA", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            when (estado) {
                Estado.Carregando -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Buscando no Magisterium…", style = MaterialTheme.typography.bodyMedium)
                }
                is Estado.Erro -> {
                    Text(estado.mensagem, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = onConhecer) { Text("Tentar de novo") }
                }
                is Estado.Ok -> {
                    val s = estado.valor
                    if (s == null) {
                        Text(
                            "Quem foi o santo de hoje e o que a vida dele ensina, em um minuto de leitura.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(onClick = onConhecer) { Text("Conhecer o santo do dia") }
                    } else {
                        Text(s.nome, style = MaterialTheme.typography.titleLarge)
                        val sub = listOf(s.titulo, s.viveu).filter { it.isNotBlank() }.joinToString(" · ")
                        if (sub.isNotEmpty()) {
                            Text(sub, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(s.biografia, style = MaterialTheme.typography.bodyLarge)
                        if (s.licao.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Text(s.licao, style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic), color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Preparado pelo Magisterium AI.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/** Estrela ao lado do nome do padre: favoritos aparecem primeiro na lista de homilias. */
@Composable
fun BotaoFavorito(favorito: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            if (favorito) Icons.Filled.Star else Icons.Filled.StarBorder,
            contentDescription = if (favorito) "Tirar dos favoritos" else "Marcar padre como favorito",
            tint = if (favorito) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

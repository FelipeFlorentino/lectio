package com.felipe.palavradehoje.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.felipe.palavradehoje.data.Magisterium
import com.felipe.palavradehoje.data.MomentoManha

@Composable
fun AbaManha(
    estado: Estado<MomentoManha?>,
    gestoFeito: Boolean,
    sequencia: Int,
    onMarcarGesto: (Boolean) -> Unit,
    onSalvarChave: (String) -> Unit,
    onGerarNovamente: () -> Unit,
    onVerLeituras: () -> Unit,
) {
    var trocarChave by remember { mutableStateOf(false) }

    when (estado) {
        Estado.Carregando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text(
                    "Preparando o momento da manhã…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        is Estado.Erro -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp)) {
            item {
                Text(estado.mensagem, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onGerarNovamente) { Text("Tentar de novo") }
                    TextButton(onClick = { trocarChave = true }) { Text("Trocar chave") }
                }
            }
        }
        is Estado.Ok -> {
            val m = estado.valor
            if (m == null) ConfigurarChave(onSalvarChave)
            else Roteiro(m, gestoFeito, sequencia, onMarcarGesto, onVerLeituras, onGerarNovamente) { trocarChave = true }
        }
    }

    if (trocarChave) DialogoChave(onFechar = { trocarChave = false }, onSalvar = { trocarChave = false; onSalvarChave(it) })
}

@Composable
private fun Roteiro(
    m: MomentoManha,
    gestoFeito: Boolean,
    sequencia: Int,
    onMarcarGesto: (Boolean) -> Unit,
    onVerLeituras: () -> Unit,
    onGerarNovamente: () -> Unit,
    onTrocarChave: () -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 1. Tema e resumo
        item {
            Column(Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.WbSunny, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("MOMENTO DA MANHÃ · 4 MIN", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                }
                Spacer(Modifier.height(8.dp))
                Text(m.tema, style = MaterialTheme.typography.headlineSmall)
                if (m.resumo.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(m.resumo, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // 2. A Palavra em poucas linhas
        item {
            Bloco(titulo = "A PALAVRA DE HOJE") {
                m.pontos.forEachIndexed { i, p ->
                    if (i > 0) Spacer(Modifier.height(14.dp))
                    Row {
                        Box(
                            Modifier.padding(top = 2.dp).size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("${i + 1}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "${p.leitura} · ${p.referencia}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(p.ideia, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp, lineHeight = 23.sp))
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = onVerLeituras, contentPadding = PaddingValues(0.dp)) { Text("Ler as leituras completas") }
            }
        }

        // 3. Versículo para levar
        if (m.versiculo.isNotBlank()) item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "PARA REPETIR DURANTE O DIA",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "“${m.versiculo.trim('“', '”', '"')}”",
                        style = MaterialTheme.typography.titleLarge.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onPrimary,
                        textAlign = TextAlign.Center,
                    )
                    if (m.versiculoRef.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(m.versiculoRef, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f))
                    }
                }
            }
        }

        // 4. Meditar
        if (m.pergunta.isNotBlank()) item {
            Bloco(titulo = "PARA MEDITAR") {
                Text(m.pergunta, style = MaterialTheme.typography.bodyLarge)
            }
        }

        // 5. Viver: o gesto do dia e o registro da prática
        if (m.gesto.isNotBlank()) item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("PARA VIVER HOJE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(8.dp))
                    Text(m.gesto, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(Modifier.height(14.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (gestoFeito) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceContainerLow)
                            .clickable { onMarcarGesto(!gestoFeito) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(24.dp).clip(CircleShape)
                                .background(if (gestoFeito) Color.White else MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (gestoFeito) Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            if (gestoFeito) "Vivi isso hoje" else "Marcar quando viver este gesto",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (gestoFeito) Color.White else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    if (sequencia >= 2) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "$sequencia dias seguidos levando a Palavra para a vida",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
        }

        // 6. Rezar
        if (m.oracao.isNotBlank()) item {
            Bloco(titulo = "ORAÇÃO") {
                Text(m.oracao, style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic))
            }
        }

        // 7. Santo do dia, em uma linha
        m.santo?.let { santo ->
            item {
                Text(
                    santo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }

        item {
            Column(Modifier.padding(top = 4.dp, bottom = 24.dp)) {
                Text(
                    "Preparado pelo Magisterium AI a partir das leituras do dia.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                Row {
                    TextButton(onClick = onGerarNovamente) { Text("Gerar outra versão") }
                    TextButton(onClick = onTrocarChave) { Text("Trocar chave") }
                }
            }
        }
    }
}

@Composable
private fun Bloco(titulo: String, conteudo: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(titulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            conteudo()
        }
    }
}

@Composable
private fun ConfigurarChave(onSalvarChave: (String) -> Unit) {
    val context = LocalContext.current
    var chave by remember { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item {
            Bloco(titulo = "MOMENTO DA MANHÃ") {
                Text("Um roteiro de 4 minutos para começar o dia", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(
                    "A cada manhã o Magisterium AI resume as leituras em poucas linhas e sugere uma pergunta para meditar, " +
                        "um gesto concreto para viver no dia e uma oração breve.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp, lineHeight = 23.sp),
                )
                Spacer(Modifier.height(16.dp))
                Text("Para ativar, uma vez só:", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                listOf(
                    "Abra o Console da API do Magisterium e entre com sua conta.",
                    "Crie uma chave de API. O plano gratuito é suficiente.",
                    "Copie a chave e cole aqui embaixo.",
                ).forEachIndexed { i, passo ->
                    Text("${i + 1}. $passo", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp))
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { abrir(context, Magisterium.CONSOLE_URL) }) {
                    Text("Abrir Console da API")
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = chave,
                    onValueChange = { chave = it },
                    label = { Text("Chave da API") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = { onSalvarChave(chave) }, enabled = chave.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                    Text("Ativar")
                }
            }
        }
    }
}

@Composable
private fun DialogoChave(onFechar: () -> Unit, onSalvar: (String) -> Unit) {
    var chave by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text("Chave da API do Magisterium") },
        text = {
            OutlinedTextField(value = chave, onValueChange = { chave = it }, label = { Text("Nova chave") }, singleLine = true)
        },
        confirmButton = { Button(onClick = { onSalvar(chave) }, enabled = chave.isNotBlank()) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onFechar) { Text("Cancelar") } },
    )
}

package com.felipe.palavradehoje.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.felipe.palavradehoje.BuildConfig
import com.felipe.palavradehoje.data.Homilia
import com.felipe.palavradehoje.data.Leitura
import com.felipe.palavradehoje.data.Liturgia
import com.felipe.palavradehoje.data.Reflexao
import com.felipe.palavradehoje.data.Repository
import com.felipe.palavradehoje.work.Lembrete
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val BR = Locale.forLanguageTag("pt-BR")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaPrincipal(vm: MainViewModel) {
    val data by vm.data.collectAsState()
    val liturgia by vm.liturgia.collectAsState()
    val homilias by vm.homilias.collectAsState()
    val reflexao by vm.reflexao.collectAsState()
    val manha by vm.manha.collectAsState()
    val gestoFeito by vm.gestoFeito.collectAsState()
    val sequencia by vm.sequencia.collectAsState()
    val atualizando by vm.atualizando.collectAsState()
    val atualizacao by vm.atualizacao.collectAsState()
    var aba by rememberSaveable { mutableIntStateOf(0) }
    var mostrarLembrete by remember { mutableStateOf(false) }
    var bannerDispensado by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Cabecalho(
                data = data,
                liturgia = (liturgia as? Estado.Ok)?.valor,
                onAnterior = { vm.mudarDia(-1) },
                onProximo = { vm.mudarDia(1) },
                onHoje = vm::irParaHoje,
                onLembrete = { mostrarLembrete = true },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (!bannerDispensado) {
                BannerAtualizacao(
                    estado = atualizacao,
                    onAtualizar = { vm.atualizar(context) },
                    onDispensar = { bannerDispensado = true },
                )
            }
            PrimaryTabRow(
                selectedTabIndex = aba,
                containerColor = MaterialTheme.colorScheme.background,
            ) {
                listOf("Manhã", "Leituras", "Homilias", "Reflexão").forEachIndexed { i, nome ->
                    Tab(selected = aba == i, onClick = { aba = i }, text = { Text(nome) })
                }
            }
            PullToRefreshBox(
                isRefreshing = atualizando,
                onRefresh = { vm.carregar(forcar = true) },
                modifier = Modifier.fillMaxSize(),
            ) {
                when (aba) {
                    0 -> AbaManha(
                        estado = manha,
                        gestoFeito = gestoFeito,
                        sequencia = sequencia,
                        onMarcarGesto = vm::marcarGesto,
                        onSalvarChave = vm::salvarChave,
                        onGerarNovamente = { vm.carregarManha(novo = true) },
                        onVerLeituras = { aba = 1 },
                    )
                    1 -> Conteudo(liturgia) { AbaLeituras(it) }
                    2 -> Conteudo(homilias) { AbaHomilias(it, data) }
                    else -> Conteudo(reflexao) { AbaReflexao(it, (liturgia as? Estado.Ok)?.valor, data) }
                }
            }
        }
    }

    if (mostrarLembrete) {
        DialogoLembrete(
            atualizacao = atualizacao,
            onVerificar = { bannerDispensado = false; vm.verificarAtualizacao(manual = true) },
            onFechar = { mostrarLembrete = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Cabecalho(
    data: LocalDate,
    liturgia: Liturgia?,
    onAnterior: () -> Unit,
    onProximo: () -> Unit,
    onHoje: () -> Unit,
    onLembrete: () -> Unit,
) {
    val hoje = data == LocalDate.now()
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onAnterior) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Dia anterior")
                }
                Column(
                    Modifier.weight(1f).clickable(enabled = !hoje, onClick = onHoje),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        if (hoje) "HOJE" else "TOQUE PARA VOLTAR A HOJE",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        data.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", BR))
                            .replaceFirstChar { it.titlecase(BR) },
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onProximo) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Próximo dia")
                }
                IconButton(onClick = onLembrete) {
                    Icon(Icons.Filled.NotificationsActive, contentDescription = "Lembrete diário")
                }
            }
            if (liturgia != null && liturgia.celebracao.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(12.dp).clip(CircleShape).background(corLiturgica(liturgia.cor))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        liturgia.celebracao + if (liturgia.cor.isNotEmpty()) " · ${liturgia.cor}" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun <T> Conteudo(estado: Estado<T>, ok: @Composable (T) -> Unit) {
    when (estado) {
        Estado.Carregando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is Estado.Erro -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(32.dp)) {
            item {
                Text(
                    estado.mensagem,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        is Estado.Ok -> ok(estado.valor)
    }
}

// ---------- Leituras ----------

@Composable
private fun AbaLeituras(liturgia: Liturgia) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(liturgia.leituras) { CartaoLeitura(it) }
        item {
            Text(
                "Fonte: liturgia.up.railway.app (Liturgia Diária)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun CartaoLeitura(leitura: Leitura) {
    val evangelho = leitura.tipo.startsWith("Evangelho")
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (evangelho) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    leitura.tipo.uppercase(BR),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    leitura.referencia,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            if (leitura.titulo.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(leitura.titulo, style = MaterialTheme.typography.titleMedium)
            }
            leitura.refrao?.let {
                Spacer(Modifier.height(10.dp))
                Text(
                    "R. $it",
                    style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic, fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                formatarVersiculos(leitura.texto, MaterialTheme.colorScheme.secondary),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

// Números de versículo vêm colados no texto ("20os fariseus"); viram sobrescritos.
private val versiculo = Regex("""(?<=^|[\s“"‘(\[])(\d{1,3})(?=[A-Za-zÀ-ÿ“"‘'(—–-])""")

private fun formatarVersiculos(texto: String, cor: Color): AnnotatedString = buildAnnotatedString {
    var i = 0
    for (m in versiculo.findAll(texto)) {
        append(texto.substring(i, m.range.first))
        withStyle(SpanStyle(fontSize = 11.sp, baselineShift = BaselineShift.Superscript, color = cor, fontWeight = FontWeight.Bold)) {
            append(m.value)
        }
        // Espaço fino sem quebra: o número nunca fica sozinho no fim da linha.
        append(' ')
        i = m.range.last + 1
    }
    append(texto.substring(i))
}

// ---------- Homilias ----------

@Composable
private fun AbaHomilias(videos: List<Homilia>, data: LocalDate) {
    val context = LocalContext.current
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(videos, key = { it.videoId }) { CartaoVideo(it) { abrir(context, it.url) } }
        item {
            OutlinedButton(
                onClick = { abrir(context, Repository.buscaHomiliasUrl(data)) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Ver mais no YouTube")
            }
        }
    }
}

@Composable
private fun CartaoVideo(video: Homilia, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
            AsyncImage(
                model = video.thumbnail,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Box(
                Modifier.align(Alignment.Center).size(56.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Assistir", tint = Color.White, modifier = Modifier.size(34.dp))
            }
            video.duracao?.let {
                Text(
                    it,
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
        Column(Modifier.padding(16.dp)) {
            Text(video.titulo, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (video.canal.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(video.canal, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ---------- Reflexão ----------

@Composable
private fun AbaReflexao(reflexao: Reflexao?, liturgia: Liturgia?, data: LocalDate) {
    val context = LocalContext.current
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("REFLEXÃO DO EVANGELHO", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(6.dp))
                    Text(reflexao?.titulo ?: "As palavras dos Papas", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(12.dp))
                    if (reflexao == null) {
                        Text(
                            "A reflexão deste dia ainda não foi publicada. Ela costuma sair na véspera ou de madrugada.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        reflexao.paragrafos.forEach {
                            Text(it, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 12.dp))
                        }
                    }
                    TextButton(onClick = { abrir(context, reflexao?.url ?: Repository.vaticanUrl(data)) }) {
                        Text("Ler no Vatican News")
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        liturgia?.oracaoDoDia?.let { oracao ->
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text("ORAÇÃO DO DIA", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.height(10.dp))
                        Text(oracao, style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic))
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ---------- Lembrete ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoLembrete(atualizacao: Atualizacao, onVerificar: () -> Unit, onFechar: () -> Unit) {
    val context = LocalContext.current
    var ativo by remember { mutableStateOf(Lembrete.ativo(context)) }
    val atual = remember { Lembrete.horario(context) }
    val relogio = rememberTimePickerState(initialHour = atual.hour, initialMinute = atual.minute, is24Hour = true)

    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text("Lembrete diário") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Avisar todo dia", modifier = Modifier.weight(1f))
                    Switch(checked = ativo, onCheckedChange = { ativo = it })
                }
                if (ativo) {
                    Spacer(Modifier.height(16.dp))
                    TimeInput(state = relogio)
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Versão ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                when (atualizacao) {
                    Atualizacao.Verificando -> Text("Verificando…", style = MaterialTheme.typography.bodyMedium)
                    Atualizacao.EmDia -> Text("Você já tem a versão mais recente.", style = MaterialTheme.typography.bodyMedium)
                    is Atualizacao.Erro -> Text(atualizacao.mensagem, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    is Atualizacao.Disponivel ->
                        Text("Há uma versão nova: veja o aviso no topo do app.", style = MaterialTheme.typography.bodyMedium)
                    Atualizacao.Nenhuma -> Unit
                }
                TextButton(onClick = onVerificar, contentPadding = PaddingValues(0.dp)) { Text("Verificar atualizações") }
            }
        },
        confirmButton = {
            Button(onClick = {
                Lembrete.salvar(context, ativo, LocalTime.of(relogio.hour, relogio.minute))
                onFechar()
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onFechar) { Text("Cancelar") } },
    )
}

internal fun abrir(context: Context, url: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

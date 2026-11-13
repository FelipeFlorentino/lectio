package com.felipe.palavradehoje.ui

import android.app.Application
import android.content.Context
import com.felipe.palavradehoje.data.Atualizador
import com.felipe.palavradehoje.data.NovaVersao
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.felipe.palavradehoje.data.Homilia
import com.felipe.palavradehoje.data.Liturgia
import com.felipe.palavradehoje.data.Magisterium
import com.felipe.palavradehoje.data.MagisteriumException
import com.felipe.palavradehoje.data.MomentoManha
import com.felipe.palavradehoje.data.Pratica
import com.felipe.palavradehoje.data.Reflexao
import com.felipe.palavradehoje.data.Repository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface Estado<out T> {
    data object Carregando : Estado<Nothing>
    data class Ok<T>(val valor: T) : Estado<T>
    data class Erro(val mensagem: String) : Estado<Nothing>
}

sealed interface Atualizacao {
    data object Nenhuma : Atualizacao
    data object Verificando : Atualizacao
    data object EmDia : Atualizacao
    data class Disponivel(val versao: NovaVersao) : Atualizacao
    data class Erro(val mensagem: String) : Atualizacao
}

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository(app)
    private val atualizador = Atualizador()

    val atualizacao = MutableStateFlow<Atualizacao>(Atualizacao.Nenhuma)

    /** Ao abrir o app, verifica em silêncio; [manual] mostra também "já está em dia" e erros. */
    fun verificarAtualizacao(manual: Boolean = false) {
        if (manual) atualizacao.value = Atualizacao.Verificando
        viewModelScope.launch {
            atualizacao.value = try {
                atualizador.verificar()?.let { Atualizacao.Disponivel(it) }
                    ?: if (manual) Atualizacao.EmDia else Atualizacao.Nenhuma
            } catch (e: Exception) {
                if (manual) Atualizacao.Erro("Não foi possível verificar. Confira a internet.") else Atualizacao.Nenhuma
            }
        }
    }

    /** Abre o download da versão nova no navegador. */
    fun atualizar(context: Context) {
        (atualizacao.value as? Atualizacao.Disponivel)?.let { atualizador.abrirDownload(context, it.versao) }
    }

    private val _data = MutableStateFlow(LocalDate.now())
    val data: StateFlow<LocalDate> = _data

    val liturgia = MutableStateFlow<Estado<Liturgia>>(Estado.Carregando)
    val homilias = MutableStateFlow<Estado<List<Homilia>>>(Estado.Carregando)
    val reflexao = MutableStateFlow<Estado<Reflexao?>>(Estado.Carregando)
    /** Ok(null) quando ainda não há chave da API do Magisterium. */
    val manha = MutableStateFlow<Estado<MomentoManha?>>(Estado.Carregando)
    val gestoFeito = MutableStateFlow(false)
    val sequencia = MutableStateFlow(0)
    val atualizando = MutableStateFlow(false)

    private var carga: Job? = null
    private var cargaManha: Job? = null

    fun salvarChave(chave: String) {
        Magisterium.salvarChave(getApplication(), chave)
        carregarManha()
    }

    /** Gera o roteiro da manhã; com [novo], pede ao Magisterium uma versão nova. */
    fun carregarManha(novo: Boolean = false) {
        cargaManha?.cancel()
        val dia = _data.value
        manha.value = Estado.Carregando
        cargaManha = viewModelScope.launch {
            manha.value = try {
                Estado.Ok(repo.momentoManha(dia, novo))
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Estado.Erro(
                    (e as? MagisteriumException)?.message
                        ?: "Não foi possível preparar o momento da manhã. Verifique a internet e tente de novo."
                )
            }
        }
    }

    fun marcarGesto(feito: Boolean) {
        Pratica.marcar(getApplication(), _data.value, feito)
        atualizarPratica()
    }

    private fun atualizarPratica() {
        gestoFeito.value = Pratica.feito(getApplication(), _data.value)
        sequencia.value = Pratica.sequencia(getApplication(), _data.value)
    }

    init {
        carregar()
        verificarAtualizacao()
    }

    fun mudarDia(dias: Long) {
        _data.value = _data.value.plusDays(dias)
        carregar()
    }

    fun irParaHoje() {
        _data.value = LocalDate.now()
        carregar()
    }

    /** Chamado ao voltar ao app: se virou o dia, mostra o dia novo. */
    fun verificarDia(ultimoHoje: LocalDate): LocalDate {
        val hoje = LocalDate.now()
        if (hoje != ultimoHoje && _data.value == ultimoHoje) irParaHoje()
        return hoje
    }

    fun carregar(forcar: Boolean = false) {
        carga?.cancel()
        val dia = _data.value
        atualizarPratica()
        // Atualizar a tela não gera outro roteiro: isso só acontece em "Gerar outra versão".
        if (!forcar || manha.value !is Estado.Ok) carregarManha()
        if (!forcar) {
            liturgia.value = Estado.Carregando
            homilias.value = Estado.Carregando
            reflexao.value = Estado.Carregando
        }
        carga = viewModelScope.launch {
            atualizando.value = forcar
            listOf(
                launch { liturgia.value = tentar { repo.liturgia(dia, forcar) } },
                launch { homilias.value = tentar { repo.homilias(dia, forcar) } },
                launch { reflexao.value = tentar { repo.reflexao(dia, forcar) } },
            ).joinAll()
            atualizando.value = false
        }
    }

    private suspend fun <T> tentar(bloco: suspend () -> T): Estado<T> = try {
        Estado.Ok(bloco())
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        Estado.Erro("Não foi possível carregar. Verifique a internet e puxe a tela para baixo para tentar de novo.")
    }
}

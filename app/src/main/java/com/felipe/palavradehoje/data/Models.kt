package com.felipe.palavradehoje.data

data class Leitura(
    val tipo: String,
    val titulo: String,
    val referencia: String,
    val texto: String,
    val refrao: String? = null,
)

data class Liturgia(
    val celebracao: String,
    val cor: String,
    val leituras: List<Leitura>,
    val oracaoDoDia: String?,
) {
    val evangelho: Leitura? get() = leituras.firstOrNull { it.tipo.startsWith("Evangelho") }
}

data class Homilia(
    val videoId: String,
    val titulo: String,
    val canal: String,
    val duracao: String?,
    val thumbnail: String,
) {
    val url: String get() = "https://www.youtube.com/watch?v=$videoId"
}

/** Roteiro curto para a oração da manhã, gerado pelo Magisterium AI a partir das leituras. */
data class MomentoManha(
    val tema: String,
    val resumo: String,
    val pontos: List<Ponto>,
    val versiculo: String,
    val versiculoRef: String,
    val pergunta: String,
    val gesto: String,
    val oracao: String,
    val santo: String?,
) {
    data class Ponto(val leitura: String, val referencia: String, val ideia: String)
}

data class Reflexao(
    val titulo: String,
    val paragrafos: List<String>,
    val fonte: String,
    val url: String,
)

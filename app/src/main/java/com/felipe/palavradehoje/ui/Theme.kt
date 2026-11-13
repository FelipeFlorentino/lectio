package com.felipe.palavradehoje.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Claro = lightColorScheme(
    primary = Color(0xFF7A2E2E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF3DED6),
    onPrimaryContainer = Color(0xFF3A1010),
    secondary = Color(0xFF8A6A2F),
    secondaryContainer = Color(0xFFF1E4C8),
    onSecondaryContainer = Color(0xFF2B2006),
    background = Color(0xFFFBF7F0),
    onBackground = Color(0xFF221B17),
    surface = Color(0xFFFBF7F0),
    onSurface = Color(0xFF221B17),
    surfaceVariant = Color(0xFFF1EAE0),
    onSurfaceVariant = Color(0xFF5A4F47),
    surfaceContainer = Color(0xFFF4EEE5),
    surfaceContainerLow = Color(0xFFFFFFFF),
    outlineVariant = Color(0xFFDCD1C4),
)

private val Escuro = darkColorScheme(
    primary = Color(0xFFF0B4A8),
    onPrimary = Color(0xFF4A1414),
    primaryContainer = Color(0xFF5E2222),
    onPrimaryContainer = Color(0xFFFFDAD3),
    secondary = Color(0xFFE2C68C),
    secondaryContainer = Color(0xFF4E3F1C),
    onSecondaryContainer = Color(0xFFF6E5BF),
    background = Color(0xFF181412),
    onBackground = Color(0xFFEDE0D8),
    surface = Color(0xFF181412),
    onSurface = Color(0xFFEDE0D8),
    surfaceVariant = Color(0xFF2A2421),
    onSurfaceVariant = Color(0xFFD3C4BA),
    surfaceContainer = Color(0xFF231E1B),
    surfaceContainerLow = Color(0xFF201B18),
    outlineVariant = Color(0xFF4A403A),
)

private val Serif = FontFamily.Serif

/** Tipografia base, sem a escala de leitura (o cabeçalho usa esta para não crescer). */
internal val Tipografia = Typography(
    headlineSmall = TextStyle(fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = Serif, fontSize = 18.sp, lineHeight = 29.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.2.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.6.sp),
)

/** Cor de destaque do tempo litúrgico (faixa do topo, abas, progresso da Manhã). */
val LocalCorDestaque = staticCompositionLocalOf { Color(0xFF7A2E2E) }

/**
 * [escala] aumenta ou diminui só os textos de leitura (leituras, roteiro, reflexão),
 * sem mexer nos botões e rótulos.
 */
@Composable
fun PalavraTheme(escala: Float = 1f, content: @Composable () -> Unit) {
    val tipografia = Tipografia.copy(
        bodyLarge = Tipografia.bodyLarge.copy(fontSize = 18.sp * escala, lineHeight = 29.sp * escala),
        titleLarge = Tipografia.titleLarge.copy(fontSize = 21.sp * escala, lineHeight = 28.sp * escala),
        headlineSmall = Tipografia.headlineSmall.copy(fontSize = 24.sp * escala, lineHeight = 30.sp * escala),
    )
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Escuro else Claro,
        typography = tipografia,
        content = content,
    )
}

/** Versão da cor litúrgica que aparece sobre o fundo claro: o branco vira dourado. */
fun corDestaque(nome: String): Color = when (nome.lowercase().trim()) {
    "branco", "" -> Color(0xFFC9A227)
    else -> corLiturgica(nome)
}

/** Cor litúrgica do dia, para o marcador do cabeçalho. */
fun corLiturgica(nome: String): Color = when (nome.lowercase().trim()) {
    "verde" -> Color(0xFF2E7D4F)
    "roxo", "violeta" -> Color(0xFF6A3D8F)
    "vermelho" -> Color(0xFFB3261E)
    "rosa", "róseo" -> Color(0xFFE48AB0)
    "branco" -> Color(0xFFF2EBDD)
    "preto" -> Color(0xFF222222)
    "dourado" -> Color(0xFFD4AF37)
    else -> Color(0xFF9E9E9E)
}

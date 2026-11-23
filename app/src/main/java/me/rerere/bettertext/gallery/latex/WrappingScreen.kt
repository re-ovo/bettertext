package me.rerere.bettertext.gallery.latex

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import me.rerere.bettertext.latex.Latex
import me.rerere.bettertext.latex.LatexChunk
import me.rerere.bettertext.latex.LatexSettings
import me.rerere.bettertext.latex.rememberLatexLayout
import me.rerere.bettertext.ui.components.Frame
import me.rerere.bettertext.ui.components.GalleryScaffold
import me.rerere.bettertext.ui.components.Heading
import me.rerere.bettertext.ui.components.SwitchItem

/** Shows how formulas break into lines as the available width changes. */
@Composable
internal fun WrappingScreen(onBack: () -> Unit) {
    val fractionSlider = rememberSliderState(0.6f, trackRange = 0.2f..1f)
    val fraction = fractionSlider.value
    var formula by rememberSaveable { mutableIntStateOf(0) }
    var leqno by rememberSaveable { mutableStateOf(false) }
    val fontSize = MaterialTheme.typography.bodyLarge.fontSize

    GalleryScaffold(title = "换行", onBack = onBack) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val width = (maxWidth - 32.dp) * fraction
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("容器宽度 ${width.value.roundToInt()} dp", style = MaterialTheme.typography.labelLarge)
                Slider(state = fractionSlider, onValueChange = { fractionSlider.value = it })

                Heading("行内公式自动换行", "只在顶层关系符、二元运算符和 \\allowbreak 之后断行，与浏览器中的 KaTeX 一致")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    wrapSamples.forEachIndexed { i, (label, _) ->
                        FilterChip(selected = i == formula, onClick = { formula = i }, label = { Text(label) })
                    }
                }
                val latex = wrapSamples[formula].second
                Frame(width) {
                    Latex(latex, fontSize = fontSize, color = MaterialTheme.colorScheme.onSurface)
                }

                Heading("断点（chunk）", "每个方框是一个不可再分的 chunk，BetterText 在 chunk 之间换行")
                ChunkFlow(latex, width, fontSize)

                Heading("块级公式与编号", "放不下时编号移到下一行；leqno 时编号在左侧，移到上一行")
                SwitchItem("编号在左", leqno, { leqno = it }, index = 0, count = 1, description = "leqno")
                val tagged = rememberLatexLayout(TAGGED_DISPLAY, LatexSettings(displayMode = true, leqno = leqno))
                Frame(width) {
                    Latex(tagged, Modifier.fillMaxWidth(), fontSize = fontSize, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun ChunkFlow(latex: String, width: Dp, fontSize: TextUnit) {
    val layout = rememberLatexLayout(latex)
    val outline = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    FlowRow(
        Modifier.width(width),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(layout.chunkCount) { i ->
            LatexChunk(
                layout,
                i,
                Modifier
                    .alignByBaseline()
                    .border(0.5.dp, outline),
                fontSize = fontSize,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
    Text(
        "共 ${layout.chunkCount} 个 chunk，单行宽 ${"%.2f".format(layout.width)} em",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

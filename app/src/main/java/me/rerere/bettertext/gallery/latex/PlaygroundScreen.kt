package me.rerere.bettertext.gallery.latex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import me.rerere.bettertext.latex.Latex
import me.rerere.bettertext.latex.LatexBlock
import me.rerere.bettertext.latex.LatexSettings
import me.rerere.bettertext.latex.rememberLatexLayout
import me.rerere.bettertext.ui.components.GalleryScaffold
import me.rerere.bettertext.ui.components.SegmentedGroup
import me.rerere.bettertext.ui.components.SwitchItem

private const val DEFAULT_LATEX = """f(x) = \int_{-\infty}^\infty \hat f(\xi)\,e^{2 \pi i \xi x} \,d\xi"""

private val snippets = listOf(
    """\frac{}{}""", """\sqrt{}""", "^{}", "_{}", """\sum_{}^{}""", """\int_{}^{}""",
    """\left(\right)""", """\begin{pmatrix}\end{pmatrix}""", "&", """\\""",
    """\alpha""", """\infty""", """\tag{}""",
)

/** Edits a formula, prefilled with [initial] when set, and previews it live. */
@Composable
internal fun PlaygroundScreen(initial: Sample?, onBack: () -> Unit) {
    var text by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        val latex = initial?.latex ?: DEFAULT_LATEX
        mutableStateOf(TextFieldValue(latex, TextRange(latex.length)))
    }
    var display by rememberSaveable { mutableStateOf(initial?.display ?: true) }
    var leqno by rememberSaveable { mutableStateOf(initial?.leqno ?: false) }
    var fleqn by rememberSaveable { mutableStateOf(initial?.fleqn ?: false) }
    val fontSizeSlider = rememberSliderState(18f, trackRange = 10f..40f)
    val fontSize = fontSizeSlider.value
    val macros = initial?.macros.orEmpty()

    val settings = LatexSettings(displayMode = display, leqno = leqno, fleqn = fleqn, macros = macros)
    val layout = rememberLatexLayout(text.text, settings)

    GalleryScaffold(title = "编辑器", onBack = onBack) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(Modifier.padding(20.dp)) {
                    val color = MaterialTheme.colorScheme.onSurface
                    if (display) {
                        LatexBlock(layout, fontSize = fontSize.sp, color = color)
                    } else {
                        Latex(layout, fontSize = fontSize.sp, color = color)
                    }
                }
            }
            val error = layout.error
            if (error != null) {
                Text(error.message.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            } else {
                Text(
                    "${layout.chunkCount} 个 chunk · 宽 %.2f em · 高 %.2f em · 深 %.2f em".format(layout.width, layout.height, layout.depth),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("LaTeX") },
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                minLines = 3,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (snippet in snippets) {
                    AssistChip(
                        onClick = { text = text.insert(snippet) },
                        label = { Text(snippet, fontFamily = FontFamily.Monospace) },
                    )
                }
                TextButton(onClick = { text = TextFieldValue() }) { Text("清空") }
            }
            if (macros.isNotEmpty()) {
                Text(
                    "宏：" + macros.entries.joinToString { (k, v) -> "$k → $v" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SegmentedGroup {
                SwitchItem("块级", display, { display = it }, index = 0, count = 3, description = "display mode")
                SwitchItem("编号在左", leqno, { leqno = it }, index = 1, count = 3, description = "leqno")
                SwitchItem("左对齐", fleqn, { fleqn = it }, index = 2, count = 3, description = "fleqn")
            }
            Text("字号 ${fontSize.roundToInt()} sp", style = MaterialTheme.typography.labelLarge)
            Slider(state = fontSizeSlider, onValueChange = { fontSizeSlider.value = it })
        }
    }
}

/** Replaces the selection with [snippet], placing the cursor inside its first `{}`. */
private fun TextFieldValue.insert(snippet: String): TextFieldValue {
    val start = selection.min
    val result = text.replaceRange(start, selection.max, snippet)
    val braces = snippet.indexOf("{}")
    val caret = start + if (braces >= 0) braces + 1 else snippet.length
    return TextFieldValue(result, TextRange(caret))
}

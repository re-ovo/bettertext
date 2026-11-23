package me.rerere.bettertext.gallery.text

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import me.rerere.bettertext.latex.DefaultMathScale
import me.rerere.bettertext.latex.Latex
import me.rerere.bettertext.latex.LatexLayout
import me.rerere.bettertext.latex.LatexSettings
import me.rerere.bettertext.latex.rememberLatexPainter
import me.rerere.bettertext.text.BetterInline
import me.rerere.bettertext.text.BetterSelectionContainer
import me.rerere.bettertext.text.BetterText
import me.rerere.bettertext.text.ComposableInline
import me.rerere.bettertext.text.InlineAlign
import me.rerere.bettertext.ui.components.Frame
import me.rerere.bettertext.ui.components.Heading
import me.rerere.bettertext.ui.components.SwitchItem

private val citations = mapOf(
    1 to "issuetracker.google.com/issues/427657659 · The line height of the text does not take the font size into account",
    2 to "issuetracker.google.com/issues/233102278 · Make SelectionRegistrar/SelectionManager public",
)

/** BetterText paragraphs mixing text, math, color swatches and citations. */
@Composable
internal fun RichTextTab(contentPadding: PaddingValues) {
    val fractionSlider = rememberSliderState(1f, trackRange = 0.3f..1f)
    val fraction = fractionSlider.value
    var useLineHeight by rememberSaveable { mutableStateOf(true) }
    var cited by rememberSaveable { mutableIntStateOf(0) }
    val bodyLarge = MaterialTheme.typography.bodyLarge
    val style = bodyLarge.copy(
        color = MaterialTheme.colorScheme.onSurface,
        lineHeight = if (useLineHeight) bodyLarge.lineHeight else TextUnit.Unspecified,
    )

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val width = (maxWidth - 32.dp) * fraction
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("容器宽度 ${width.value.roundToInt()} dp", style = MaterialTheme.typography.labelLarge)
            Slider(state = fractionSlider, onValueChange = { fractionSlider.value = it })
            SwitchItem(
                title = "使用 bodyLarge 行高（${bodyLarge.lineHeight}）",
                checked = useLineHeight,
                onCheckedChange = { useLineHeight = it },
                index = 0,
                count = 1,
                description = "关闭后按字体的自然高度排版",
            )

            Text(
                "长按选择文字，拖动可以跨段落选择；公式和引用整体选中，复制时输出源码",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            BetterSelectionContainer(obscuredPadding = contentPadding) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (sample in richTextSamples) {
                        Heading(sample.title, sample.description)
                        val (text, inlineContent) = rememberDemoText(sample.markup) { cited = it }
                        Frame(width) { BetterText(text, Modifier.padding(4.dp), style = style, inlineContent = inlineContent) }
                        if (sample.markup.contains("[^")) {
                            Text(
                                citations[cited]?.let { "[$cited] $it" } ?: "点击引用标记查看来源",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (sample.markup == TALL_PARAGRAPH) {
                            Text(
                                "对比：Compose Text + Placeholder",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.error,
                            )
                            Frame(width) { PlaceholderParagraph(TALL_PARAGRAPH, style, Modifier.padding(4.dp)) }
                            Text(
                                "设置了 lineHeight 时高公式和上下行重叠；占位符只能居中，公式基线和文字基线对不齐",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Heading("样式", "公式跟随所在位置的字号和颜色")
                    StyledParagraph(width, style)
                }
            }
        }
    }
}

@Composable
private fun StyledParagraph(width: Dp, style: TextStyle) {
    val markup = rememberMarkup()
    val (text, inlineContent) = remember(markup) {
        with(markup) {
            val text = buildAnnotatedString {
                appendMarkup(STYLED_PLAIN)
                withStyle(SpanStyle(fontSize = 24.sp)) { appendMarkup(STYLED_LARGE) }
                withStyle(SpanStyle(color = Color(0xFF26A69A))) { appendMarkup(STYLED_COLORED) }
            }
            text to inlineContent.toMap()
        }
    }
    Frame(width) { BetterText(text, Modifier.padding(4.dp), style = style, inlineContent = inlineContent) }
}

@Composable
private fun rememberDemoText(markup: String, onCite: (Int) -> Unit): Pair<AnnotatedString, Map<String, BetterInline>> {
    val converter = rememberMarkup(onCite)
    return remember(markup, converter) {
        with(converter) {
            buildAnnotatedString { appendMarkup(markup) } to inlineContent.toMap()
        }
    }
}

@Composable
private fun rememberMarkup(onCite: (Int) -> Unit = {}): DemoMarkup {
    val context = LocalContext.current
    val painter = rememberLatexPainter()
    val codeBackground = MaterialTheme.colorScheme.surfaceContainerHighest
    val chip = MaterialTheme.colorScheme.primaryContainer
    val onChip = MaterialTheme.colorScheme.onPrimaryContainer
    val label = MaterialTheme.typography.labelSmall
    return remember(context, painter, codeBackground, chip, onChip, label) {
        DemoMarkup(context, painter, codeBackground) { n ->
            ComposableInline(InlineAlign.Center) {
                Box(
                    Modifier
                        .padding(horizontal = 2.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(chip)
                        .clickable { onCite(n) }
                        .padding(horizontal = 5.dp),
                ) {
                    Text("$n", style = label, color = onChip)
                }
            }
        }
    }
}

/** The same paragraph with Compose's Text, one placeholder per formula, for comparison. */
@Composable
private fun PlaceholderParagraph(markup: String, style: TextStyle, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val (text, inlineContent) = remember(markup) {
        val content = HashMap<String, InlineTextContent>()
        val text = buildAnnotatedString {
            var i = 0
            while (i < markup.length) {
                val close = if (markup[i] == '$') markup.indexOf('$', i + 1) else -1
                if (close < 0) {
                    append(markup[i++])
                    continue
                }
                val layout = LatexLayout.create(markup.substring(i + 1, close), LatexSettings(), context)
                val id = "math${content.size}"
                // Latex reserves at least 0.75em above and 0.25em below the baseline.
                val height = maxOf(layout.height, 0.75f) + maxOf(layout.depth, 0.25f)
                content[id] = InlineTextContent(
                    Placeholder((layout.width * DefaultMathScale).em, (height * DefaultMathScale).em, PlaceholderVerticalAlign.TextCenter),
                ) {
                    Latex(layout, fontSize = style.fontSize, color = style.color)
                }
                appendInlineContent(id, markup.substring(i, close + 1))
                i = close + 1
            }
        }
        text to content
    }
    Text(text, modifier, style = style, inlineContent = inlineContent)
}

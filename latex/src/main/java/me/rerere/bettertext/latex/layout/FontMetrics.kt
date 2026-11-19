package me.rerere.bettertext.latex.layout

import me.rerere.bettertext.latex.data.FontMetricsData
import me.rerere.bettertext.latex.parse.Mode
import me.rerere.bettertext.latex.parse.UnicodeTables

/** Metrics of a single character, in em of its own font size. */
internal class CharacterMetrics(
    val depth: Float,
    val height: Float,
    val italic: Float,
    val skew: Float,
    val width: Float,
)

/**
 * TeX font parameters (sigmas of cmsy and xis of cmex) for text, script and
 * scriptscript sizes. Values are in em of the font size they apply to.
 */
internal class GlobalMetrics(private val sizeIndex: Int) {
    private fun v(values: FloatArray) = values[sizeIndex]

    val slant = v(floatArrayOf(0.250f, 0.250f, 0.250f))
    val space = 0f
    val stretch = 0f
    val shrink = 0f
    val xHeight = v(floatArrayOf(0.431f, 0.431f, 0.431f))
    val quad = v(floatArrayOf(1.000f, 1.171f, 1.472f))
    val extraSpace = 0f
    val num1 = v(floatArrayOf(0.677f, 0.732f, 0.925f))
    val num2 = v(floatArrayOf(0.394f, 0.384f, 0.387f))
    val num3 = v(floatArrayOf(0.444f, 0.471f, 0.504f))
    val denom1 = v(floatArrayOf(0.686f, 0.752f, 1.025f))
    val denom2 = v(floatArrayOf(0.345f, 0.344f, 0.532f))
    val sup1 = v(floatArrayOf(0.413f, 0.503f, 0.504f))
    val sup2 = v(floatArrayOf(0.363f, 0.431f, 0.404f))
    val sup3 = v(floatArrayOf(0.289f, 0.286f, 0.294f))
    val sub1 = v(floatArrayOf(0.150f, 0.143f, 0.200f))
    val sub2 = v(floatArrayOf(0.247f, 0.286f, 0.400f))
    val supDrop = v(floatArrayOf(0.386f, 0.353f, 0.494f))
    val subDrop = v(floatArrayOf(0.050f, 0.071f, 0.100f))
    val delim1 = v(floatArrayOf(2.390f, 1.700f, 1.980f))
    val delim2 = v(floatArrayOf(1.010f, 1.157f, 1.420f))
    val axisHeight = v(floatArrayOf(0.250f, 0.250f, 0.250f))
    val defaultRuleThickness = v(floatArrayOf(0.04f, 0.049f, 0.049f))
    val bigOpSpacing1 = v(floatArrayOf(0.111f, 0.111f, 0.111f))
    val bigOpSpacing2 = v(floatArrayOf(0.166f, 0.166f, 0.166f))
    val bigOpSpacing3 = v(floatArrayOf(0.2f, 0.2f, 0.2f))
    val bigOpSpacing4 = v(floatArrayOf(0.6f, 0.611f, 0.611f))
    val bigOpSpacing5 = v(floatArrayOf(0.1f, 0.143f, 0.143f))
    val sqrtRuleThickness = v(floatArrayOf(0.04f, 0.04f, 0.04f))
    val ptPerEm = v(floatArrayOf(10.0f, 10.0f, 10.0f))
    val doubleRuleSep = v(floatArrayOf(0.2f, 0.2f, 0.2f))
    val arrayRuleWidth = v(floatArrayOf(0.04f, 0.04f, 0.04f))
    val fboxsep = v(floatArrayOf(0.3f, 0.3f, 0.3f))
    val fboxrule = v(floatArrayOf(0.04f, 0.04f, 0.04f))
    val cssEmPerMu = quad / 18f

    companion object {
        private val bySizeIndex = Array(3) { GlobalMetrics(it) }

        /** Get the font metrics for a given size (1..11). */
        fun forSize(size: Int): GlobalMetrics = bySizeIndex[
            when {
                size >= 5 -> 0
                size >= 3 -> 1
                else -> 2
            },
        ]
    }
}

internal object FontMetrics {
    private val metricMap: Map<String, Map<Int, FloatArray>> by lazy { parse() }

    private fun parse(): Map<String, Map<Int, FloatArray>> {
        val result = HashMap<String, Map<Int, FloatArray>>()
        for (record in FontMetricsData.raw.split('\u0002')) {
            val idx = record.indexOf('\u0001')
            val font = record.substring(0, idx)
            val map = HashMap<Int, FloatArray>()
            for (entry in record.substring(idx + 1).split(';')) {
                val colon = entry.indexOf(':')
                val values = entry.substring(colon + 1).split(',')
                map[entry.substring(0, colon).toInt()] = FloatArray(5) { values[it].toFloat() }
            }
            result[font] = map
        }
        for ((font, cp, width) in widthFixes) result[font]?.get(cp)?.set(4, width)
        return result
    }

    /**
     * Widths that KaTeX's tables get wrong. KaTeX lets the browser measure
     * glyphs, so it never notices; we lay out with the table widths, so use
     * the advance widths of the bundled fonts instead. The tables copy the
     * width of ∫ to ∬ and ∭, which made the integrals overlap what follows.
     */
    private val widthFixes = listOf(
        Triple("Size1-Regular", 0x222C, 0.819f),
        Triple("Size1-Regular", 0x222D, 1.166f),
        Triple("Size2-Regular", 0x222C, 1.084f),
        Triple("Size2-Regular", 0x222D, 1.592f),
        Triple("Main-Regular", 0xB0, 0.5f),
        Triple("Main-Bold", 0xB0, 0.575f),
        Triple("Main-Italic", 0xB0, 0.511f),
        Triple("Main-BoldItalic", 0xB0, 0.789f),
        Triple("SansSerif-Regular", 0xB0, 0.383f),
        Triple("SansSerif-Bold", 0xB0, 0.617f),
        Triple("SansSerif-Italic", 0xB0, 0.512f),
    )

    /** Rough approximations for characters missing from the fonts. */
    private val extraCharacterMap: Map<Char, Char> = buildMap {
        val pairs = "ÅAÐDÞoåaðdþoАAБBВBГFДAЕEЖKЗ3ИNЙNКKЛNМMНHОOПNРPСCТTУyФOХXЦUЧhШWЩWЪBЫXЬBЭ3ЮXЯR" +
            "аaбbвaгrдyеeжmзeиnйnкnлnмmнnоoпnрpсcтoуyфbхxцnчnшwщwъaыmьaэeюmяr"
        var i = 0
        while (i < pairs.length) {
            put(pairs[i], pairs[i + 1])
            i += 2
        }
    }

    fun hasFont(font: String) = metricMap.containsKey(font)

    /** Whether [font] has metrics for the first character of [text] itself (no substitutes). */
    fun hasGlyph(text: String, font: String): Boolean =
        text.isNotEmpty() && metricMap[font]?.containsKey(text[0].code) == true

    /**
     * Look up the metrics of [character] in [font]. Returns null if the
     * character is not in the font.
     */
    fun getCharacterMetrics(character: String, font: String, mode: Mode): CharacterMetrics? {
        val map = metricMap[font] ?: error("Font metrics not found for font: $font.")
        if (character.isEmpty()) return null
        var ch = character.codePointAt(0)
        // KaTeX looks up by UTF-16 code unit
        if (ch > 0xFFFF) ch = character[0].code
        var metrics = map[ch]
        if (metrics == null) {
            val replacement = extraCharacterMap[character[0]]
            if (replacement != null) metrics = map[replacement.code]
        }
        if (metrics == null && mode == Mode.TEXT) {
            // Use the metrics of 'M' for scripts we support but have no metrics for.
            if (UnicodeTables.supportedCodepoint(ch)) metrics = map[77]
        }
        return metrics?.let { CharacterMetrics(it[0], it[1], it[2], it[3], it[4]) }
    }
}

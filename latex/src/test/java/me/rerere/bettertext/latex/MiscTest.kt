package me.rerere.bettertext.latex

import me.rerere.bettertext.latex.layout.Box
import me.rerere.bettertext.latex.layout.FragmentBox
import me.rerere.bettertext.latex.layout.Options
import me.rerere.bettertext.latex.layout.SpanBox
import me.rerere.bettertext.latex.layout.Style
import me.rerere.bettertext.latex.layout.SymbolBox
import me.rerere.bettertext.latex.layout.VListBox
import me.rerere.bettertext.latex.layout.buildHTML
import me.rerere.bettertext.latex.parse.parseTree
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Operators, classes, fonts, text, styles, spacing, phantoms and macros. */
class MiscTest {
    private fun layout(latex: String, display: Boolean = false): LatexLayout {
        val l = LatexLayout.create(latex, LatexSettings(displayMode = display))
        assertNull("$latex: ${l.error?.message}", l.error)
        assertTrue("$latex has zero width", l.width > 0f || latex.contains("phantom"))
        return l
    }

    /** Builds [latex] and returns the flattened top-level boxes. */
    private fun boxes(latex: String, display: Boolean = false): List<Box> {
        val settings = LatexSettings(displayMode = display)
        val tree = parseTree(latex, settings)
        val options = Options(style = if (display) Style.DISPLAY else Style.TEXT)
        return buildHTML(tree, options).chunks.filterNotNull().flatMap { it.children }
    }

    private fun symbols(box: Box): List<SymbolBox> = when (box) {
        is SymbolBox -> listOf(box)
        is SpanBox -> box.children.flatMap { symbols(it) }
        is VListBox -> box.items.flatMap { symbols(it.box) }
        else -> emptyList()
    }

    private fun dump(box: Box, indent: String = ""): String = buildString {
        append(indent).append(box::class.simpleName).append(box.classes)
        append(" w=%.3f h=%.3f d=%.3f".format(box.width, box.height, box.depth))
        if (box is SymbolBox) append(" '").append(box.text).append("' ").append(box.font)
        append('\n')
        when (box) {
            is SpanBox -> box.children.forEach { append(dump(it, "$indent  ")) }
            is VListBox -> box.items.forEach { append(dump(it.box, "$indent  ")) }
            else -> {}
        }
    }

    /** Top-level boxes without explicit/implicit glue. */
    private fun atoms(latex: String, display: Boolean = false) = boxes(latex, display).filter { !it.hasClass("mspace") }

    private fun near(expected: Float, actual: Float, eps: Float = 1e-3f) =
        assertEquals(expected, actual, eps)

    @Test
    fun relationsFromMacros() {
        layout("x \\neq y")
        layout("a \\iff b")
        layout("a \\not= b")
        layout("a \\notin B")
    }

    @Test
    fun sumInlineHasScriptsBeside() {
        val inline = layout("\\sum_{i=1}^n i")
        val top = boxes("\\sum_{i=1}^n i")
        // Inline: the supsub is a regular span with msupsub, the sum uses Size1.
        val sum = symbols(top[0]).first()
        assertEquals("Size1-Regular", sum.font)
        assertTrue(dump(top[0]), (top[0] as SpanBox).children.any { it.hasClass("msupsub") })

        val display = layout("\\sum_{i=1}^n i", display = true)
        val dtop = boxes("\\sum_{i=1}^n i", display = true)
        assertTrue(dump(dtop[0]), dtop[0].hasClass("op-limits"))
        assertEquals("Size2-Regular", symbols(dtop[0]).first { it.text == "\u2211" }.font)
        assertTrue(display.height > inline.height)
        assertTrue(display.depth > inline.depth)

        val ds = layout("\\displaystyle\\sum_{i=1}^n")
        assertTrue(boxes("\\displaystyle\\sum_{i=1}^n")[0].hasClass("op-limits"))
        assertTrue(ds.height > inline.height)
    }

    @Test
    fun sumNumbers() {
        // Size2 ∑ is 1.05 high and 0.55 deep, already centered on the axis.
        val l = layout("\\sum", display = true)
        near(1.05f, l.height)
        near(0.55f, l.depth)
        val i = layout("\\sum")
        near(0.75f, i.height)
        near(0.25f, i.depth)
    }

    @Test
    fun integralsAndLimits() {
        layout("\\int_0^1 f(x)\\,dx")
        layout("\\int_0^1 f(x)\\,dx", display = true)
        layout("\\oint_C", display = true)
        layout("\\oiint_S \\oiiint_V", display = true)
        layout("\\iint\\iiint\\smallint", display = true)
        val lim = boxes("\\lim_{x\\to 0}", display = true)
        assertTrue(lim[0].hasClass("op-limits"))
        val limInline = boxes("\\lim_{x\\to 0}")
        assertTrue(dump(limInline[0]), !limInline[0].hasClass("op-limits"))
        layout("\\lim\\limits_{x\\to 0} x")
        layout("\\sum\\nolimits_{i} x", display = true)
    }

    @Test
    fun multipleIntegralsAreWiderThanInt() {
        // KaTeX's tables give ∬ and ∭ the width of ∫; the fonts don't.
        fun opWidth(latex: String, display: Boolean) = symbols(boxes(latex, display)[0]).single().width
        near(0.819f, opWidth("\\iint", display = false))
        near(1.166f, opWidth("\\iiint", display = false))
        near(1.084f, opWidth("\\iint", display = true))
        near(1.592f, opWidth("\\iiint", display = true))
        near(0.556f, opWidth("\\int", display = true))
    }

    @Test
    fun textOperators() {
        val top = boxes("\\sin x")
        assertEquals("mop", top[0].classes[0])
        assertEquals("sin", symbols(top[0]).joinToString("") { it.text })
        // thin space between mop and mord
        assertEquals("mspace", top[1].classes[0])
        near(3f / 18f, top[1].marginRight)

        val op = boxes("\\operatorname{tr} A")
        assertEquals("mop", op[0].classes[0])
        assertEquals(listOf("Main-Regular"), symbols(op[0]).map { it.font }.distinct())
        layout("\\operatorname*{argmax}_x f", display = true)
        assertTrue(boxes("\\operatorname*{argmax}_x f", display = true)[0].hasClass("op-limits"))
        layout("\\mathop{\\rm ab}\\limits_x")
        layout("\\argmax_x f", display = true)
    }

    @Test
    fun fonts() {
        assertEquals("AMS-Regular", symbols(boxes("\\mathbb{R}")[0]).single().font)
        assertEquals("Main-Bold", symbols(boxes("\\mathbf{v}")[0]).single().font)
        assertEquals("Math-BoldItalic", symbols(boxes("\\boldsymbol{\\alpha}")[0]).single().font)
        assertEquals("Math-BoldItalic", symbols(boxes("\\bm{x}")[0]).single().font)
        assertEquals("Caligraphic-Regular", symbols(boxes("\\mathcal{L}")[0]).single().font)
        assertEquals("Main-Regular", symbols(boxes("{\\rm d}")[0]).single().font)
        // \boldsymbol keeps the bin class of its argument
        assertEquals("mbin", atoms("a\\boldsymbol{+}b")[1].classes[0])
        layout("\\mathrm{d}x + \\mathit{x} + \\mathsf{A} + \\mathtt{t} + \\mathfrak{g} + \\mathscr{S} + \\Bbb{N}")
    }

    @Test
    fun text() {
        val t = boxes("\\text{if } x > 0")
        assertTrue(t[0].hasClass("text"))
        assertEquals("Main-Regular", symbols(t[0]).first().font)
        layout("\\text{if } x > 0")
        val cjk = layout("\\text{中文}")
        assertTrue(cjk.width >= 1.9f)
        assertEquals("Main-Bold", symbols(boxes("\\textbf{a}")[0]).single().font)
        assertEquals("Main-Italic", symbols(boxes("\\textit{a}")[0]).single().font)
        assertEquals("Main-Italic", symbols(boxes("\\emph{a}")[0]).single().font)
        assertEquals("Main-Regular", symbols(boxes("\\textit{\\emph{a}}")[0]).single().font)
        assertEquals("SansSerif-Regular", symbols(boxes("\\textsf{a}")[0]).single().font)
        assertEquals("Typewriter-Regular", symbols(boxes("\\texttt{a}")[0]).single().font)
        layout("\\text{a \$x^2\$ b}")
        layout("\\text{a \\(x\\) b}")
    }

    @Test
    fun colors() {
        val red = boxes("\\color{red}{x}")
        assertEquals(0xFFFF0000.toInt(), red[0].color)
        val blue = boxes("\\textcolor{blue}{y} z")
        assertEquals(0xFF0000FF.toInt(), blue[0].color)
        assertNull(blue.last().color)
        // \color doesn't change spacing: 2 + 3 still has bin spacing
        val c = boxes("\\color{red}2+3")
        assertTrue(c.any { it.hasClass("mbin") })
        layout("\\textcolor{#00ff00}{y}")
    }

    @Test
    fun stylesAndSizes() {
        // display style raises superscripts more (sup1 vs sup2)
        val inline = layout("x^2")
        val d = layout("\\displaystyle x^2")
        assertTrue(d.height > inline.height)
        val small = layout("x")
        val large = layout("\\Large x")
        near(small.width * 1.44f, large.width)
        val script = layout("\\scriptstyle x")
        near(small.width * 0.7f, script.width)
        layout("\\tiny a \\Huge b \\normalsize c")
    }

    @Test
    fun spacing() {
        val base = layout("ab").width
        near(base + 1f, layout("a\\quad b").width)
        near(base + 2f, layout("a\\qquad b").width)
        near(base + 1f, layout("a\\hspace{1em}b").width)
        near(base + 1f, layout("a\\kern1em b").width)
        near(base + 1f, layout("a\\mkern18mu b").width)
        near(base + 3f / 18f, layout("a\\,b").width)
        near(base - 3f / 18f, layout("a\\!b").width)
        // em is relative to the current font size
        near(1.44f, layout("\\Large a\\kern1em b").width - layout("\\Large ab").width)
    }

    @Test
    fun rule() {
        val l = layout("\\rule{1em}{2em}")
        near(1f, l.width)
        near(2f, l.height)
        near(0f, l.depth)
        val s = layout("\\rule[-1em]{1em}{2em}")
        near(1f, s.height)
        near(1f, s.depth)
    }

    @Test
    fun phantoms() {
        val x = layout("x")
        val p = LatexLayout.create("\\phantom{x}", LatexSettings())
        assertNull(p.error)
        near(x.width, p.width)
        near(x.height, p.height)
        val h = layout("\\hphantom{x}y")
        near(x.width + layout("y").width, h.width)
        val v = layout("\\vphantom{\\int}y")
        near(layout("y").width, v.width)
        assertTrue(v.height > layout("y").height)
        layout("\\smash{y}")
        near(0f, layout("\\smash[b]{y}").depth)
        near(layout("y").width, layout("\\mathllap{x}y").width)
        near(layout("y").width, layout("\\mathrlap{x}y").width)
        near(layout("y").width, layout("\\mathclap{x}y").width)
        val r = layout("\\raisebox{0.5em}{x}")
        near(x.height + 0.5f, r.height)
        layout("\\vcenter{x}")
        layout("\\mathchoice{a}{b}{c}{d}")
    }

    @Test
    fun stackedRelations() {
        val o = layout("\\overset{a}{=}")
        val eq = layout("=")
        assertTrue(o.height > eq.height)
        assertEquals("mrel", atoms("x\\overset{a}{=}y")[1].classes[0])
        assertEquals("mrel", atoms("x\\stackrel{def}{=}y")[1].classes[0])
        val u = layout("\\underset{a}{=}")
        assertTrue(u.depth > eq.depth)
        assertEquals("mord", boxes("\\overset{a}{x}")[0].classes[0])
        layout("\\mathrel{x} \\mathbin{y} \\mathopen{(} \\mathclose{)} \\mathpunct{,} \\mathinner{z} \\mathord{+}")
    }

    @Test
    fun macros() {
        assertEquals("x", symbols(boxes("\\def\\foo{x}\\foo")[0]).single().text)
        assertEquals("AMS-Regular", symbols(boxes("\\newcommand{\\RR}{\\mathbb R}\\RR")[0]).single().font)
        // Like KaTeX, \R is a built-in macro, so \newcommand refuses to redefine it.
        assertNotNull(LatexLayout.create("\\newcommand{\\R}{\\mathbb R}\\R", LatexSettings()).error)
        assertEquals("AMS-Regular", symbols(boxes("\\renewcommand{\\R}{\\mathbb R}\\R")[0]).single().font)
        layout("\\def\\f#1{#1^2}\\f{y}")
        layout("\\gdef\\g{z}\\g \\global\\def\\h{w}\\h \\edef\\e{\\g}\\e \\let\\l=\\alpha\\l")
        layout("\\def\\d#1.{[#1]}\\d abc.")
        layout("\\futurelet\\x\\relax a")
        layout("\\relax x")
        layout("\\dots + \\cdots + a\\ldots")
        layout("x \\pmod{n}")
        layout("x \\bmod n \\pod{m}")
    }

    @Test
    fun misc() {
        layout("\\verb|a b~c|")
        layout("\\hbox{a b}")
        layout("\\pmb{x}")
        layout("\\href{https://example.com}{x}")
        layout("\\url{https://example.com/~a}")
        layout("\\htmlClass{foo}{x} \\htmlData{a=1,b=2}{y}")
        layout("\\char`a \\char\"41 \\char65")
        layout("\\reflectbox{ab}")
        layout("\\copyright \\textregistered")
        assertNotNull(LatexLayout.create("a \\)", LatexSettings()).error)
    }
}

package com.example.nummia.updates

internal sealed interface ReleaseNotesBlock {
    data class Heading(val text: String) : ReleaseNotesBlock
    data class Paragraph(val text: String) : ReleaseNotesBlock
    data class Bullet(val text: String) : ReleaseNotesBlock
}

/** Convierte el Markdown habitual de las Releases en bloques legibles sin dependencias externas. no entiendo*/
internal object ReleaseNotesFormatter {
    fun parse(notes: String): List<ReleaseNotesBlock> {
        val blocks = mutableListOf<ReleaseNotesBlock>()
        val paragraph = mutableListOf<String>()

        fun flushParagraph() {
            if (paragraph.isNotEmpty()) {
                blocks += ReleaseNotesBlock.Paragraph(
                    removeInlineMarkdown(paragraph.joinToString(" "))
                )
                paragraph.clear()
            }
        }

        notes.lineSequence().forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.isEmpty() -> flushParagraph()
                trimmed.matches(Regex("""#{1,6}\s+.+""")) -> {
                    flushParagraph()
                    blocks += ReleaseNotesBlock.Heading(
                        removeInlineMarkdown(trimmed.dropWhile { it == '#' }.trim())
                    )
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    flushParagraph()
                    blocks += ReleaseNotesBlock.Bullet(
                        removeInlineMarkdown(trimmed.drop(2).trim())
                    )
                }
                else -> paragraph += trimmed
            }
        }
        flushParagraph()
        return blocks
    }

    private fun removeInlineMarkdown(text: String): String = text
        .replace(Regex("""\[([^]]+)]\(([^)]+)\)"""), "$1 ($2)")
        .replace(Regex("""\*\*(.*?)\*\*|__(.*?)__"""), "$1$2")
        .replace(Regex("""(?<!\*)\*(?!\*)(.*?)(?<!\*)\*(?!\*)"""), "$1")
        .replace(Regex("""`([^`]+)`"""), "$1")
}

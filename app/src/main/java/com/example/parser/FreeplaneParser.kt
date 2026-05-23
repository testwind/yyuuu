package com.example.parser

import android.util.Xml
import com.example.model.MindMapNode
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.io.StringReader
import java.io.StringWriter
import java.util.Stack
import java.util.UUID

object FreeplaneParser {

    /**
     * Parse an .mm (Freeplane) file structure from an InputStream.
     */
    fun parse(inputStream: InputStream): MindMapNode? {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, "UTF-8")
        return parseXml(parser)
    }

    /**
     * Parse from standard XML String.
     */
    fun parse(xmlString: String): MindMapNode? {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(xmlString))
        return parseXml(parser)
    }

    private data class NodeBuilder(
        var id: String = "",
        var text: String = "",
        var folded: Boolean = false,
        var children: MutableList<MindMapNode> = mutableListOf(),
        var color: String? = null,
        var backgroundColor: String? = null,
        var note: String? = null,
        var side: String? = null,
        var icons: MutableList<String> = mutableListOf()
    ) {
        fun build(): MindMapNode {
            return MindMapNode(
                id = id.ifEmpty { "node_${UUID.randomUUID()}" },
                text = text.ifEmpty { "Untitled Node" },
                folded = folded,
                children = children.toList(),
                color = color,
                backgroundColor = backgroundColor,
                note = note,
                side = side,
                icons = icons.toList()
            )
        }
    }

    private fun parseXml(parser: XmlPullParser): MindMapNode? {
        var eventType = parser.eventType
        val nodeStack = Stack<NodeBuilder>()
        var rootNode: MindMapNode? = null

        // Keep track of rich content reading context
        var insideRichContent = false
        var richContentType: String? = null
        val richContentBuffer = java.lang.StringBuilder()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name

            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (insideRichContent) {
                        // Gather richcontent raw tags inside body if any, or just ignore and wait for text
                        // Freeplane richcontent contains <html><body><p>Text</p></body></html>
                    } else if (tagName == "node") {
                        val id = parser.getAttributeValue(null, "ID") ?: "node_${UUID.randomUUID()}"
                        var text = parser.getAttributeValue(null, "TEXT") ?: ""
                        val foldedVal = parser.getAttributeValue(null, "FOLDED")
                        val folded = foldedVal == "true"
                        val color = parser.getAttributeValue(null, "COLOR")
                        val bgColor = parser.getAttributeValue(null, "BACKGROUND_COLOR")
                        val side = parser.getAttributeValue(null, "POSITION") // "left" or "right"

                        val builder = NodeBuilder(
                            id = id,
                            text = text,
                            folded = folded,
                            color = color,
                            backgroundColor = bgColor,
                            side = side
                        )
                        nodeStack.push(builder)
                    } else if (tagName == "richcontent") {
                        insideRichContent = true
                        richContentType = parser.getAttributeValue(null, "TYPE") // "NODE" (text) or "NOTE" (notes)
                        richContentBuffer.setLength(0)
                    } else if (tagName == "icon") {
                        val builtin = parser.getAttributeValue(null, "BUILTIN")
                        if (builtin != null && nodeStack.isNotEmpty()) {
                            nodeStack.peek().icons.add(builtin)
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideRichContent) {
                        richContentBuffer.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (tagName == "richcontent") {
                        insideRichContent = false
                        val cleanText = stripHtml(richContentBuffer.toString()).trim()
                        if (nodeStack.isNotEmpty() && cleanText.isNotEmpty()) {
                            val currentBuilder = nodeStack.peek()
                            if (richContentType == "NODE") {
                                if (currentBuilder.text.isEmpty()) {
                                    currentBuilder.text = cleanText
                                }
                            } else if (richContentType == "NOTE") {
                                currentBuilder.note = cleanText
                            }
                        }
                        richContentType = null
                    } else if (tagName == "node") {
                        if (nodeStack.isNotEmpty()) {
                            val finishedBuilder = nodeStack.pop()
                            val node = finishedBuilder.build()

                            if (nodeStack.isEmpty()) {
                                rootNode = node
                            } else {
                                // Add to parent's children
                                val parent = nodeStack.peek()
                                // Propagate side down if parent has a side defined (optional layout aid)
                                val resolvedNode = if (node.side == null && parent.side != null) {
                                    node.copy(side = parent.side)
                                } else {
                                    node
                                }
                                parent.children.add(resolvedNode)
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return rootNode
    }

    /**
     * Strips HTML tags for clean text parsing
     */
    private fun stripHtml(html: String): String {
        return html.replace(Regex("<[^>]*>"), "")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&nbsp;", " ")
    }

    /**
     * Converts a MindMapNode back into Freeplane .mm XML format for exporting.
     */
    fun toXml(root: MindMapNode): String {
        val writer = StringWriter()
        writer.write("<map version=\"freeplane 1.9.12\">\n")
        writeNodeXml(root, writer, 1)
        writer.write("</map>")
        return writer.toString()
    }

    private fun writeNodeXml(node: MindMapNode, writer: StringWriter, depth: Int) {
        val indent = "  ".repeat(depth)
        val textEscaped = escapeXml(node.text)
        val foldedStr = if (node.folded) " FOLDED=\"true\"" else ""
        val colorStr = if (node.color != null) " COLOR=\"${node.color}\"" else ""
        val sideStr = if (node.side != null) " POSITION=\"${node.side}\"" else ""

        if (node.note == null && node.icons.isEmpty() && node.children.isEmpty()) {
            // Self-closing node
            writer.write("$indent<node TEXT=\"$textEscaped\" ID=\"${node.id}\"$foldedStr$colorStr$sideStr/>\n")
        } else {
            writer.write("$indent<node TEXT=\"$textEscaped\" ID=\"${node.id}\"$foldedStr$colorStr$sideStr>\n")
            
            // Note as secondary richcontent
            if (node.note != null) {
                val noteEscaped = escapeXml(node.note)
                writer.write("$indent  <richcontent TYPE=\"NOTE\">\n")
                writer.write("$indent    <html><head/><body><p>$noteEscaped</p></body></html>\n")
                writer.write("$indent  </richcontent>\n")
            }

            // Icons
            for (icon in node.icons) {
                writer.write("$indent  <icon BUILTIN=\"$icon\"/>\n")
            }

            // Children Recursive
            for (child in node.children) {
                writeNodeXml(child, writer, depth + 1)
            }

            writer.write("$indent</node>\n")
        }
    }

    private fun escapeXml(input: String): String {
        return input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}

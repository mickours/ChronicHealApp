package org.chronicheal.app.presentation.util

import android.content.Context
import android.graphics.Matrix
import android.graphics.Path
import android.util.Xml
import androidx.core.graphics.PathParser
import org.xmlpull.v1.XmlPullParser
import java.util.Stack

data class SvgPath(val id: String, val path: Path)

class SvgBodyParser(private val context: Context) {
    fun parse(fileName: String): List<SvgPath> {
        val result = mutableListOf<SvgPath>()
        try {
            val inputStream = context.assets.open(fileName)
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)

            val matrixStack = Stack<Matrix>()
            matrixStack.push(Matrix())

            val idStack = Stack<String?>()
            idStack.push(null)

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val tagName = parser.name
                        val id = parser.getAttributeValue(null, "id")
                        val transform = parser.getAttributeValue(null, "transform")

                        if (tagName == "g" || tagName == "svg") {
                            val parentMatrix = matrixStack.peek()
                            val currentMatrix = Matrix(parentMatrix)
                            if (transform != null) {
                                applyTransform(currentMatrix, transform)
                            }
                            matrixStack.push(currentMatrix)
                            idStack.push(id ?: idStack.peek())
                        } else if (tagName == "path" || tagName == "ellipse") {
                            val parentMatrix = matrixStack.peek()
                            val currentMatrix = Matrix(parentMatrix)
                            if (transform != null) {
                                applyTransform(currentMatrix, transform)
                            }
                            
                            val partId = id ?: idStack.peek() ?: "unknown"
                            
                            if (tagName == "path") {
                                val d = parser.getAttributeValue(null, "d")
                                if (d != null) {
                                    try {
                                        val path = PathParser.createPathFromPathData(d)
                                        path.transform(currentMatrix)
                                        result.add(SvgPath(partId, path))
                                    } catch (e: Exception) {
                                        // Ignore malformed paths
                                    }
                                }
                            } else if (tagName == "ellipse") {
                                val cx = parser.getAttributeValue(null, "cx")?.toFloatOrNull() ?: 0f
                                val cy = parser.getAttributeValue(null, "cy")?.toFloatOrNull() ?: 0f
                                val rx = parser.getAttributeValue(null, "rx")?.toFloatOrNull() ?: 0f
                                val ry = parser.getAttributeValue(null, "ry")?.toFloatOrNull() ?: 0f
                                val path = Path()
                                path.addOval(cx - rx, cy - ry, cx + rx, cy + ry, Path.Direction.CW)
                                path.transform(currentMatrix)
                                result.add(SvgPath(partId, path))
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val tagName = parser.name
                        if (tagName == "g" || tagName == "svg") {
                            if (matrixStack.size > 1) {
                                matrixStack.pop()
                                idStack.pop()
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
            inputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    private fun applyTransform(matrix: Matrix, transform: String) {
        val transformRegex = Regex("""(\w+)\s*\(([^)]+)\)""")
        val matches = transformRegex.findAll(transform)

        for (match in matches) {
            val type = match.groupValues[1]
            val argsStr = match.groupValues[2]
            val args = argsStr.split(Regex("""[\s,]+""")).filter { it.isNotBlank() }.mapNotNull { it.toFloatOrNull() }

            when (type) {
                "translate" -> {
                    val dx = args.getOrNull(0) ?: 0f
                    val dy = args.getOrNull(1) ?: 0f
                    matrix.preTranslate(dx, dy)
                }
                "matrix" -> {
                    if (args.size >= 6) {
                        val m = floatArrayOf(
                            args[0], args[2], args[4], // a, c, e
                            args[1], args[3], args[5], // b, d, f
                            0f, 0f, 1f
                        )
                        val matrixM = Matrix()
                        matrixM.setValues(m)
                        matrix.preConcat(matrixM)
                    }
                }
                "scale" -> {
                    val sx = args.getOrNull(0) ?: 1f
                    val sy = args.getOrNull(1) ?: sx
                    matrix.preScale(sx, sy)
                }
                "rotate" -> {
                    val angle = args.getOrNull(0) ?: 0f
                    val px = args.getOrNull(1) ?: 0f
                    val py = args.getOrNull(2) ?: 0f
                    matrix.preRotate(angle, px, py)
                }
            }
        }
    }
}

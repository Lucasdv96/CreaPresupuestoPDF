package com.example.myapplication.data.service

import com.example.myapplication.data.db.entity.BudgetItemEntity
import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.Rectangle
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject
import com.itextpdf.layout.element.Image
import kotlin.math.min

class TechnicalDiagramDrawer {

    companion object {
        const val CANVAS_WIDTH = 220f
        const val CANVAS_HEIGHT = 170f
        private const val MARGIN_LEFT = 32f
        private const val MARGIN_BOTTOM = 28f
        private const val MARGIN_TOP = 10f
        private const val MARGIN_RIGHT = 10f
        private val DRAW_AREA_W = CANVAS_WIDTH - MARGIN_LEFT - MARGIN_RIGHT
        private val DRAW_AREA_H = CANVAS_HEIGHT - MARGIN_BOTTOM - MARGIN_TOP
    }

    fun createDiagram(
        item: BudgetItemEntity,
        pdfDocument: PdfDocument,
        logoImageData: com.itextpdf.io.image.ImageData? = null
    ): Image? {
        if (item.widthMm <= 0 || item.heightMm <= 0) return null
        val supportedTypes = listOf(
            "WINDOW", "DOOR", "RAILING",
            "FENCE", "FENCE_DOOR", "GATE", "STAIR", "GRILL", "GRILL_FRONT",
            "UNDER_COUNTER", "INDUSTRIAL_FURNITURE", "TRAILER", "STORAGE"
        )
        if (item.type !in supportedTypes) return null

        val xObject = PdfFormXObject(Rectangle(CANVAS_WIDTH, CANVAS_HEIGHT))
        val canvas = PdfCanvas(xObject, pdfDocument)
        val font = PdfFontFactory.createFont(StandardFonts.HELVETICA)

        if (logoImageData != null) {
            try {
                val logoW = CANVAS_WIDTH * 0.65f
                val logoH = logoImageData.height.toFloat() * (logoW / logoImageData.width.toFloat())
                val lx = (CANVAS_WIDTH - logoW) / 2f
                val ly = (CANVAS_HEIGHT - logoH) / 2f
                val gs = com.itextpdf.kernel.pdf.extgstate.PdfExtGState()
                    .setFillOpacity(0.08f).setStrokeOpacity(0.08f)
                canvas.saveState()
                canvas.setExtGState(gs)
                canvas.addImageFittedIntoRectangle(
                    logoImageData,
                    com.itextpdf.kernel.geom.Rectangle(lx, ly, logoW, logoH),
                    false
                )
                canvas.restoreState()
            } catch (_: Exception) { }
        }

        val scale = min(DRAW_AREA_W / item.widthMm, DRAW_AREA_H / item.heightMm)
        val drawW = item.widthMm * scale
        val drawH = item.heightMm * scale

        // Center element within the drawing area
        val originX = MARGIN_LEFT + (DRAW_AREA_W - drawW) / 2f
        val originY = MARGIN_BOTTOM + (DRAW_AREA_H - drawH) / 2f

        when (item.type) {
            "WINDOW"               -> drawWindow(canvas, originX, originY, drawW, drawH, item.panelCount.coerceAtLeast(1), item.panelTypes)
            "DOOR"                 -> drawDoor(canvas, originX, originY, drawW, drawH, item.panelCount.coerceAtLeast(1))
            "RAILING"              -> drawRailing(canvas, originX, originY, drawW, drawH)
            "FENCE"                -> drawFence(canvas, originX, originY, drawW, drawH)
            "FENCE_DOOR"           -> drawFenceDoor(canvas, originX, originY, drawW, drawH)
            "GATE"                 -> drawGate(canvas, originX, originY, drawW, drawH)
            "STAIR"                -> drawStair(canvas, originX, originY, drawW, drawH)
            "GRILL"                -> drawGrill(canvas, originX, originY, drawW, drawH)
            "GRILL_FRONT"          -> drawGrillFront(canvas, originX, originY, drawW, drawH)
            "UNDER_COUNTER"        -> drawUnderCounter(canvas, originX, originY, drawW, drawH)
            "INDUSTRIAL_FURNITURE" -> drawIndustrialFurniture(canvas, originX, originY, drawW, drawH)
            else                   -> drawGenericItem(canvas, originX, originY, drawW, drawH)
        }

        drawDimensions(canvas, font, originX, originY, drawW, drawH, item.widthMm, item.heightMm)
        canvas.release()

        return Image(xObject)
    }

    // ── WINDOW ────────────────────────────────────────────────────────────────

    private fun drawWindow(
        canvas: PdfCanvas, x: Float, y: Float, w: Float, h: Float,
        panels: Int, panelTypes: String = ""
    ) {
        val inset = 3f
        val typeList = panelTypes.split(",")

        // Outer frame
        canvas.setLineWidth(2f)
        canvas.rectangle(x.toDouble(), y.toDouble(), w.toDouble(), h.toDouble())
        canvas.stroke()

        // Inner frame
        canvas.setLineWidth(0.8f)
        canvas.rectangle(
            (x + inset).toDouble(), (y + inset).toDouble(),
            (w - inset * 2).toDouble(), (h - inset * 2).toDouble()
        )
        canvas.stroke()

        // Panel dividers
        val panelW = w / panels
        for (i in 1 until panels) {
            val divX = x + panelW * i
            canvas.setLineWidth(0.8f)
            canvas.moveTo(divX.toDouble(), (y + inset).toDouble())
            canvas.lineTo(divX.toDouble(), (y + h - inset).toDouble())
            canvas.stroke()
        }

        // Per-panel indicator: F = cross (✕), M = sliding arrow
        canvas.setLineWidth(0.5f)
        for (i in 0 until panels) {
            val px = x + panelW * i
            val isFijo = typeList.getOrElse(i) { "M" } == "F"
            if (isFijo) {
                drawFixedCross(canvas, px + inset, y + inset, panelW - inset * 2, h - inset * 2)
            } else {
                val panelCenterX = px + panelW / 2f
                val arrowY = y + h / 2f
                val dir = if (i % 2 == 0) 1f else -1f
                drawSlidingArrow(canvas, panelCenterX, arrowY, panelW * 0.3f, dir)
            }
        }
    }

    private fun drawFixedCross(canvas: PdfCanvas, x: Float, y: Float, w: Float, h: Float) {
        val margin = minOf(w, h) * 0.15f
        canvas.setLineWidth(0.6f)
        canvas.moveTo((x + margin).toDouble(), (y + margin).toDouble())
        canvas.lineTo((x + w - margin).toDouble(), (y + h - margin).toDouble())
        canvas.stroke()
        canvas.moveTo((x + w - margin).toDouble(), (y + margin).toDouble())
        canvas.lineTo((x + margin).toDouble(), (y + h - margin).toDouble())
        canvas.stroke()
    }

    private fun drawSlidingArrow(canvas: PdfCanvas, cx: Float, cy: Float, half: Float, dir: Float) {
        val startX = cx - half * dir
        val endX   = cx + half * dir
        val tip    = 3.5f

        canvas.moveTo(startX.toDouble(), cy.toDouble())
        canvas.lineTo(endX.toDouble(), cy.toDouble())
        canvas.stroke()

        // Filled arrowhead
        canvas.moveTo(endX.toDouble(), cy.toDouble())
        canvas.lineTo((endX - tip * dir).toDouble(), (cy + tip * 0.5f).toDouble())
        canvas.lineTo((endX - tip * dir).toDouble(), (cy - tip * 0.5f).toDouble())
        canvas.closePath()
        canvas.fill()
    }

    // ── DOOR ──────────────────────────────────────────────────────────────────

    private fun drawDoor(canvas: PdfCanvas, x: Float, y: Float, w: Float, h: Float, panels: Int) {
        val inset = 3f

        // Outer frame
        canvas.setLineWidth(2f)
        canvas.rectangle(x.toDouble(), y.toDouble(), w.toDouble(), h.toDouble())
        canvas.stroke()

        canvas.setLineWidth(0.8f)

        if (panels == 1) {
            // Door leaf (vertical line at left edge = closed)
            canvas.moveTo((x + inset).toDouble(), (y + inset).toDouble())
            canvas.lineTo((x + inset).toDouble(), (y + h - inset).toDouble())
            canvas.stroke()

            // Swing arc — radius equals door width
            val r = (w - inset * 2f).toDouble()
            canvas.arc(
                (x + inset).toDouble(), (y + inset).toDouble(),
                (x + inset + r * 2).toDouble(), (y + inset + r * 2).toDouble(),
                0.0, 90.0
            )
            canvas.stroke()
        } else {
            // Centre divider
            val midX = x + w / 2f
            canvas.moveTo(midX.toDouble(), (y + inset).toDouble())
            canvas.lineTo(midX.toDouble(), (y + h - inset).toDouble())
            canvas.stroke()

            // Left leaf swing arc
            val rLeft = ((w / 2f) - inset).toDouble()
            canvas.arc(
                (x + inset).toDouble(), (y + inset).toDouble(),
                (x + inset + rLeft * 2).toDouble(), (y + inset + rLeft * 2).toDouble(),
                0.0, 90.0
            )
            canvas.stroke()

            // Right leaf swing arc (mirrors left)
            canvas.arc(
                (midX).toDouble(), (y + inset).toDouble(),
                (midX + rLeft * 2).toDouble(), (y + inset + rLeft * 2).toDouble(),
                90.0, 90.0
            )
            canvas.stroke()
        }
    }

    // ── RAILING ───────────────────────────────────────────────────────────────

    private fun drawRailing(canvas: PdfCanvas, x: Float, y: Float, w: Float, h: Float) {
        val postW  = (w * 0.05f).coerceAtLeast(4f).coerceAtMost(9f)
        val railH  = (h * 0.14f).coerceAtLeast(5f).coerceAtMost(11f)
        val balW   = 2.5f
        val gray   = com.itextpdf.kernel.colors.DeviceGray(0.22f)

        canvas.setLineWidth(0.5f)
        canvas.setFillColor(gray)

        // Left post
        canvas.rectangle(x.toDouble(), y.toDouble(), postW.toDouble(), h.toDouble())
        canvas.fillStroke()
        // Right post
        canvas.rectangle((x + w - postW).toDouble(), y.toDouble(), postW.toDouble(), h.toDouble())
        canvas.fillStroke()
        // Top handrail
        canvas.rectangle(x.toDouble(), (y + h - railH).toDouble(), w.toDouble(), railH.toDouble())
        canvas.fillStroke()

        // Balusters — thin hollow rectangles evenly spaced between posts
        val innerX = x + postW
        val innerW = w - postW * 2f
        val innerY = y
        val innerH = h - railH
        val count  = (innerW / 10f).toInt().coerceAtLeast(1)

        canvas.setFillColor(com.itextpdf.kernel.colors.DeviceGray(0f))
        canvas.setLineWidth(0.4f)
        for (i in 1..count) {
            val bx = innerX + innerW * i.toFloat() / (count + 1).toFloat() - balW / 2f
            canvas.rectangle(bx.toDouble(), innerY.toDouble(), balW.toDouble(), innerH.toDouble())
            canvas.stroke()
        }

        // Reset fill to black
        canvas.setFillColor(com.itextpdf.kernel.colors.DeviceGray(0f))
    }

    // ── FENCE (Reja) ──────────────────────────────────────────────────────────

    private fun drawFence(canvas: PdfCanvas, x: Float, y: Float, w: Float, h: Float) {
        val railH = (h * 0.08f).coerceAtLeast(4f).coerceAtMost(7f)
        val barW  = 2.5f
        val count = (w / 8f).toInt().coerceAtLeast(3)

        canvas.setLineWidth(1.5f)
        // Top rail
        canvas.rectangle(x.toDouble(), (y + h - railH).toDouble(), w.toDouble(), railH.toDouble())
        canvas.stroke()
        // Bottom rail
        canvas.rectangle(x.toDouble(), y.toDouble(), w.toDouble(), railH.toDouble())
        canvas.stroke()

        // Vertical bars
        canvas.setLineWidth(0.8f)
        for (i in 0..count) {
            val bx = x + w * i.toFloat() / count.toFloat() - barW / 2f
            canvas.rectangle(bx.toDouble(), y.toDouble(), barW.toDouble(), h.toDouble())
            canvas.stroke()
        }
    }

    // ── FENCE DOOR (Puerta Reja) ──────────────────────────────────────────────

    private fun drawFenceDoor(canvas: PdfCanvas, x: Float, y: Float, w: Float, h: Float) {
        drawFence(canvas, x, y, w, h)
        // Swing arc over the fence
        canvas.setLineWidth(0.8f)
        val r = (w * 0.6f).toDouble()
        canvas.arc(x.toDouble(), y.toDouble(), (x + r * 2).toDouble(), (y + r * 2).toDouble(), 0.0, 90.0)
        canvas.stroke()
    }

    // ── GATE (Portón) ─────────────────────────────────────────────────────────

    private fun drawGate(canvas: PdfCanvas, x: Float, y: Float, w: Float, h: Float) {
        val midX = x + w / 2f
        // Left panel fence
        drawFence(canvas, x, y, w / 2f - 1f, h)
        // Right panel fence
        drawFence(canvas, midX + 1f, y, w / 2f - 1f, h)
        // Center post
        canvas.setLineWidth(2f)
        canvas.moveTo(midX.toDouble(), y.toDouble())
        canvas.lineTo(midX.toDouble(), (y + h).toDouble())
        canvas.stroke()
    }

    // ── STAIR (Escalera) ──────────────────────────────────────────────────────

    private fun drawStair(canvas: PdfCanvas, x: Float, y: Float, w: Float, h: Float) {
        val steps = 5
        val stepW = w / steps
        val stepH = h / steps
        canvas.setLineWidth(1.2f)
        // Outer frame
        canvas.rectangle(x.toDouble(), y.toDouble(), w.toDouble(), h.toDouble())
        canvas.stroke()
        // Steps (side view)
        canvas.setLineWidth(0.8f)
        for (i in 1 until steps) {
            val sx = x + stepW * i
            val sy = y + stepH * i
            // Horizontal tread
            canvas.moveTo((sx - stepW).toDouble(), sy.toDouble())
            canvas.lineTo(sx.toDouble(), sy.toDouble())
            canvas.stroke()
            // Vertical riser
            canvas.moveTo(sx.toDouble(), (sy - stepH).toDouble())
            canvas.lineTo(sx.toDouble(), sy.toDouble())
            canvas.stroke()
        }
    }

    // ── GRILL (Parrilla) ──────────────────────────────────────────────────────

    private fun drawGrill(canvas: PdfCanvas, x: Float, y: Float, w: Float, h: Float) {
        canvas.setLineWidth(1.5f)
        canvas.rectangle(x.toDouble(), y.toDouble(), w.toDouble(), h.toDouble())
        canvas.stroke()
        // Horizontal grill bars
        val rows = 6
        canvas.setLineWidth(1.2f)
        for (i in 1 until rows) {
            val gy = y + h * i.toFloat() / rows.toFloat()
            canvas.moveTo((x + 4f).toDouble(), gy.toDouble())
            canvas.lineTo((x + w - 4f).toDouble(), gy.toDouble())
            canvas.stroke()
        }
        // Vertical supports
        canvas.setLineWidth(0.6f)
        for (i in listOf(0.25f, 0.5f, 0.75f)) {
            val gx = x + w * i
            canvas.moveTo(gx.toDouble(), y.toDouble())
            canvas.lineTo(gx.toDouble(), (y + h).toDouble())
            canvas.stroke()
        }
    }

    // ── GRILL FRONT (Frente de Parrilla) ──────────────────────────────────────

    private fun drawGrillFront(canvas: PdfCanvas, x: Float, y: Float, w: Float, h: Float) {
        val shelfH = h * 0.15f
        canvas.setLineWidth(1.5f)
        canvas.rectangle(x.toDouble(), y.toDouble(), w.toDouble(), h.toDouble())
        canvas.stroke()
        // Horizontal shelves
        canvas.setLineWidth(1f)
        for (i in 1..4) {
            val sy = y + h * i.toFloat() / 5f
            canvas.moveTo((x + 2f).toDouble(), sy.toDouble())
            canvas.lineTo((x + w - 2f).toDouble(), sy.toDouble())
            canvas.stroke()
        }
        // Bottom base
        canvas.setLineWidth(2f)
        canvas.rectangle(x.toDouble(), y.toDouble(), w.toDouble(), shelfH.toDouble())
        canvas.stroke()
    }

    // ── UNDER COUNTER (Bajo Mesada) ───────────────────────────────────────────

    private fun drawUnderCounter(canvas: PdfCanvas, x: Float, y: Float, w: Float, h: Float) {
        val counterH = h * 0.12f
        canvas.setLineWidth(2f)
        // Counter top
        canvas.rectangle(x.toDouble(), (y + h - counterH).toDouble(), w.toDouble(), counterH.toDouble())
        canvas.stroke()
        // Cabinet body
        canvas.setLineWidth(1.2f)
        canvas.rectangle(x.toDouble(), y.toDouble(), w.toDouble(), (h - counterH).toDouble())
        canvas.stroke()
        // Door split
        canvas.setLineWidth(0.6f)
        val midX = x + w / 2f
        canvas.moveTo(midX.toDouble(), y.toDouble())
        canvas.lineTo(midX.toDouble(), (y + h - counterH).toDouble())
        canvas.stroke()
        // Handles
        for (side in listOf(0.35f, 0.65f)) {
            val hx = x + w * side
            val hy = y + (h - counterH) / 2f
            canvas.circle(hx.toDouble(), hy.toDouble(), 2.5)
            canvas.stroke()
        }
    }

    // ── INDUSTRIAL FURNITURE (Mueble Industrial) ──────────────────────────────

    private fun drawIndustrialFurniture(canvas: PdfCanvas, x: Float, y: Float, w: Float, h: Float) {
        canvas.setLineWidth(1.5f)
        canvas.rectangle(x.toDouble(), y.toDouble(), w.toDouble(), h.toDouble())
        canvas.stroke()
        // Shelves
        canvas.setLineWidth(1f)
        val shelves = 4
        for (i in 1 until shelves) {
            val sy = y + h * i.toFloat() / shelves.toFloat()
            canvas.moveTo((x + 2f).toDouble(), sy.toDouble())
            canvas.lineTo((x + w - 2f).toDouble(), sy.toDouble())
            canvas.stroke()
        }
        // Side columns
        canvas.setLineWidth(2f)
        canvas.moveTo((x + 3f).toDouble(), y.toDouble())
        canvas.lineTo((x + 3f).toDouble(), (y + h).toDouble())
        canvas.stroke()
        canvas.moveTo((x + w - 3f).toDouble(), y.toDouble())
        canvas.lineTo((x + w - 3f).toDouble(), (y + h).toDouble())
        canvas.stroke()
    }

    // ── GENERIC (Trailer, Baulera, etc.) ──────────────────────────────────────

    private fun drawGenericItem(canvas: PdfCanvas, x: Float, y: Float, w: Float, h: Float) {
        canvas.setLineWidth(2f)
        canvas.rectangle(x.toDouble(), y.toDouble(), w.toDouble(), h.toDouble())
        canvas.stroke()
        // Diagonal cross to indicate a generic item
        canvas.setLineWidth(0.4f)
        canvas.moveTo(x.toDouble(), y.toDouble())
        canvas.lineTo((x + w).toDouble(), (y + h).toDouble())
        canvas.stroke()
        canvas.moveTo((x + w).toDouble(), y.toDouble())
        canvas.lineTo(x.toDouble(), (y + h).toDouble())
        canvas.stroke()
    }

    // ── DIMENSION LINES ───────────────────────────────────────────────────────

    private fun drawDimensions(
        canvas: PdfCanvas,
        font: com.itextpdf.kernel.font.PdfFont,
        x: Float, y: Float,
        drawW: Float, drawH: Float,
        widthMm: Int, heightMm: Int
    ) {
        canvas.setLineWidth(0.4f)
        val tick = 4f
        val fontSize = 7f

        // Width dimension (bottom)
        val dimY = y - 14f
        canvas.moveTo(x.toDouble(), dimY.toDouble())
        canvas.lineTo((x + drawW).toDouble(), dimY.toDouble())
        canvas.stroke()
        canvas.moveTo(x.toDouble(), (dimY - tick).toDouble()); canvas.lineTo(x.toDouble(), (dimY + tick).toDouble()); canvas.stroke()
        canvas.moveTo((x + drawW).toDouble(), (dimY - tick).toDouble()); canvas.lineTo((x + drawW).toDouble(), (dimY + tick).toDouble()); canvas.stroke()

        val wText = "${widthMm}mm"
        val wTextW = font.getWidth(wText, fontSize)
        canvas.beginText()
        canvas.setFontAndSize(font, fontSize)
        canvas.moveText((x + drawW / 2 - wTextW / 2).toDouble(), (dimY - 9).toDouble())
        canvas.showText(wText)
        canvas.endText()

        // Height dimension (left, rotated 90°)
        val dimX = x - 16f
        canvas.moveTo(dimX.toDouble(), y.toDouble())
        canvas.lineTo(dimX.toDouble(), (y + drawH).toDouble())
        canvas.stroke()
        canvas.moveTo((dimX - tick).toDouble(), y.toDouble()); canvas.lineTo((dimX + tick).toDouble(), y.toDouble()); canvas.stroke()
        canvas.moveTo((dimX - tick).toDouble(), (y + drawH).toDouble()); canvas.lineTo((dimX + tick).toDouble(), (y + drawH).toDouble()); canvas.stroke()

        val hText = "${heightMm}mm"
        val hTextW = font.getWidth(hText, fontSize)
        canvas.beginText()
        canvas.setFontAndSize(font, fontSize)
        canvas.setTextMatrix(0f, 1f, -1f, 0f, dimX - 8f, y + drawH / 2 - hTextW / 2)
        canvas.showText(hText)
        canvas.endText()
    }
}

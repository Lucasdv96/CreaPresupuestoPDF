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
        canvas.setFillColor(com.itextpdf.kernel.colors.DeviceGray(0f))

        val handrailH  = (h * 0.10f).coerceIn(4f, 9f)   // grosor del pasamanos superior
        val handrailY  = y + h - handrailH               // base del pasamanos
        val bottomRailY = y + h * 0.10f                  // riel inferior
        val plateW     = 8f
        val plateH     = 2.5f

        // Pasamanos superior (barra horizontal gruesa)
        canvas.setLineWidth(1f)
        canvas.rectangle(x.toDouble(), handrailY.toDouble(), w.toDouble(), handrailH.toDouble())
        canvas.fillStroke()

        // Rieles horizontales (inferior + intermedio)
        canvas.setLineWidth(2f)
        val midRailY = (bottomRailY + handrailY) / 2f
        for (railY in listOf(bottomRailY, midRailY)) {
            canvas.moveTo(x.toDouble(), railY.toDouble())
            canvas.lineTo((x + w).toDouble(), railY.toDouble())
            canvas.stroke()
        }

        // Postes verticales gruesos (extremos + intermedios) con base al piso
        val postCount = (w / 70f).toInt().coerceIn(2, 4)
        canvas.setLineWidth(2.4f)
        for (i in 0..postCount) {
            val px = x + w * i.toFloat() / postCount.toFloat()
            val cx = px.coerceIn(x + 1.5f, x + w - 1.5f)
            canvas.moveTo(cx.toDouble(), y.toDouble())
            canvas.lineTo(cx.toDouble(), (y + h).toDouble())
            canvas.stroke()
            // Base (placa de anclaje al piso)
            canvas.rectangle((cx - plateW / 2f).toDouble(), y.toDouble(), plateW.toDouble(), plateH.toDouble())
            canvas.fill()
        }

        canvas.setFillColor(com.itextpdf.kernel.colors.DeviceGray(0f))
    }

    // ── FENCE (Reja) ──────────────────────────────────────────────────────────

    private fun drawFence(canvas: PdfCanvas, x: Float, y: Float, w: Float, h: Float) {
        val barCount = (w / 7f).toInt().coerceAtLeast(4)
        val railY1   = y + h * 0.22f   // rail inferior (no está en el borde)
        val railY2   = y + h * 0.78f   // rail superior

        // Barras finas verticales que sobresalen por encima y debajo de los rieles
        canvas.setLineWidth(1.2f)
        for (i in 0 until barCount) {
            val bx = x + w * (i + 0.5f) / barCount.toFloat()
            canvas.moveTo(bx.toDouble(), y.toDouble())
            canvas.lineTo(bx.toDouble(), (y + h).toDouble())
            canvas.stroke()
        }

        // Rieles horizontales gruesos encima de las barras (hacia adentro)
        canvas.setLineWidth(3.5f)
        canvas.moveTo(x.toDouble(), railY1.toDouble())
        canvas.lineTo((x + w).toDouble(), railY1.toDouble())
        canvas.stroke()
        canvas.moveTo(x.toDouble(), railY2.toDouble())
        canvas.lineTo((x + w).toDouble(), railY2.toDouble())
        canvas.stroke()
    }

    // ── FENCE DOOR (Puerta Reja) ──────────────────────────────────────────────

    private fun drawFenceDoor(canvas: PdfCanvas, x: Float, y: Float, w: Float, h: Float) {
        // Patrón de reja al interior (sin los posts laterales para no superponerse)
        drawFence(canvas, x, y, w, h)

        // Posts laterales gruesos (marco de la puerta reja)
        canvas.setLineWidth(5f)
        canvas.moveTo(x.toDouble(), y.toDouble())
        canvas.lineTo(x.toDouble(), (y + h).toDouble())
        canvas.stroke()
        canvas.moveTo((x + w).toDouble(), y.toDouble())
        canvas.lineTo((x + w).toDouble(), (y + h).toDouble())
        canvas.stroke()

        // Travesaños superior e inferior
        canvas.setLineWidth(3.5f)
        canvas.moveTo(x.toDouble(), y.toDouble())
        canvas.lineTo((x + w).toDouble(), y.toDouble())
        canvas.stroke()
        canvas.moveTo(x.toDouble(), (y + h).toDouble())
        canvas.lineTo((x + w).toDouble(), (y + h).toDouble())
        canvas.stroke()

        // Arco de apertura
        canvas.setLineWidth(0.8f)
        val r = (w * 0.55f).toDouble()
        canvas.arc(x.toDouble(), y.toDouble(), (x + r * 2).toDouble(), (y + r * 2).toDouble(), 0.0, 90.0)
        canvas.stroke()
    }

    // ── GATE (Portón) ─────────────────────────────────────────────────────────

    private fun drawGate(canvas: PdfCanvas, x: Float, y: Float, w: Float, h: Float) {
        val midX = x + w / 2f

        // Marco exterior grueso (líneas negras)
        canvas.setFillColor(com.itextpdf.kernel.colors.DeviceGray(0f))
        canvas.setLineWidth(2.5f)
        canvas.rectangle(x.toDouble(), y.toDouble(), w.toDouble(), h.toDouble())
        canvas.stroke()

        // Poste central (separa las dos hojas)
        canvas.setLineWidth(2.5f)
        canvas.moveTo(midX.toDouble(), y.toDouble())
        canvas.lineTo(midX.toDouble(), (y + h).toDouble())
        canvas.stroke()

        // Travesaño horizontal medio en cada hoja (refuerzo)
        val railY = y + h * 0.5f
        canvas.setLineWidth(1.6f)
        canvas.moveTo((x + 2f).toDouble(), railY.toDouble())
        canvas.lineTo((x + w - 2f).toDouble(), railY.toDouble())
        canvas.stroke()

        // Barras verticales negras dentro de cada hoja (como en la puerta)
        val inset = 3f
        val barsPerLeaf = ((w / 2f) / 9f).toInt().coerceAtLeast(3)
        canvas.setLineWidth(1.4f)
        // Hoja izquierda
        val leftW = (midX - x) - inset * 2f
        for (i in 1..barsPerLeaf) {
            val bx = x + inset + leftW * i.toFloat() / (barsPerLeaf + 1).toFloat()
            canvas.moveTo(bx.toDouble(), (y + inset).toDouble())
            canvas.lineTo(bx.toDouble(), (y + h - inset).toDouble())
            canvas.stroke()
        }
        // Hoja derecha
        val rightW = (x + w - midX) - inset * 2f
        for (i in 1..barsPerLeaf) {
            val bx = midX + inset + rightW * i.toFloat() / (barsPerLeaf + 1).toFloat()
            canvas.moveTo(bx.toDouble(), (y + inset).toDouble())
            canvas.lineTo(bx.toDouble(), (y + h - inset).toDouble())
            canvas.stroke()
        }
    }

    // ── STAIR (Escalera) ──────────────────────────────────────────────────────

    private fun drawStair(canvas: PdfCanvas, x: Float, y: Float, w: Float, h: Float) {
        canvas.setFillColor(com.itextpdf.kernel.colors.DeviceGray(0f))

        val groundY    = y
        val deckY      = y + h * 0.52f          // nivel del piso de la plataforma
        val railRise   = h * 0.26f              // altura de la baranda sobre el piso/escalón
        val railTopY   = deckY + railRise       // pasamanos de la plataforma
        val platformW  = w * 0.30f
        val platRight  = x + platformW
        val plateW     = 6f
        val plateH     = 2.5f

        // ── Plataforma (descanso) ──────────────────────────────────────────────
        // Piso/deck de la plataforma
        canvas.setLineWidth(2.5f)
        canvas.moveTo(x.toDouble(), deckY.toDouble())
        canvas.lineTo((platRight + 2f).toDouble(), deckY.toDouble())
        canvas.stroke()

        // Dos patas con base
        val legXs = listOf(x + platformW * 0.14f, x + platformW * 0.82f)
        canvas.setLineWidth(2.2f)
        for (lx in legXs) {
            canvas.moveTo(lx.toDouble(), deckY.toDouble())
            canvas.lineTo(lx.toDouble(), groundY.toDouble())
            canvas.stroke()
            // Base (placa)
            canvas.rectangle((lx - plateW / 2f).toDouble(), groundY.toDouble(), plateW.toDouble(), plateH.toDouble())
            canvas.fill()
        }

        // Baranda de la plataforma: pasamanos + postes + balaustres
        canvas.setLineWidth(2f)
        canvas.moveTo(x.toDouble(), railTopY.toDouble())
        canvas.lineTo(platRight.toDouble(), railTopY.toDouble())
        canvas.stroke()
        // Postes en los extremos
        canvas.setLineWidth(2f)
        for (px in listOf(x + 1.5f, platRight)) {
            canvas.moveTo(px.toDouble(), deckY.toDouble())
            canvas.lineTo(px.toDouble(), railTopY.toDouble())
            canvas.stroke()
        }
        // Balaustres de la plataforma
        canvas.setLineWidth(0.8f)
        val balCount = (platformW / 9f).toInt().coerceAtLeast(2)
        for (i in 1..balCount) {
            val bx = x + platformW * i.toFloat() / (balCount + 1).toFloat()
            canvas.moveTo(bx.toDouble(), deckY.toDouble())
            canvas.lineTo(bx.toDouble(), railTopY.toDouble())
            canvas.stroke()
        }

        // ── Escalera descendente ───────────────────────────────────────────────
        val steps    = ((w - platformW) / 16f).toInt().coerceIn(7, 12)
        val runW      = (x + w) - platRight
        val riseH     = deckY - groundY
        val stepRun   = runW / steps
        val stepRise  = riseH / steps

        // Perfil escalonado (huellas y contrahuellas)
        canvas.setLineWidth(1.5f)
        canvas.moveTo(platRight.toDouble(), deckY.toDouble())
        for (i in 0 until steps) {
            val sx = platRight + stepRun * i
            val sy = deckY - stepRise * i
            canvas.lineTo((sx + stepRun).toDouble(), sy.toDouble())              // huella
            canvas.lineTo((sx + stepRun).toDouble(), (sy - stepRise).toDouble()) // contrahuella
        }
        canvas.stroke()

        // Larguero inferior (viga diagonal de apoyo)
        canvas.setLineWidth(1.8f)
        canvas.moveTo(platRight.toDouble(), deckY.toDouble())
        canvas.lineTo((x + w).toDouble(), groundY.toDouble())
        canvas.stroke()

        // Baranda de la escalera: pasamanos diagonal + balaustres
        canvas.setLineWidth(2f)
        canvas.moveTo(platRight.toDouble(), (deckY + railRise).toDouble())
        canvas.lineTo((x + w).toDouble(), (groundY + railRise).toDouble())
        canvas.stroke()
        // Balaustres verticales sobre cada escalón
        canvas.setLineWidth(0.8f)
        for (i in 0..steps) {
            val bx = platRight + stepRun * i
            val byBottom = deckY - stepRise * i
            canvas.moveTo(bx.toDouble(), byBottom.toDouble())
            canvas.lineTo(bx.toDouble(), (byBottom + railRise).toDouble())
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
        // Solo las puertas del mueble (sin mesada superior)
        canvas.setFillColor(com.itextpdf.kernel.colors.DeviceGray(0f))

        // Marco exterior del frente
        canvas.setLineWidth(2f)
        canvas.rectangle(x.toDouble(), y.toDouble(), w.toDouble(), h.toDouble())
        canvas.stroke()

        // Cantidad de puertas según el ancho
        val doors = (w / 38f).toInt().coerceIn(2, 4)
        val doorW = w / doors
        val inset = 3f

        canvas.setLineWidth(1f)
        for (i in 0 until doors) {
            val dx = x + doorW * i
            // Panel de cada puerta (recuadro interior)
            canvas.rectangle(
                (dx + inset).toDouble(), (y + inset).toDouble(),
                (doorW - inset * 2f).toDouble(), (h - inset * 2f).toDouble()
            )
            canvas.stroke()

            // Manija (línea vertical corta cerca del borde interno de la puerta)
            val handleX = if (i < doors / 2) dx + doorW - inset * 2.2f else dx + inset * 2.2f
            val handleY0 = y + h * 0.42f
            val handleY1 = y + h * 0.58f
            canvas.setLineWidth(2f)
            canvas.moveTo(handleX.toDouble(), handleY0.toDouble())
            canvas.lineTo(handleX.toDouble(), handleY1.toDouble())
            canvas.stroke()
            canvas.setLineWidth(1f)
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

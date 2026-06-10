package com.example.myapplication.data.service

import android.content.Context
import com.example.myapplication.data.db.entity.BudgetEntity
import com.example.myapplication.data.db.entity.BudgetItemEntity
import com.example.myapplication.data.db.entity.SettingsEntity
import com.example.myapplication.utils.formatCurrency
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceGray
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.events.Event
import com.itextpdf.kernel.events.IEventHandler
import com.itextpdf.kernel.events.PdfDocumentEvent
import com.itextpdf.kernel.geom.Rectangle
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.kernel.pdf.extgstate.PdfExtGState
import com.itextpdf.layout.Canvas
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.itextpdf.layout.properties.VerticalAlignment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfGeneratorService(private val context: Context) {

    private val FOOTER_LINE1 = "App creada por Lucas Del Valle · Fullstack Developer  |  lucas.delvalle1996@gmail.com  |  wa.me/542267450234"
    private val FOOTER_LINE2 = "lucasdv-developer.vercel.app  |  linkedin.com/in/lucas-del-valle-740277163"

    private val diagramDrawer = TechnicalDiagramDrawer()

    fun generateBudgetPdf(
        budget: BudgetEntity,
        items: List<BudgetItemEntity>,
        client: com.example.myapplication.data.db.entity.ClientEntity?,
        settings: SettingsEntity
    ): String {
        val pdfDir = File(context.getExternalFilesDir(null), "presupuestos")
        pdfDir.mkdirs()

        val fileName = "Presupuesto_${budget.budgetNumber}_${System.currentTimeMillis()}.pdf"
        val pdfFile = File(pdfDir, fileName)

        val writer = PdfWriter(pdfFile.absolutePath)
        val pdfDocument = PdfDocument(writer)
        // Registrar el footer ANTES de agregar contenido para que se dibuje
        // de forma confiable en cada página (END_PAGE) con la fuente registrada.
        pdfDocument.addEventHandler(PdfDocumentEvent.END_PAGE, DeveloperFooterHandler())
        val document = Document(pdfDocument)
        document.setBottomMargin(62f)

        addCompanyHeader(document, settings)
        document.add(Paragraph("\n"))
        addBudgetInfo(document, budget, client)
        document.add(Paragraph("\n"))
        val itemsWithDimensions = items.filter { it.widthMm > 0 && it.heightMm > 0 }
        if (itemsWithDimensions.isNotEmpty()) {
            addTechnicalDetails(document, pdfDocument, itemsWithDimensions)
            document.add(Paragraph("\n"))
        }

        addItemsTable(document, items)

        document.add(Paragraph("\n"))
        addPriceSummary(document, items, budget)

        if (budget.notes.isNotEmpty()) {
            document.add(Paragraph("\n"))
            document.add(Paragraph("NOTAS").setBold())
            document.add(Paragraph(budget.notes))
        }

        if (settings.termsConditions.isNotEmpty()) {
            document.add(Paragraph("\n"))
            document.add(Paragraph("TÉRMINOS Y CONDICIONES").setBold())
            document.add(Paragraph(settings.termsConditions).setFontSize(10f))
        }

        document.flush()
        addWatermarks(pdfDocument, settings.logoPath)
        document.close()
        return pdfFile.absolutePath
    }

    /**
     * Dibuja el footer del desarrollador en cada página usando el evento END_PAGE.
     * Se registra antes de agregar contenido, por lo que iText invoca este handler
     * en el momento exacto en que cada página se finaliza, con los recursos de la
     * página todavía escribibles (registro de fuente correcto). Esto evita el bug
     * de footer invisible que ocurría al dibujar después de document.flush().
     */
    private inner class DeveloperFooterHandler : IEventHandler {
        override fun handleEvent(event: Event) {
            try {
                val docEvent   = event as PdfDocumentEvent
                val pdfDoc     = docEvent.document
                val page       = docEvent.page
                val pageSize   = page.pageSize
                val leftMargin = 36f
                val usableW    = pageSize.width - leftMargin * 2

                val pdfCanvas = PdfCanvas(page.newContentStreamAfter(), page.resources, pdfDoc)

                // Línea separadora
                pdfCanvas.saveState()
                    .setStrokeColor(DeviceGray(0.65f))
                    .setLineWidth(0.5f)
                    .moveTo(leftMargin.toDouble(), 50.0)
                    .lineTo((pageSize.width - leftMargin).toDouble(), 50.0)
                    .stroke()
                    .restoreState()

                // Texto via Canvas layout (registra la fuente automáticamente)
                val footerRect = Rectangle(leftMargin, 8f, usableW, 40f)
                Canvas(pdfCanvas, footerRect).use { layoutCanvas ->
                    layoutCanvas.add(
                        Paragraph("$FOOTER_LINE1\n$FOOTER_LINE2")
                            .setFontSize(7.5f)
                            .setFontColor(DeviceGray(0.40f))
                            .setTextAlignment(TextAlignment.CENTER)
                            .setMargin(0f)
                            .setMultipliedLeading(1.3f)
                    )
                }

                pdfCanvas.release()
            } catch (_: Exception) { }
        }
    }

    private fun addWatermarks(pdfDocument: PdfDocument, companyLogoPath: String) {
        try {
            // Usa el logo de la empresa configurado; si no hay, cae al recurso raw
            val imageData = if (companyLogoPath.isNotEmpty()) {
                val f = java.io.File(companyLogoPath)
                if (f.exists()) ImageDataFactory.create(f.absolutePath)
                else {
                    val resId = context.resources.getIdentifier("logo_watermark", "raw", context.packageName)
                    if (resId == 0) return
                    ImageDataFactory.create(context.resources.openRawResource(resId).use { it.readBytes() })
                }
            } else {
                val resId = context.resources.getIdentifier("logo_watermark", "raw", context.packageName)
                if (resId == 0) return
                ImageDataFactory.create(context.resources.openRawResource(resId).use { it.readBytes() })
            }

            for (i in 1..pdfDocument.numberOfPages) {
                try {
                    val page = pdfDocument.getPage(i)
                    val pageSize = page.pageSize
                    val canvas = PdfCanvas(page.newContentStreamBefore(), page.resources, pdfDocument)

                    val targetW = pageSize.width * 0.60f
                    val targetH = imageData.height.toFloat() * (targetW / imageData.width.toFloat())
                    val x = (pageSize.width - targetW) / 2f
                    val y = (pageSize.height - targetH) / 2f

                    val gs = PdfExtGState().setFillOpacity(0.12f).setStrokeOpacity(0.12f)
                    canvas.saveState()
                    canvas.setExtGState(gs)
                    canvas.addImageFittedIntoRectangle(imageData, Rectangle(x, y, targetW, targetH), false)
                    canvas.restoreState()
                    canvas.release()
                } catch (_: Exception) { }
            }
        } catch (_: Exception) { }
    }

    private fun addCompanyHeader(document: Document, settings: SettingsEntity) {
        val headerTable = Table(UnitValue.createPercentArray(floatArrayOf(45f, 55f)))
        headerTable.setWidth(UnitValue.createPercentValue(100f))

        val infoCell = Cell().setBorder(null).setVerticalAlignment(VerticalAlignment.MIDDLE)
        infoCell.add(Paragraph(settings.companyName).setBold().setFontSize(18f).setMarginBottom(4f))
        if (settings.companyCuit.isNotEmpty())   infoCell.add(Paragraph("CUIT: ${settings.companyCuit}").setFontSize(13f))
        if (settings.companyAddress.isNotEmpty()) infoCell.add(Paragraph(settings.companyAddress).setFontSize(13f))
        if (settings.companyCity.isNotEmpty())   infoCell.add(Paragraph(settings.companyCity).setFontSize(13f))
        if (settings.companyPhone.isNotEmpty())  infoCell.add(Paragraph("Teléfono: ${settings.companyPhone}").setFontSize(13f))
        if (settings.companyEmail.isNotEmpty())  infoCell.add(Paragraph("Email: ${settings.companyEmail}").setFontSize(13f))
        headerTable.addCell(infoCell)

        val logoFile = if (settings.logoPath.isNotEmpty()) File(settings.logoPath) else null
        if (logoFile != null && logoFile.exists()) {
            val imageData = ImageDataFactory.create(logoFile.absolutePath)
            val image = Image(imageData).setMaxHeight(300f).setMaxWidth(420f).setAutoScale(false)
            headerTable.addCell(
                Cell().add(image).setTextAlignment(TextAlignment.RIGHT).setBorder(null)
            )
        } else {
            headerTable.addCell(Cell().setBorder(null))
        }

        document.add(headerTable)
    }

    private fun addBudgetInfo(
        document: Document,
        budget: BudgetEntity,
        client: com.example.myapplication.data.db.entity.ClientEntity?
    ) {
        val infoTable = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f)))
        infoTable.setWidth(UnitValue.createPercentValue(100f))

        // 1. Sin estado
        val budgetInfoCell = Cell()
            .add(Paragraph("PRESUPUESTO #${budget.budgetNumber}").setBold())
            .add(Paragraph("Proyecto: ${budget.project}"))
            .add(Paragraph("Fecha: ${formatDate(budget.createdDate)}"))
            .setBorder(null)
        infoTable.addCell(budgetInfoCell)

        val clientInfoCell = Cell().add(Paragraph("CLIENTE").setBold())
        if (client != null) {
            clientInfoCell.add(Paragraph(client.name))
            if (client.cuit.isNotEmpty()) clientInfoCell.add(Paragraph("CUIT: ${client.cuit}"))
            if (client.address.isNotEmpty()) clientInfoCell.add(Paragraph(client.address))
            if (client.city.isNotEmpty()) clientInfoCell.add(
                Paragraph("${client.city}${if (client.province.isNotEmpty()) ", ${client.province}" else ""}")
            )
            if (client.phone.isNotEmpty()) clientInfoCell.add(Paragraph("Tel: ${client.phone}"))
            if (client.email.isNotEmpty()) clientInfoCell.add(Paragraph(client.email))
        }
        clientInfoCell.setBorder(null)
        infoTable.addCell(clientInfoCell)

        document.add(infoTable)
    }

    private fun addItemsTable(document: Document, items: List<BudgetItemEntity>) {
        // 2. Sin columna M.O. — columnas: Tipo | Descripción/Especificaciones/Notas | Cant | Precio | Subtotal
        val table = Table(UnitValue.createPercentArray(floatArrayOf(15f, 40f, 10f, 17f, 18f)))
        table.setWidth(UnitValue.createPercentValue(100f))

        listOf("Tipo", "Descripción", "Cant.", "Precio Unit.", "Subtotal").forEach { header ->
            table.addCell(
                Cell().add(Paragraph(header).setBold())
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
            )
        }

        items.forEach { item ->
            val itemType = itemTypeDisplayName(item.type)
            val subtotal = item.quantity * item.unitPrice

            // 5. Descripción + especificaciones + notas en la misma celda
            val descCell = Cell()
            if (item.description.isNotEmpty()) descCell.add(Paragraph(item.description))
            if (item.specifications.isNotEmpty()) descCell.add(Paragraph(item.specifications).setFontSize(9f).setItalic())
            if (item.notes.isNotEmpty()) descCell.add(Paragraph("Nota: ${item.notes}").setFontSize(9f))

            table.addCell(Cell().add(Paragraph(itemType)))
            table.addCell(descCell)
            table.addCell(Cell().add(Paragraph(item.quantity.toString())).setTextAlignment(TextAlignment.CENTER))
            table.addCell(Cell().add(Paragraph(formatCurrency(item.unitPrice))).setTextAlignment(TextAlignment.RIGHT))
            table.addCell(Cell().add(Paragraph(formatCurrency(subtotal))).setTextAlignment(TextAlignment.RIGHT))
        }

        document.add(table)
    }

    private fun addPriceSummary(document: Document, items: List<BudgetItemEntity>, budget: BudgetEntity) {
        val summaryTable = Table(UnitValue.createPercentArray(floatArrayOf(70f, 30f)))
        summaryTable.setWidth(UnitValue.createPercentValue(100f))

        val itemsTotal = items.sumOf { it.quantity * it.unitPrice }
        val grandTotal = itemsTotal + budget.laborCostPerItem

        var cell = Cell().add(Paragraph("Subtotal Items:")).setTextAlignment(TextAlignment.RIGHT).setBorder(null)
        summaryTable.addCell(cell)
        cell = Cell().add(Paragraph(formatCurrency(itemsTotal))).setTextAlignment(TextAlignment.RIGHT).setBorder(null)
        summaryTable.addCell(cell)

        // 3. Sin "Mano de obra (items)" — 4. "Mano de obra" sin "(presupuesto)"
        if (budget.laborCostPerItem > 0) {
            cell = Cell().add(Paragraph("Mano de obra:")).setTextAlignment(TextAlignment.RIGHT).setBorder(null)
            summaryTable.addCell(cell)
            cell = Cell().add(Paragraph(formatCurrency(budget.laborCostPerItem))).setTextAlignment(TextAlignment.RIGHT).setBorder(null)
            summaryTable.addCell(cell)
        }

        cell = Cell().add(Paragraph("TOTAL:").setBold().setFontSize(14f)).setTextAlignment(TextAlignment.RIGHT)
            .setBorder(SolidBorder(1f))
        summaryTable.addCell(cell)
        cell = Cell().add(Paragraph(formatCurrency(grandTotal)).setBold().setFontSize(14f)).setTextAlignment(TextAlignment.RIGHT)
            .setBorder(SolidBorder(1f))
        summaryTable.addCell(cell)

        document.add(summaryTable)
    }

    private fun addTechnicalDetails(
        document: Document,
        pdfDocument: PdfDocument,
        items: List<BudgetItemEntity>
    ) {
        val logoImageData: com.itextpdf.io.image.ImageData? = try {
            val resId = context.resources.getIdentifier("logo_watermark", "raw", context.packageName)
            if (resId != 0) {
                val bytes = context.resources.openRawResource(resId).use { it.readBytes() }
                ImageDataFactory.create(bytes)
            } else null
        } catch (_: Exception) { null }

        val headerColor = DeviceRgb(0x1a, 0x4f, 0x8a)

        document.add(
            Paragraph("DETALLE TÉCNICO")
                .setBold()
                .setFontSize(11f)
                .setFontColor(headerColor)
        )

        val typeCounters = mutableMapOf<String, Int>()

        items.forEachIndexed { _, item ->
            typeCounters[item.type] = (typeCounters[item.type] ?: 0) + 1
            val prefix = itemTypePrefix(item.type)
            val itemCode = "$prefix${typeCounters[item.type]!!.toString().padStart(2, '0')}"
            val typeLabel = itemTypeDisplayName(item.type)

            // Card outer table: full width, slight top margin
            val card = Table(UnitValue.createPercentArray(floatArrayOf(100f)))
                .setWidth(UnitValue.createPercentValue(100f))
                .setMarginTop(8f)

            // Header row: "V01 - Ventana"
            val headerCell = Cell()
                .add(Paragraph("$itemCode - $typeLabel").setBold().setFontSize(10f).setFontColor(com.itextpdf.kernel.colors.ColorConstants.WHITE))
                .setBackgroundColor(headerColor)
                .setPadding(4f)
                .setBorder(null)
            card.addCell(headerCell)

            // Content row: diagram (left) + info table (right)
            val contentTable = Table(UnitValue.createPercentArray(floatArrayOf(45f, 55f)))
                .setWidth(UnitValue.createPercentValue(100f))

            // Left cell: diagram
            val diagram = diagramDrawer.createDiagram(item, pdfDocument, logoImageData)
            val diagramCell = Cell().setBorder(SolidBorder(0.5f)).setPadding(6f)
            if (diagram != null) {
                diagram.setMaxWidth(UnitValue.createPercentValue(100f))
                diagramCell.add(diagram)
            } else {
                diagramCell.add(Paragraph("Sin dimensiones").setFontSize(9f))
            }
            contentTable.addCell(diagramCell)

            // Right cell: specs + values table
            val infoCell = Cell().setBorder(SolidBorder(0.5f)).setPadding(6f).setVerticalAlignment(VerticalAlignment.TOP)

            // Specs sub-table
            val specsTable = Table(UnitValue.createPercentArray(floatArrayOf(40f, 60f)))
                .setWidth(UnitValue.createPercentValue(100f))
                .setMarginBottom(6f)

            fun specRow(label: String, value: String) {
                specsTable.addCell(Cell().add(Paragraph(label).setBold().setFontSize(9f)).setBackgroundColor(ColorConstants.LIGHT_GRAY).setPadding(3f))
                specsTable.addCell(Cell().add(Paragraph(value).setFontSize(9f)).setPadding(3f))
            }

            specRow("Dimensiones:", "${item.widthMm}mm x ${item.heightMm}mm")
            if (item.description.isNotEmpty()) specRow("Descripción:", item.description)
            if (item.specifications.isNotEmpty()) specRow("Perfil:", item.specifications)
            if (item.panelCount > 0 && item.type in listOf("WINDOW", "DOOR", "UNDER_COUNTER")) specRow("Hojas:", item.panelCount.toString())
            if (item.notes.isNotEmpty()) specRow("Notas:", item.notes)

            infoCell.add(specsTable)

            // Values sub-table
            val valuesTable = Table(UnitValue.createPercentArray(floatArrayOf(40f, 30f, 30f)))
                .setWidth(UnitValue.createPercentValue(100f))

            listOf("", "Cantidad", "Valor").forEach { h ->
                valuesTable.addCell(
                    Cell().add(Paragraph(h).setBold().setFontSize(9f))
                        .setBackgroundColor(headerColor)
                        .setFontColor(com.itextpdf.kernel.colors.ColorConstants.WHITE)
                        .setPadding(3f)
                )
            }
            valuesTable.addCell(Cell().add(Paragraph("Precio Unit.").setFontSize(9f)).setPadding(3f))
            valuesTable.addCell(Cell().add(Paragraph(item.quantity.toString()).setFontSize(9f).setTextAlignment(TextAlignment.CENTER)).setPadding(3f))
            valuesTable.addCell(Cell().add(Paragraph(formatCurrency(item.unitPrice)).setFontSize(9f).setTextAlignment(TextAlignment.RIGHT)).setPadding(3f))

            val subtotal = item.quantity * item.unitPrice
            valuesTable.addCell(Cell().add(Paragraph("Subtotal").setBold().setFontSize(9f)).setPadding(3f))
            valuesTable.addCell(Cell().add(Paragraph("").setFontSize(9f)).setPadding(3f))
            valuesTable.addCell(Cell().add(Paragraph(formatCurrency(subtotal)).setBold().setFontSize(9f).setTextAlignment(TextAlignment.RIGHT)).setPadding(3f))

            infoCell.add(valuesTable)
            contentTable.addCell(infoCell)

            card.addCell(Cell().add(contentTable).setBorder(null).setPadding(0f))
            document.add(card)
        }
    }

    private fun itemTypeDisplayName(type: String): String = when (type) {
        "WINDOW"               -> "Ventana"
        "DOOR"                 -> "Puerta"
        "RAILING"              -> "Baranda"
        "FENCE"                -> "Reja"
        "FENCE_DOOR"           -> "Puerta Reja"
        "GATE"                 -> "Portón"
        "STAIR"                -> "Escalera"
        "GRILL"                -> "Parrilla"
        "GRILL_FRONT"          -> "Frente de Parrilla"
        "UNDER_COUNTER"        -> "Bajo Mesada"
        "INDUSTRIAL_FURNITURE" -> "Mueble Industrial"
        "TABLE"                -> "Mesa"
        "CHAIR"                -> "Silla"
        "TRAILER"              -> "Trailer"
        "STORAGE"              -> "Baulera"
        "TRASH_CAN"            -> "Tacho de Basura"
        else                   -> "Otro"
    }

    private fun itemTypePrefix(type: String): String = when (type) {
        "WINDOW"               -> "V"
        "DOOR"                 -> "P"
        "RAILING"              -> "B"
        "FENCE"                -> "R"
        "FENCE_DOOR"           -> "PR"
        "GATE"                 -> "PT"
        "STAIR"                -> "ES"
        "GRILL"                -> "PA"
        "GRILL_FRONT"          -> "FP"
        "UNDER_COUNTER"        -> "BM"
        "INDUSTRIAL_FURNITURE" -> "MI"
        "TABLE"                -> "ME"
        "CHAIR"                -> "SI"
        "TRAILER"              -> "TR"
        "STORAGE"              -> "BA"
        "TRASH_CAN"            -> "TB"
        else                   -> "O"
    }

    private fun formatDate(timestamp: Long): String =
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
}

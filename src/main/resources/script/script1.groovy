import com.sap.gateway.ip.core.customdev.util.Message
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import java.io.ByteArrayOutputStream
import org.apache.camel.impl.DefaultAttachment
import javax.mail.util.ByteArrayDataSource

def cleanHtml(String html) {
    html = html.replaceAll(/<br>/, '<br/>')
    html = html.replaceAll(/<hr>/, '<hr/>')
    html = html.replaceAll("&nbsp;", "&#160;")
    html = html.replaceAll("&(?!amp;|lt;|gt;|quot;|#)", "&amp;")
    return html
}

def Message processData(Message message) {
    def outputStream = new ByteArrayOutputStream()
    try {
        def html = cleanHtml(message.getBody(String))
        new PdfRendererBuilder()
            .useFastMode()
            .withHtmlContent(html, null)
            .toStream(outputStream)
            .run()

        byte[] pdfBytes = outputStream.toByteArray()
        def orderId = message.getProperty("orderId") ?: "unknown"
        def custName = message.getProperty("customerName") ?: "Customer"

        // Add PDF as a proper attachment
        def data = new ByteArrayDataSource(pdfBytes, 'application/pdf')
        def att = new DefaultAttachment(data)
        message.addAttachmentObject("Invoice-" + orderId + ".pdf", att)

        // Body becomes readable email text (NOT the PDF)
        message.setBody("Dear " + custName + ",\n\nPlease find attached your invoice from Halchal Industries.\n\nThank you for your order.\n\nRegards,\nHalchal Industries")
        message.setHeader("Content-Type", "text/plain")
    } catch (Exception e) {
        throw new RuntimeException("PDF generation failed: " + e.message, e)
    } finally {
        outputStream.close()
    }
    return message
}
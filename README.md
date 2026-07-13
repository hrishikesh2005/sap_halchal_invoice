# Halchal Industries — Order-to-Invoice Integration (SAP Cloud Integration)

An event-driven integration built on **SAP Integration Suite (Cloud Integration / CPI)** that automatically generates a **PDF invoice** from an incoming e-commerce order and emails it to the customer and the business owner.

When a customer places an order on the Halchal Industries e-commerce platform, the order is pushed to a CPI endpoint via an HTTP webhook. CPI transforms the order, generates a styled PDF invoice, and delivers it by email — no manual invoicing.

---

## Architecture
Halchal E-commerce (Node.js)
│  order placed → HTTP POST (webhook)
▼
SAP Cloud Integration (CPI)
HTTPS Sender
→ Convert JSON to XML
→ Extract Order Properties (XPath)
→ Build Invoice HTML
→ Generate PDF Invoice (Groovy + OpenHTMLToPDF)
→ Send Invoice via Email (PDF attachment)

---

## Integration Flow

| Step | Type | Purpose |
|------|------|---------|
| **HTTPS Sender** | Sender Adapter | Receives the order as a JSON webhook (`/halchalorders`), secured by the `ESBMessaging.send` role |
| **Convert JSON to XML** | JSON to XML Converter | Converts the incoming JSON order to XML so fields can be read via XPath |
| **Extract Order Properties** | Content Modifier | Extracts order, customer, delivery, line-item, GST, and payment fields into message properties |
| **Build Invoice HTML** | Content Modifier | Constructs a styled XHTML invoice from the extracted properties |
| **Generate PDF Invoice** | Groovy Script | Renders the HTML to a PDF using OpenHTMLToPDF + Apache PDFBox, and attaches it to the message |
| **Send Invoice** | Mail Adapter | Emails the invoice (PDF attached) to the customer and the business owner via Gmail SMTP |

---

## Tech Stack

- **SAP Integration Suite** — Cloud Integration (CPI)
- **Groovy** — payload parsing, PDF generation, attachment handling
- **OpenHTMLToPDF + Apache PDFBox** — HTML-to-PDF rendering (Apache 2.0 / LGPL licensed)
- **XPath** — field extraction from XML
- **Mail Adapter (SMTP)** — Gmail-based email delivery
- **Node.js** — source e-commerce platform (webhook sender)

---

## Sample Order Payload

```json
{
  "orderId": "6a52a03a597bf367f63a1629",
  "orderDate": "2026-07-11T19:57:46.364Z",
  "status": "Pending Approval",
  "customer": { "name": "Sam", "email": "sam@example.com", "phone": "8585858585" },
  "delivery": { "address": "123 Farm Road", "pincode": "111111", "state": "Maharashtra" },
  "lineItem": { "product": "Supreme 16mm Online", "quantity": 1, "unitPriceFinal": 1400, "discountPercent": 0 },
  "amounts": { "taxableValue": 1400, "gstRate": 12, "gstAmount": 168, "grandTotal": 1568 },
  "payment": { "method": "Cash", "status": "Pending" }
}
```

---

## Prerequisites (to deploy this iFlow)

### 1. Required JAR libraries
The PDF generation depends on the following libraries, added as **iFlow Archive resources** (not included in this repo):

| Library | Version |
|---------|---------|
| openhtmltopdf-core | 1.0.10 |
| openhtmltopdf-pdfbox | 1.0.10 |
| pdfbox | 2.0.27 |
| fontbox | 2.0.27 |
| xmpbox | 2.0.27 |
| commons-io | 2.11.0 |
| graphics2d | 0.32 |

Download from Maven Central and upload via the iFlow **References → Archives** section.

### 2. Security Material
A **User Credentials** artifact named `GmailSMTP` (Gmail address + a Gmail **App Password**, with 2-Factor Authentication enabled on the account).

### 3. Runtime
An SAP Process Integration Runtime instance (Cloud Foundry) with a service key providing the `clientid` / `clientsecret` used by the webhook caller.

---

## Key Design Decisions

**Why generate the PDF inside CPI?**  
This project generates the PDF within CPI (via bundled Java libraries) to demonstrate external-library integration and binary/attachment handling in the Groovy runtime. In a production enterprise landscape, PDF generation would more typically live in the source application or a dedicated service such as **SAP Forms Service by Adobe**, with CPI handling integration and routing.

**Why OpenHTMLToPDF over iText?**  
OpenHTMLToPDF (LGPL) and Apache PDFBox (Apache 2.0) are commercially safe, whereas iText is AGPL-licensed. OpenHTMLToPDF also provides a modern CSS renderer for cleaner invoice styling.

**Fault tolerance**  
The PDF generation step wraps rendering in error handling so malformed input fails cleanly rather than corrupting the flow.

---

## Author

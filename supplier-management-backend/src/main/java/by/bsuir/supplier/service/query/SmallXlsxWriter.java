package by.bsuir.supplier.service.query;

import by.bsuir.supplier.dto.response.SupplierReportRow;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.*;

/**
 * Minimal valid Office Open XML spreadsheet, no extra dependencies. All untrusted text is
 * inlineStr.
 */
public final class SmallXlsxWriter {
    private SmallXlsxWriter() {}

    public static byte[] supplierReport(List<SupplierReportRow> rows) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
                entry(
                        zip,
                        "[Content_Types].xml",
                        """
          <?xml version="1.0" encoding="UTF-8"?>
          <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
          <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
          <Default Extension="xml" ContentType="application/xml"/>
          <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
          <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
          </Types>
          """);
                entry(
                        zip,
                        "_rels/.rels",
                        """
          <?xml version="1.0" encoding="UTF-8"?>
          <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
          </Relationships>
          """);
                entry(
                        zip,
                        "xl/workbook.xml",
                        """
          <?xml version="1.0" encoding="UTF-8"?>
          <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
          <sheets><sheet name="Поставщики" sheetId="1" r:id="rId1"/></sheets>
          </workbook>
          """);
                entry(
                        zip,
                        "xl/_rels/workbook.xml.rels",
                        """
          <?xml version="1.0" encoding="UTF-8"?>
          <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
          </Relationships>
          """);
                zip.putNextEntry(new ZipEntry("xl/worksheets/sheet1.xml"));
                Writer w = new OutputStreamWriter(zip, StandardCharsets.UTF_8);
                w.write(
                        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
                String[] headings = {
                    "ID", "Поставщик", "Статус", "Рейтинг", "Категория", "Состояние рейтинга"
                };
                w.write("<row r=\"1\">");
                for (int i = 0; i < headings.length; i++) cell(w, i, 1, headings[i]);
                w.write("</row>");
                int n = 2;
                for (SupplierReportRow row : rows) {
                    w.write("<row r=\"" + n + "\">");
                    Object[] fields = {
                        row.supplierId(),
                        row.name(),
                        row.status(),
                        row.latestScore(),
                        row.band(),
                        row.ratingState()
                    };
                    for (int i = 0; i < fields.length; i++) cell(w, i, n, fields[i]);
                    w.write("</row>");
                    n++;
                }
                w.write("</sheetData></worksheet>");
                w.flush();
                zip.closeEntry();
            }
            return bytes.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static void entry(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static void cell(Writer out, int col, int row, Object value) throws IOException {
        if (value == null) return;
        String address = "" + (char) ('A' + col) + row;
        if (value instanceof Number) {
            out.write("<c r=\"" + address + "\"><v>" + value + "</v></c>");
            return;
        }
        out.write(
                "<c r=\""
                        + address
                        + "\" t=\"inlineStr\"><is><t>"
                        + esc(String.valueOf(value))
                        + "</t></is></c>");
    }

    private static String esc(String val) {
        String clean = val.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");
        return clean.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}

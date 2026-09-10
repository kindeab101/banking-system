package com.securebank.bms.service;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.securebank.bms.dto.StatementResponse;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

@Service
public class ExportService {

    public byte[] statementPdf(StatementResponse statement) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();
            document.add(new Paragraph("DEMO / TEST — Simulated account statement"));
            document.add(new Paragraph("Account: " + statement.accountNumber()));
            document.add(new Paragraph("Customer: " + statement.customerName()));
            document.add(new Paragraph("Period: " + statement.from() + " to " + statement.to()));
            document.add(new Paragraph("Opening: " + statement.openingBalance() + " " + statement.currency()));
            document.add(new Paragraph("Closing: " + statement.closingBalance() + " " + statement.currency()));
            document.add(new Paragraph(" "));
            PdfPTable table = new PdfPTable(6);
            table.addCell("Date");
            table.addCell("Reference");
            table.addCell("Description");
            table.addCell("Debit");
            table.addCell("Credit");
            table.addCell("Balance");
            for (var line : statement.lines()) {
                table.addCell(String.valueOf(line.date()));
                table.addCell(line.reference());
                table.addCell(line.description() == null ? "" : line.description());
                table.addCell(line.debit().toPlainString());
                table.addCell(line.credit().toPlainString());
                table.addCell(line.runningBalance().toPlainString());
            }
            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to generate statement PDF");
        }
    }

    public byte[] transactionsCsv(Iterable<com.securebank.bms.dto.TransactionResponse> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("reference,type,status,amount,currency,source,destination,createdAt,description\n");
        for (var t : rows) {
            sb.append(csv(t.reference())).append(',')
                    .append(csv(t.transactionType())).append(',')
                    .append(csv(t.status())).append(',')
                    .append(t.amount().toPlainString()).append(',')
                    .append(csv(t.currency())).append(',')
                    .append(csv(t.sourceAccountNumber())).append(',')
                    .append(csv(t.destinationAccountNumber())).append(',')
                    .append(csv(String.valueOf(t.createdAt()))).append(',')
                    .append(csv(t.description())).append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}

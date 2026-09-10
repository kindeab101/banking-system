package com.securebank.bms.controller;

import com.securebank.bms.dto.StatementResponse;
import com.securebank.bms.security.CurrentUserService;
import com.securebank.bms.service.BankingQueryService;
import com.securebank.bms.service.ExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/accounts")
public class StatementController {

    private final BankingQueryService queryService;
    private final ExportService exportService;
    private final CurrentUserService currentUserService;

    public StatementController(BankingQueryService queryService,
                               ExportService exportService,
                               CurrentUserService currentUserService) {
        this.queryService = queryService;
        this.exportService = exportService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/{accountNumber}/statement")
    public StatementResponse statement(@PathVariable String accountNumber,
                                       @RequestParam LocalDate from,
                                       @RequestParam LocalDate to) {
        return queryService.statement(currentUserService.requireUser(), accountNumber, from, to);
    }

    @GetMapping("/{accountNumber}/statement.pdf")
    public ResponseEntity<byte[]> statementPdf(@PathVariable String accountNumber,
                                               @RequestParam LocalDate from,
                                               @RequestParam LocalDate to) {
        var statement = queryService.statement(currentUserService.requireUser(), accountNumber, from, to);
        byte[] pdf = exportService.statementPdf(statement);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=statement-" + accountNumber + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}

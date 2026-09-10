package com.securebank.bms.controller;

import com.securebank.bms.dto.TransferRequest;
import com.securebank.bms.dto.TransferResponse;
import com.securebank.bms.security.CurrentUserService;
import com.securebank.bms.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
public class TransferController {

    private final TransferService transferService;
    private final CurrentUserService currentUserService;

    public TransferController(TransferService transferService, CurrentUserService currentUserService) {
        this.transferService = transferService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasRole('CUSTOMER')")
    public TransferResponse transfer(@Valid @RequestBody TransferRequest request,
                                     @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return transferService.transfer(currentUserService.requireUser(), request, idempotencyKey, false);
    }
}

package com.securebank.bms.dto;

public record AdminDashboardResponse(
        long totalCustomers,
        long activeAccounts,
        long transactionsToday,
        long successfulToday,
        long failedTransactions,
        ListItem[] recentAudit
) {
    public record ListItem(String action, String actor, String result, String createdAt) {
    }
}

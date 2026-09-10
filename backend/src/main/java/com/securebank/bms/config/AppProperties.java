package com.securebank.bms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private Cors cors = new Cors();
    private Jwt jwt = new Jwt();
    private Security security = new Security();
    private Transfer transfer = new Transfer();
    private Demo demo = new Demo();

    public Cors getCors() { return cors; }
    public Jwt getJwt() { return jwt; }
    public Security getSecurity() { return security; }
    public Transfer getTransfer() { return transfer; }
    public Demo getDemo() { return demo; }

    public static class Cors {
        private String origins = "http://localhost:5173";
        public String getOrigins() { return origins; }
        public void setOrigins(String origins) { this.origins = origins; }
    }

    public static class Jwt {
        private String secret;
        private long accessTokenMinutes = 15;
        private long refreshTokenDays = 7;
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public long getAccessTokenMinutes() { return accessTokenMinutes; }
        public void setAccessTokenMinutes(long accessTokenMinutes) { this.accessTokenMinutes = accessTokenMinutes; }
        public long getRefreshTokenDays() { return refreshTokenDays; }
        public void setRefreshTokenDays(long refreshTokenDays) { this.refreshTokenDays = refreshTokenDays; }
    }

    public static class Security {
        private int maxFailedLogins = 5;
        private int lockoutMinutes = 15;
        public int getMaxFailedLogins() { return maxFailedLogins; }
        public void setMaxFailedLogins(int maxFailedLogins) { this.maxFailedLogins = maxFailedLogins; }
        public int getLockoutMinutes() { return lockoutMinutes; }
        public void setLockoutMinutes(int lockoutMinutes) { this.lockoutMinutes = lockoutMinutes; }
    }

    public static class Transfer {
        private BigDecimal maxAmount = new BigDecimal("1000000.00");
        public BigDecimal getMaxAmount() { return maxAmount; }
        public void setMaxAmount(BigDecimal maxAmount) { this.maxAmount = maxAmount; }
    }

    public static class Demo {
        private boolean seed = true;
        public boolean isSeed() { return seed; }
        public void setSeed(boolean seed) { this.seed = seed; }
    }
}

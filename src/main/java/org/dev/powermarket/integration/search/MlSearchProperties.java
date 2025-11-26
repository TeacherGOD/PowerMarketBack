package org.dev.powermarket.integration.search;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ml.search")
public class MlSearchProperties {
    /**
     * Enable/disable ML-based search integration. If disabled, DB search will be used.
     */
    private boolean enabled = true;

    /**
     * Base URL of ML search service (without trailing slash), e.g. http://localhost:8081/api
     */
    private String baseUrl = "http://localhost:8081/api";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
}
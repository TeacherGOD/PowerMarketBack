package org.dev.powermarket.integration.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class MlSearchClient {
    private static final Logger log = LoggerFactory.getLogger(MlSearchClient.class);

    private final RestTemplate restTemplate;
    private final MlSearchProperties props;
    private final ObjectMapper mapper = new ObjectMapper();

    public MlSearchClient(RestTemplate restTemplate, MlSearchProperties props) {
        this.restTemplate = restTemplate;
        this.props = props;
    }

    /**
     * Calls ML search microservice and returns list of Service UUIDs ordered by relevance.
     */
    public List<UUID> searchServiceIds(String query, int page, int perPage) {
        List<UUID> ids = new ArrayList<>();
        if (!props.isEnabled() || query == null || query.isBlank()) {
            return ids;
        }
        try {
            String url = String.format("%s/search?q=%s&page=%d&per_page=%d", props.getBaseUrl(), encode(query), page, perPage);
            ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                log.warn("ML search returned non-2xx: {}", resp.getStatusCode());
                return ids;
            }
            JsonNode root = mapper.readTree(resp.getBody());
            JsonNode hits = root.path("hits");
            if (hits.isMissingNode() || !hits.isArray()) {
                // Some services may wrap hits into { data: { hits: [...] } }
                hits = root.path("data").path("hits");
            }
            for (JsonNode hit : hits) {
                // Typesense format: each hit has 'document' with fields
                JsonNode doc = hit.path("document").isMissingNode() ? hit : hit.path("document");
                String idStr = doc.path("id").asText(null);
                if (idStr != null) {
                    try {
                        ids.add(UUID.fromString(idStr));
                    } catch (IllegalArgumentException e) {
                        log.debug("Skip non-UUID id from ML search: {}", idStr);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error calling ML search service", e);
        }
        return ids;
    }

    private String encode(String s) {
        try {
            return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }
}
package org.opensearch.security.configuration;
 
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.TreeMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.security.configuration.SecurityConfigVersionDocument.SecurityConfig;
import org.opensearch.security.securityconf.impl.SecurityDynamicConfiguration;
 
public class SecurityConfigDiffCalculator {
    private static final Logger LOGGER = LogManager.getLogger(SecurityConfigDiffCalculator.class);
 
    /**
     * Checks if the security configuration has changed.
     * This method normalizes both configurations (using sorted maps and removing the "lastUpdated" field)
     * and then compares them.
     *
     * @param oldConfig the old configuration map
     * @param newConfig the new configuration map
     * @return true if there are differences; false otherwise.
     */
    public static boolean hasSecurityConfigChanged(Map<String, SecurityConfig> oldConfig, Map<String, SecurityConfig> newConfig) {
        if (oldConfig == null || oldConfig.isEmpty()) {
            LOGGER.info("Old configuration is empty. Treating as a new configuration.");
            return true;
        }
 
        Map<String, Object> normOld = normalize(oldConfig);
        Map<String, Object> normNew = normalize(newConfig);
 
        if (normOld.equals(normNew)) {
            LOGGER.info("No changes detected in security configuration.");
            return false;
        }
 
        MapDifference<String, Object> diff = Maps.difference(normOld, normNew);
        if (!diff.entriesOnlyOnLeft().isEmpty()) {
            LOGGER.info("Removed entries: {}", diff.entriesOnlyOnLeft());
        }
        if (!diff.entriesOnlyOnRight().isEmpty()) {
            LOGGER.info("Added entries: {}", diff.entriesOnlyOnRight());
        }
        if (!diff.entriesDiffering().isEmpty()) {
            LOGGER.info("Modified entries:");
            diff.entriesDiffering().forEach((key, value) -> {
                LOGGER.info("Key: '{}', Old Value: {}, New Value: {}", key, value.leftValue(), value.rightValue());
            });
        }
 
        return true;
    }
 
    public static Map<String, Object> normalize(Map<String, SecurityConfig> config) {
        Map<String, Object> normalizedMap = new TreeMap<>();
    
        for (Map.Entry<String, SecurityConfig> entry : config.entrySet()) { 
            SecurityConfig value = entry.getValue();
    
            if (value != null) {
                // Normalize only `configData` and exclude `lastUpdated`
                Map<String, Object> normalizedConfigData = normalizeConfigData(value.getConfigData());
                normalizedMap.put(entry.getKey(), normalizedConfigData);
            } else {
                normalizedMap.put(entry.getKey(), null);
            }
        }
        return normalizedMap;
    }
    
    private static Map<String, Object> normalizeConfigData(Map<String, ?> configData) {
        Map<String, Object> normalizedConfigData = new TreeMap<>();
    
        for (Map.Entry<String, ?> entry : configData.entrySet()) {
            Object value = entry.getValue();
    
            if (value instanceof SecurityDynamicConfiguration<?>) {
                // Extract actual config data from SecurityDynamicConfiguration
                SecurityDynamicConfiguration<?> config = (SecurityDynamicConfiguration<?>) value;
                normalizedConfigData.put(entry.getKey(), extractConfigData(config));
            } else if (value instanceof Map) {
                // If it's already a Map, normalize it recursively
                @SuppressWarnings("unchecked")
                Map<String, Object> innerMap = (Map<String, Object>) value;
                normalizedConfigData.put(entry.getKey(), normalizeConfigData(innerMap));
            } else {
                normalizedConfigData.put(entry.getKey(), value);
            }
        }
    
        return normalizedConfigData;
    }
    
    private static Map<String, Object> extractConfigData(SecurityDynamicConfiguration<?> config) {
        Map<String, Object> extractedMap = new TreeMap<>();
    
        Map<String, ?> configEntries = config.getCEntries();
        if (configEntries != null) {
            extractedMap.putAll(configEntries);
        }
    
        return extractedMap;
    }     
    
}
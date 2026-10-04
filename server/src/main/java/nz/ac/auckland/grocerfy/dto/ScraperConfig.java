package nz.ac.auckland.grocerfy.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * A POJO implementation of the scrape_targets json file for easy access
 * ScraperConfig
 * @param supermarket
 * @param links
 * @param branches
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ScraperConfig(
    String supermarket,
    List<String> links,
    List<Map<String, String>> branches) {
    
}

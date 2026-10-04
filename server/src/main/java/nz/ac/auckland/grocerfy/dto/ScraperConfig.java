package nz.ac.auckland.grocerfy.dto;

import java.util.List;
import java.util.Map;

/**
 * A POJO implementation of the scrape_targets json file for easy access
 * ScraperConfig
 * @param supermarket
 * @param links
 * @param branches
 */
public record ScraperConfig(String supermarket, List<String> links, List<Map<String, String>> branches) {
    
}

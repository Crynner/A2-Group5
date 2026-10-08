package nz.ac.auckland.grocerfy.scraper;

import java.util.List;
import java.util.Set;

import org.jsoup.nodes.Document;
import org.springframework.data.util.Pair;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import nz.ac.auckland.grocerfy.dto.ProductInfo;
import nz.ac.auckland.grocerfy.model.Allergen;
import nz.ac.auckland.grocerfy.model.Dietary;

public abstract class SupermarketScraper {

    protected final ObjectMapper mapper = new ObjectMapper();

    protected void addDietaryIfMatch(Set<Dietary> dietSet, JsonNode facet) {
        String facetName = facet.get("itemDescription").asText();
        for (Dietary diet : Dietary.values()) {
            if (facetName.equalsIgnoreCase(diet.getKeyword())) {
                dietSet.add(diet);
                System.out.println("adding dietary " + facetName);
                break;
            }
        }
    }

    public abstract void setupCookies();
    public abstract boolean changeStore(String storeData);
    public abstract List<ProductInfo> extractProducts(Document pageData);
    public abstract Pair<Set<Allergen>, Set<Dietary>> getProductInfo(String productId, String storeUuid);
}

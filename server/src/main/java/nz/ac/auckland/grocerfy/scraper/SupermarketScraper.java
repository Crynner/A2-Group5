package nz.ac.auckland.grocerfy.scraper;

import java.util.List;

import org.jsoup.nodes.Document;

import com.fasterxml.jackson.databind.ObjectMapper;

import nz.ac.auckland.grocerfy.dto.ProductInfo;
import nz.ac.auckland.grocerfy.model.Allergen;

public abstract class SupermarketScraper {

    protected final ObjectMapper mapper = new ObjectMapper();

    public abstract void setupCookies();
    public abstract boolean changeStore(String storeData);
    public abstract List<ProductInfo> extractProducts(Document pageData);
    public abstract List<Allergen> getProductAllergens(String productId, String storeUuid);
}

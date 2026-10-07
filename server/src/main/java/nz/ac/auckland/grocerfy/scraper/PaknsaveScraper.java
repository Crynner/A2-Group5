package nz.ac.auckland.grocerfy.scraper;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import nz.ac.auckland.grocerfy.dto.ProductInfo;
import nz.ac.auckland.grocerfy.util.HttpUtils;
import nz.ac.auckland.grocerfy.model.Allergen;

public class PaknsaveScraper extends SupermarketScraper {
    private static final String STORE_ADDRESS = "https://www.paknsave.co.nz";
    private static final String AUTH_LINK = "https://www.paknsave.co.nz/api/user/get-current-user";
    private static final String STORE_CHANGE_ENDPOINT = "https://api-prod.paknsave.co.nz/v1/edge/cart/store/";

    private static final String PRODUCT_XPATH = "//*[@itemtype='https://schema.org/Product']";
    private static final String PRODUCT_URL_XPATH = ".//a[1]";
    private static final String PRODUCT_NAME_XPATH = ".//*[@itemprop='name']";
    private static final String PRODUCT_PRICE_DOLLARS_XPATH = ".//*[@data-testid='price-dollars']";
    private static final String PRODUCT_PRICE_CENTS_XPATH = ".//*[@data-testid='price-cents']";
    private static final String PRODUCT_SIZE_XPATH = ".//*[@data-testid='product-subtitle']";

    protected String authToken;

    public void setupCookies() {
        // initialise generic store cookies - visit homepage
        HttpRequest homepageRequest = HttpRequest.newBuilder()
            .uri(URI.create(STORE_ADDRESS))
            .headers(HttpUtils.getGenericHeaders())
            .headers(HttpUtils.getGetHeaders())
            .GET()
            .build();
        HttpUtils.sendHttpRequest(homepageRequest, HttpResponse.BodyHandlers.discarding());

        HttpRequest authRequest = HttpRequest.newBuilder()
            .uri(URI.create(AUTH_LINK))
            .headers(HttpUtils.getGenericHeaders())
            .headers(HttpUtils.getGetHeaders())
            .header("Sec-Fetch-Site", "same-origin")
            .header("Origin", STORE_ADDRESS)
            .header("Referer", STORE_ADDRESS)
            .POST(HttpRequest.BodyPublishers.ofString("{}"))
            .build();

        Optional<HttpResponse<String>> authOptional = HttpUtils.sendHttpRequest(authRequest, HttpResponse.BodyHandlers.ofString());
        if (authOptional.isEmpty()) {
            throw new IllegalStateException("Auth token cannot be established.");
        }
        try {
            JsonNode authNode = mapper.readTree(authOptional.get().body());
            authToken = authNode.get("access_token").asText();
        } catch (JsonProcessingException exc) {
            throw new IllegalStateException("Auth token cannot be established.");
        }
    }
    
    public boolean changeStore(String storeData) {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(STORE_CHANGE_ENDPOINT + storeData))
            .headers(HttpUtils.getGenericHeaders())
            .headers(HttpUtils.getPostHeaders())
            .header("Sec-Fetch-Site", "same-site")
            .header("Authorization", "Bearer " + authToken)
            .header("Origin", STORE_ADDRESS)
            .header("Referer", STORE_ADDRESS)
            .POST(HttpRequest.BodyPublishers.ofString("{}"))
            .build();
        Optional<HttpResponse<Void>> responseOptional = HttpUtils.sendHttpRequest(req, HttpResponse.BodyHandlers.discarding());
        return responseOptional.isPresent();
    }

    public List<ProductInfo> extractProducts(Document pageData) {
        List<ProductInfo> products = new ArrayList<>();
        Elements productRaws = pageData.selectXpath(PRODUCT_XPATH);
        for (Element product : productRaws) {
            // for each element, extracts text only if not null from first()
            Element nameElement = product.selectXpath(PRODUCT_NAME_XPATH).first();
            String name = nameElement == null ? null : nameElement.text().trim();

            // last to skip multibuy deals
            Element dollarElement = product.selectXpath(PRODUCT_PRICE_DOLLARS_XPATH).last();
            Element centElement = product.selectXpath(PRODUCT_PRICE_CENTS_XPATH).last();
            BigDecimal price;
            if (dollarElement != null && centElement != null) {
                try {
                    price = new BigDecimal(dollarElement.text() + "." + centElement.text());
                } catch (NumberFormatException e) {
                    price = null;
                    System.err.println("Error for product: " + name + " given price " + dollarElement.text() + "." + centElement.text() + ": " + e.getLocalizedMessage());
                }
                
            } else {
                System.err.println("Price not found for product " + name + ", skipping...");
                continue;
            }

            Element sizeElement = product.selectXpath(PRODUCT_SIZE_XPATH).first();
            String size = sizeElement == null ? null : sizeElement.text().trim();

            // get page for allergy crap
            Element productLink = product.selectXpath(PRODUCT_URL_XPATH).first();
            // url stored in the DOM has multiple RequestParam components (i.e. the ?name=value?cost=etc)
            String untrimmedUrl = productLink.attr("href");
            Pattern productIdPattern = Pattern.compile("\\/([^\\/]*?)pns\\?");
            Matcher matcher = productIdPattern.matcher(untrimmedUrl);
            matcher.find();
            String productId = matcher.group(1)
                .toUpperCase()
                .replace("_", "-");

            products.add(new ProductInfo(name, price, size, productId));
        }

        return products;
    }

    public List<Allergen> getProductAllergens(String productId, String storeUuid) {
        HttpRequest productInfoRequest = HttpRequest.newBuilder()
            .uri(URI.create("https://api-prod.paknsave.co.nz/v1/edge/store/" + storeUuid + "/product/" + productId))
            .headers(HttpUtils.getGenericHeaders())
            .headers(HttpUtils.getPostHeaders())
            .header("Sec-Fetch-Site", "same-site")
            .header("Authorization", "Bearer " + authToken)
            .header("Origin", STORE_ADDRESS)
            .header("Referer", STORE_ADDRESS)
            .GET()
            .build();

        Optional<HttpResponse<String>> productInfoOptional = HttpUtils.sendHttpRequest(productInfoRequest, HttpResponse.BodyHandlers.ofString());
        if (productInfoOptional.isEmpty()) {
            System.err.println("Product info for product id " + productId + " unable to be retrieved. Skipping...");
            return null;
        }
        try {
            JsonNode responseNode = mapper.readTree(productInfoOptional.get().body());
            // read allergens first, base solely off that, else look at ingredients, else use UNKNOWN
            String allergenString = "";

            JsonNode ingredients = responseNode.get("ingredientStatement");
            if (ingredients != null) {
                allergenString = allergenString.concat(ingredients.asText().toLowerCase() + " "); // add space to separate from allergen list
            }
            JsonNode allergens = responseNode.get("allergenStatement");
            if (allergens != null) {
                allergenString = allergenString.concat(allergens.asText().toLowerCase());
            }

            // if not empty, run regex checks on Allergen enum
            if (!allergenString.equals("")) {
                Set<Allergen> productAllergens = EnumSet.noneOf(Allergen.class);
                for (Allergen allergen : Allergen.values()) {
                    for (String keyword : allergen.getKeywords()) {
                        Pattern keywordPattern = Pattern.compile("\\b" + keyword + "\\b");
                        if (keywordPattern.matcher(allergenString).find()) {
                            productAllergens.add(allergen);
                            break; // check next allergen
                        }
                    }
                }
            } 
            // should set to actual product as param, inject fields in post
            

            // set vegan/vegetarian flag, non-gmo? read facets list
            return null;
        } catch (JsonProcessingException exc) {
            System.err.println("Product info for product id " + productId + " unable to be read. Skipping...");
        }
        
        return null;
    }
}

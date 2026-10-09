package nz.ac.auckland.grocerfy.scraper;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.data.util.Pair;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import nz.ac.auckland.grocerfy.dto.ProductInfo;
import nz.ac.auckland.grocerfy.util.HttpUtils;
import nz.ac.auckland.grocerfy.model.Allergen;
import nz.ac.auckland.grocerfy.model.Dietary;

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

        refreshCookies();
    }

    public void refreshCookies() {
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

    public <T> HttpResponse<T> sendRequestWithAuth(HttpRequest request, BodyHandler<T> handler) {
        Optional<HttpResponse<T>> responseOptional = HttpUtils.sendHttpRequest(request, handler);
        if (responseOptional.isEmpty()) { // shouldn't get here unless through other exceptions
            throw new IllegalStateException("Request returns null, unexpected (is the link valid?)");
        }
        HttpResponse<T> response = responseOptional.get();
        // potentially due to auth token expiry
        if (response.statusCode() >= 400) {
            refreshCookies();
            HttpRequest newRequest = HttpRequest.newBuilder()
                .uri(request.uri())
                .headers(HttpUtils.getGenericHeaders())
                .headers(HttpUtils.getPostHeaders()) // post headers because of a non-traditional get (returning json)
                .header("Sec-Fetch-Site", "same-site")
                .header("Authorization", "Bearer " + authToken)
                .header("Origin", STORE_ADDRESS)
                .header("Referer", STORE_ADDRESS)
                .GET()
                .build();

            Optional<HttpResponse<T>> responseOptional2 = HttpUtils.sendHttpRequest(newRequest, handler);
            if (responseOptional2.isEmpty()) { // shouldn't get here unless through other exceptions
                throw new IllegalStateException("Request returns null, unexpected (is the link valid?)");
            }
            // second check doesn't work? hard to tell why, workaround with multiple exc
            HttpResponse<T> response2 = responseOptional2.get();
            if (response2.statusCode() >= 400) { 
                throw new IllegalStateException("Error with request links, does not return after refreshing auth: " + request.uri().toString());
            }
            return response2;
        }
        return response;
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

    public Pair<Set<Allergen>, Set<Dietary>> getProductInfo(String productId, String storeUuid) {
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

        HttpResponse<String> productInfo = sendRequestWithAuth(productInfoRequest, HttpResponse.BodyHandlers.ofString());

        try {
            Set<Allergen> productAllergens = EnumSet.noneOf(Allergen.class);
            Set<Dietary> productDietary = EnumSet.noneOf(Dietary.class);

            JsonNode responseNode = mapper.readTree(productInfo.body());
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
            JsonNode facets = responseNode.get("facets");
            if (facets != null) {
                Iterator<JsonNode> facetIterator = facets.elements();
                facetIterator.forEachRemaining(facet -> addDietaryIfMatch(productDietary, facet));
            }

            JsonNode categoryNode = responseNode.get("categories");
            if (categoryNode == null) {
                System.err.println("Category JSON not found(?), short-circuiting...");
                return Pair.of(productAllergens, productDietary);
            }
            List<String> productCategories = mapper.convertValue(categoryNode, new TypeReference<List<String>>(){});
            for (String category : productCategories) {
                if (category.equalsIgnoreCase("vegetables") ||
                        category.equalsIgnoreCase("fruit")) {
                    productDietary.add(Dietary.VEGAN);
                    productDietary.add(Dietary.VEGETARIAN);
                    System.out.println("Product in Fruit/Veg category, adding Vegetarian/Vegan tag");
                }
            }

        System.out.println("Product Dietary/Allergens fully identified: " + productId);
        return Pair.of(productAllergens, productDietary);

        } catch (JsonProcessingException exc) {
            System.err.println("Product info for product id " + productId + " unable to be read. Skipping...");
        }
        
        return null;
    }
}

package nz.ac.auckland.grocerfy.scraper;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import nz.ac.auckland.grocerfy.dto.ProductInfo;
import nz.ac.auckland.grocerfy.util.HttpUtils;

public class NewWorldScraper extends SupermarketScraper {
    private static final String STORE_ADDRESS = "https://www.newworld.co.nz/";
    private static final String AUTH_LINK = "https://www.newworld.co.nz/api/user/get-current-user";
    private static final String STORE_CHANGE_ENDPOINT = "https://api-prod.newworld.co.nz/v1/edge/cart/store/";

    private static final String PRODUCT_XPATH = "//*[@itemtype='https://schema.org/Product']";
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

            products.add(new ProductInfo(name, price, size));
        }

        return products;
    }
}

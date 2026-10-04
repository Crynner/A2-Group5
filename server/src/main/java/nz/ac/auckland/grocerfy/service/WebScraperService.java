package nz.ac.auckland.grocerfy.service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import nz.ac.auckland.grocerfy.dto.ProductInfo;
import nz.ac.auckland.grocerfy.dto.ScraperConfig;
import nz.ac.auckland.grocerfy.model.Product;
import nz.ac.auckland.grocerfy.model.Store;
import nz.ac.auckland.grocerfy.model.StorePrice;
import nz.ac.auckland.grocerfy.repository.ProductRepository;

// TODO have better exception handling
// TODO logger whatnots?
// TODO refactor generic headers
// TODO wipe store prices every 2 days

@Service
public class WebScraperService {
    private static final String PRODUCT_XPATH = "//*[@itemtype='https://schema.org/Product']";
    private static final String PRODUCT_NAME_XPATH = ".//*[@itemprop='name']";
    private static final String PRODUCT_PRICE_DOLLARS_XPATH = ".//*[@data-testid='price-dollars']";
    private static final String PRODUCT_PRICE_CENTS_XPATH = ".//*[@data-testid='price-cents']";
    private static final String PRODUCT_SIZE_XPATH = ".//*[@data-testid='product-subtitle']";

    private final ProductRepository productRepository;

    private final ScraperDatabaseService databaseService;

    private final ObjectMapper mapper = new ObjectMapper();

    private final CookieManager cookieManager = new CookieManager();
    private final HttpClient scraperClient;

    private final String paknsaveSessionAuth;

    @Value ("classpath:scrape_targets.json")
    private Resource targets;

    @Autowired 
    public WebScraperService(
        ProductRepository productRepository,
        ScraperDatabaseService databaseService
    ) {
        this.productRepository = productRepository;
        this.databaseService = databaseService;

        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        this.scraperClient = HttpClient.newBuilder()
            .cookieHandler(cookieManager)
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

        paknsaveSessionAuth = paknsaveInit();
    }

    /**
     * Runs immediately when the app starts.
     * Checks if the database is empty and populates it if true.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void checkAndScrapeOnStartup() {
        if (productRepository.count() == 0) {
            executeScraping();
        }
    }

    /**
     * Runs every 2 days.
     * initialDelayString prevents running immediately on startup alongside the event listener.
     */
    @Scheduled(fixedRateString = "P2D", initialDelayString = "P2D")
    public void scheduledScrape() {
        executeScraping();
    }

    private synchronized void executeScraping() {
        // firstly wipe storeprices
        // open json file, parsed as object?
        try (InputStream inputStream = targets.getInputStream()) {
            // iterate ove reach supermarket brand (e.g. paknsave, new world, etc.)
            for (ScraperConfig scraperData : mapper.readValue(inputStream, new TypeReference<List<ScraperConfig>>(){})) {
                for (Map<String, String> storeInfo : scraperData.branches()) {
                    // change client's store region
                    if (!setPaknsaveStore(storeInfo.get("region_cookie"))) {
                        System.err.println("Store POST closed unexpectedly, see above error for details.");
                        continue;
                    }

                    Store currentStore = databaseService.addStore(
                        scraperData.supermarket() + " " + storeInfo.get("store_name"),
                        storeInfo.get("store_name"),
                        storeInfo.get("address"));

                    System.out.println("Store created: " + currentStore.getName());

                    scrapeLinks(scraperData.links(), currentStore);

                }
                System.out.println("All stores for " + scraperData.supermarket() + " completed.");
            }
        } catch (IOException exc) {
            System.err.println("Some IO issue, " + exc.getLocalizedMessage());
        } 
    }

    private HttpRequest makeRequest(String link) {
        return HttpRequest.newBuilder()
            .uri(URI.create(link))
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/153.0.0.0 Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "en-US")
            .header("Sec-Ch-Ua", "\"Chromium\";v=\"122\", \"Not(A:Brand\";v=\"24\"")
            .header("Sec-Ch-Ua-Mobile", "?0")
            .header("Sec-Ch-Ua-Platform", "\"Windows\"")
            .header("Sec-Fetch-Dest", "document")
            .header("Sec-Fetch-Mode", "navigate")
            .header("Sec-Fetch-Site", "none")
            .header("Sec-Fetch-User", "?1")
            .header("Upgrade-Insecure-Requests", "1")
            .GET()
            .build();
    }

    /**
     * Scrapes provided links given a Store object to correspond the prices to.
     * @param links the list of links to scrape
     * @param store the store to map prices to (regional pricing)
     */
    private void scrapeLinks(List<String> links, Store store) {
        for (String link : links) {
            Optional<Document> docOpt = getWebPage(link);
            if (docOpt.isEmpty()) {
                System.err.println("Link failed, skipping: " + link);
                continue;
            }
            Document doc = docOpt.get();
            List<ProductInfo> productData = extractProducts(doc);

            for (ProductInfo productInfo : productData) {
                Product product = databaseService.createOrGetProduct(productInfo.productName(), productInfo.productSize());
                StorePrice productPrice = new StorePrice(product, store, productInfo.price());

                databaseService.saveProductPrice(productPrice);
            }
            System.out.println("Link fully scraped: " + link);
        }
    }

    /**
     * Helper method for processing GET body without repeated try-catch patterns.
     * @param link the URL to GET
     * @return the link's DOM, else Optional.empty()
     */
    private Optional<Document> getWebPage(String link) {
        try {
            HttpResponse<String> res = scraperClient.send(makeRequest(link), HttpResponse.BodyHandlers.ofString());
            return Optional.of(Jsoup.parse(res.body()));
        } catch (InterruptedException exc) { // propagate interruption signal
            System.err.println("Caught interrupt from GET " + link + ", no data returned.");
            Thread.currentThread().interrupt();
        } catch (IOException exc) {
            System.err.println("Caught IO error from GET " + link + ", no data returned.");
        }
        return Optional.empty();
        
    }

    /**
     * Extracts the Product information and their associated prices.
     * @param html the DOM of the scraped page
     * @return a list of ProductInfo DTO objects, holding necesary Product info plus prices.
     */
    private List<ProductInfo> extractProducts(Document html) {
        List<ProductInfo> products = new ArrayList<>();
        Elements productRaws = html.selectXpath(PRODUCT_XPATH);
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
                price = null;
                System.out.println("Price invalid/not found");
            }

            Element sizeElement = product.selectXpath(PRODUCT_SIZE_XPATH).first();
            String size = sizeElement == null ? null : sizeElement.text().trim();

            products.add(new ProductInfo(name, price, size));
        }

        return products;
    }

    /**
     * Initialises necessary cookies and headers for Pak'nSave scraping.
     * @return The auth token header to use under 'Bearer'.
     */
    private String paknsaveInit() {
        // initialise generic store cookies - visit homepage
        HttpRequest homeRequest = HttpRequest.newBuilder()
            .uri(URI.create("https://www.paknsave.co.nz/"))
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/153.0.0.0 Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "en-US")
            .header("Sec-Ch-Ua", "\"Chromium\";v=\"122\", \"Not(A:Brand\";v=\"24\"")
            .header("Sec-Ch-Ua-Mobile", "?0")
            .header("Sec-Ch-Ua-Platform", "\"Windows\"")
            .header("Sec-Fetch-Dest", "document")
            .header("Sec-Fetch-Mode", "navigate")
            .header("Sec-Fetch-Site", "none")
            .header("Sec-Fetch-User", "?1")
            .header("Upgrade-Insecure-Requests", "1")
            .GET()
            .build();

        try {
            // send request discarding GET body (we only care about cookies)
            scraperClient.send(homeRequest, HttpResponse.BodyHandlers.discarding());
        } catch (Exception exc) {
            exc.printStackTrace();
            return null;
        }

        HttpRequest authRequest = HttpRequest.newBuilder()
            .uri(URI.create("https://www.paknsave.co.nz/api/user/get-current-user"))
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/153.0.0.0 Safari/537.36")
            .header("Accept", "*/*")
            .header("Accept-Language", "en-US")
            .header("Sec-Ch-Ua", "\"Chromium\";v=\"122\", \"Not(A:Brand\";v=\"24\"")
            .header("Sec-Ch-Ua-Mobile", "?0")
            .header("Sec-Ch-Ua-Platform", "\"Windows\"")
            .header("Sec-Fetch-Dest", "empty")
            .header("Sec-Fetch-Mode", "cors")
            .header("Sec-Fetch-Site", "same-origin")
            .header("Sec-Fetch-User", "?1")
            .header("Upgrade-Insecure-Requests", "1")
            .header("Content-Type", "application/json")
            .header("Origin", "https://www.paknsave.co.nz")
            .header("Referer", "https://www.paknsave.co.nz/")
            .POST(HttpRequest.BodyPublishers.ofString("{}"))
            .build();
        
        try {
            HttpResponse<String> authResponse = scraperClient.send(authRequest, HttpResponse.BodyHandlers.ofString());
            // process json response to get the auth token
            JsonNode authNode = mapper.readTree(authResponse.body());
            return authNode.get("access_token").asText();
        } catch (Exception exc) {
            exc.printStackTrace();
            return null;
        }
    }

    /**
     * Sets the store of the paknsave to view their respective regional prices.
     * @param storeId The Pak'nSave store UUID
     * @return true if POST request is successful, else false.
     */
    private boolean setPaknsaveStore(String storeId) {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create("https://api-prod.paknsave.co.nz/v1/edge/cart/store/" + storeId))
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/153.0.0.0 Safari/537.36")
            .header("Accept", "*/*")
            .header("Accept-Language", "en-US")
            .header("Sec-Ch-Ua", "\"Chromium\";v=\"122\", \"Not(A:Brand\";v=\"24\"")
            .header("Sec-Ch-Ua-Mobile", "?0")
            .header("Sec-Ch-Ua-Platform", "\"Windows\"")
            .header("Sec-Fetch-Dest", "empty")
            .header("Sec-Fetch-Mode", "cors")
            .header("Sec-Fetch-Site", "same-site")
            .header("Sec-Fetch-User", "?1")
            .header("Upgrade-Insecure-Requests", "1")
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + paknsaveSessionAuth)
            .header("Origin", "https://www.paknsave.co.nz")
            .header("Referer", "https://www.paknsave.co.nz/")
            .POST(HttpRequest.BodyPublishers.ofString("{}"))
            .build();
        try {
            HttpResponse<String> response = scraperClient.send(req, HttpResponse.BodyHandlers.ofString());
            // 400+ indicates error
            if (response.statusCode() >= 400) {
                System.err.println("Store POST failed unexpectedly, code: " + response.statusCode());
                return false;
            }
            return true;
        } catch (Exception exc) {
            exc.printStackTrace();
            return false;
        }
        
    }
}

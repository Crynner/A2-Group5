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
import java.net.http.HttpResponse.BodyHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import nz.ac.auckland.grocerfy.dto.ProductInfo;
import nz.ac.auckland.grocerfy.dto.ScraperConfig;
import nz.ac.auckland.grocerfy.model.Product;
import nz.ac.auckland.grocerfy.model.Store;
import nz.ac.auckland.grocerfy.model.StorePrice;
import nz.ac.auckland.grocerfy.repository.ProductRepository;

@Service
public class WebScraperService {
    private boolean bypassTimeDebug = true;

    private static final String PAKNSAVE_ADDRESS = "https://www.paknsave.co.nz";

    private static final String PRODUCT_XPATH = "//*[@itemtype='https://schema.org/Product']";
    private static final String PRODUCT_NAME_XPATH = ".//*[@itemprop='name']";
    private static final String PRODUCT_PRICE_DOLLARS_XPATH = ".//*[@data-testid='price-dollars']";
    private static final String PRODUCT_PRICE_CENTS_XPATH = ".//*[@data-testid='price-cents']";
    private static final String PRODUCT_SIZE_XPATH = ".//*[@data-testid='product-subtitle']";

    // missing Sec-Fetch-User, Upgrade-Insecure-Requests

    private static final String[] GENERIC_HEADERS = {
        "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/153.0.0.0 Safari/537.36",
        "Accept-Language", "en-US",
        "Sec-Ch-Ua", "\"Chromium\";v=\"154\", \"Google Chrome\";v=\"154\", \"Not A(Brand\";v=\"99\"",
        "Sec-Ch-Ua-Mobile", "?0",
        "Sec-Ch-Ua-Platform", "\"Windows\""
    };

    private static final String[] GET_HEADERS = {
        "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Sec-Fetch-Dest", "document",
        "Sec-Fetch-Mode", "navigate",
        "Sec-Fetch-Site", "none"
    };

    private static final String[] POST_HEADERS = {
        "Accept", "*/*",
        "Sec-Fetch-Dest", "empty",
        "Sec-Fetch-Mode", "cors",
        "Content-Type", "application/json"
    };

    private final ProductRepository productRepository;

    private final ScraperDatabaseService databaseService;

    private final ObjectMapper mapper = new ObjectMapper();

    private final CookieManager cookieManager = new CookieManager();
    private final HttpClient scraperClient;

    private String paknsaveSessionAuth;

    @Value ("classpath:scrape_targets_min.json")
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

        try {
            paknsaveSessionAuth = paknsaveInit();
        } catch (IllegalStateException exc) {
            paknsaveSessionAuth = null;
        }
        
    }

    /**
     * Runs immediately when the app starts.
     * Checks if the database is empty and populates it if true.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void checkAndScrapeOnStartup() {
        if (productRepository.count() == 0 || bypassTimeDebug) {
            executeScraping();
        }
    }

    /**
     * Runs every 2 days.
     * initialDelayString prevents running immediately on startup alongside the event listener.
     */
    @Scheduled(fixedRateString = "P2D", initialDelayString = "P2D")
    public void scheduledScrape() {
        if (paknsaveSessionAuth == null) {
            System.err.println("Auth token error. Early termination...");
            return; // early terminate
        }
        executeScraping();
    }

    /**
     * Main scraper method. Accomplishes the following: <br>
     * - Deletes the existing store prices from database, <br>
     * - Reads scrape_targets.json for links and store data to scrape from, <br>
     * - For each store, sets store via POST request (and adds if missing), and scrapes each supermarket link, <br>
     * - Processes scraped data into Product objects and StorePrice objects and puts into database. <br>
     */
    private synchronized void executeScraping() {
        // firstly wipe storeprices and generate cache from noted products
        databaseService.clearPrices();
        databaseService.generateProductCache();
        // open json file, parsed as object?
        try (InputStream inputStream = targets.getInputStream()) {
            // iterate over reach supermarket brand (e.g. paknsave, new world, etc.)
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

                    System.out.println("Store initialised: " + currentStore.getName());

                    scrapeLinks(scraperData.links(), currentStore);

                }
                System.out.println("All stores for " + scraperData.supermarket() + " completed.");
            }
        } catch (IOException exc) {
            System.err.println("Some IO issue, " + exc.getLocalizedMessage());
        } 
    }

    /**
     * Helper method to introduce delay, as basic rate-limit prevention mechanism.
     */
    private void sleepRandom() {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(1000, 3000));
        } catch (InterruptedException exc) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * builds an HttpRequest from the provided link and return the request object.
     * @param link the link to GET from
     * @return the HttpRequest object with populated headers
     */
    private HttpRequest makeRequest(String link) {
        return HttpRequest.newBuilder()
            .uri(URI.create(link))
            .headers(GENERIC_HEADERS)
            .headers(GET_HEADERS)
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
            sleepRandom(); // apply random pause for no rate limiting
            Optional<HttpResponse<String>> responseOptional = getWebResponse(link);
            if (responseOptional.isEmpty()) {
                System.err.println("Link failed, skipping: " + link);
                continue;
            }
            Document doc = Jsoup.parse(responseOptional.get().body());
            List<ProductInfo> productData = extractProducts(doc);

            for (ProductInfo productInfo : productData) {
                Product product = databaseService.createOrGetProduct(productInfo.productName(), productInfo.productSize());
                StorePrice productPrice = new StorePrice(product, store, productInfo.price());

                databaseService.saveProductPrice(productPrice);
            }
            System.out.println("Link fully scraped: " + link);
        }
    }

    // TODO refactor the request methods to a unified instance?

    /**
     * Helper method for processing GET body without repeated try-catch patterns. Defaults to reading body as String.
     * @param link the URL to GET
     * @return the HttpResponse result, else Optional.empty()
     */
    private Optional<HttpResponse<String>> getWebResponse(String link) {
        return getWebResponse(link, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Helper method for processing GET body without repeated try-catch patterns.
     * @param link the URL to GET
     * @param handler The BodyHandler, which interprets response body.
     * @return the HttpResponse result, else Optional.empty()
     */
    private <T> Optional<HttpResponse<T>> getWebResponse(String link, BodyHandler<T> handler) {
        try {
            return Optional.of(scraperClient.send(makeRequest(link), handler));
        } catch (InterruptedException exc) { // propagate interruption signal
            System.err.println("Caught interrupt from GET " + link + ", no data returned.");
            Thread.currentThread().interrupt();
        } catch (IOException exc) {
            System.err.println("Caught IO error from GET " + link + ", no data returned.");
        }
        return Optional.empty();
    }

    /**
     * Encapsulates POST request sending with error checking.
     * @param <T> The return type of the HttpResponse, based on expected read (e.g. String or discarding)
     * @param request The HttpRequest to sent by client
     * @param handler The BodyHandler type, how the response data should be interpreted
     * @return If no errors, the HttpResponse with valid data. Else, Optional.empty()
     */
    private <T> Optional<HttpResponse<T>> postWebResponse(HttpRequest request, BodyHandler<T> handler) {
        try {
            return Optional.of(scraperClient.send(request, handler));
        } catch (InterruptedException exc) {
            System.err.println("Caught interrupt from GET " + request.uri().toString() + ", no data returned.");
            Thread.currentThread().interrupt();
        } catch (IOException exc) {
            System.err.println("Caught IO error from GET " + request.uri().toString() + ", no data returned.");
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
                System.err.println("Price not found for product " + name + ", skipping...");
                continue;
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
        getWebResponse(PAKNSAVE_ADDRESS, HttpResponse.BodyHandlers.discarding());

        HttpRequest authRequest = HttpRequest.newBuilder()
            .uri(URI.create("https://www.paknsave.co.nz/api/user/get-current-user"))
            .headers(GENERIC_HEADERS)
            .headers(POST_HEADERS)
            .header("Sec-Fetch-Site", "same-origin")
            .header("Origin", PAKNSAVE_ADDRESS)
            .header("Referer", PAKNSAVE_ADDRESS)
            .POST(HttpRequest.BodyPublishers.ofString("{}"))
            .build();

        Optional<HttpResponse<String>> authOptional = postWebResponse(authRequest, HttpResponse.BodyHandlers.ofString());
        if (authOptional.isEmpty()) {
            throw new IllegalStateException("Auth token cannot be established.");
        }
        try {
            JsonNode authNode = mapper.readTree(authOptional.get().body());
            return authNode.get("access_token").asText();
        } catch (JsonProcessingException exc) {
            throw new IllegalStateException("Auth token cannot be established.");
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
            .headers(GENERIC_HEADERS)
            .headers(POST_HEADERS)
            .header("Sec-Fetch-Site", "same-site")
            .header("Authorization", "Bearer " + paknsaveSessionAuth)
            .header("Origin", PAKNSAVE_ADDRESS)
            .header("Referer", PAKNSAVE_ADDRESS)
            .POST(HttpRequest.BodyPublishers.ofString("{}"))
            .build();
        Optional<HttpResponse<Void>> responseOptional = postWebResponse(req, HttpResponse.BodyHandlers.discarding());
        return responseOptional.isPresent();
        
    }
}

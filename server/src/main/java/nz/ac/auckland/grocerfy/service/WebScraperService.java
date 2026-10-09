package nz.ac.auckland.grocerfy.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.data.util.Pair;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import nz.ac.auckland.grocerfy.dto.ProductInfo;
import nz.ac.auckland.grocerfy.dto.ScraperConfig;
import nz.ac.auckland.grocerfy.model.Allergen;
import nz.ac.auckland.grocerfy.model.Dietary;
import nz.ac.auckland.grocerfy.model.Product;
import nz.ac.auckland.grocerfy.model.Store;
import nz.ac.auckland.grocerfy.model.StorePrice;
import nz.ac.auckland.grocerfy.scraper.NewWorldScraper;
import nz.ac.auckland.grocerfy.scraper.PaknsaveScraper;
import nz.ac.auckland.grocerfy.scraper.SupermarketScraper;
import nz.ac.auckland.grocerfy.util.HttpUtils;

@Service
public class WebScraperService {
    private boolean bypassTimeDebug = false;

    private static final int SCRAPE_DELAY_MIN = 1000;
    private static final int SCRAPE_DELAY_MAX = 3000;

    private static final Map<String, SupermarketScraper> scrapers = Map.of(
        "Pak'nSave", new PaknsaveScraper(),
        "New World", new NewWorldScraper()
    );

    private SupermarketScraper currentScraper;

    private final ScraperDatabaseService databaseService;

    private final ObjectMapper mapper = new ObjectMapper();

    @Value ("classpath:scrape_targets.json")
    private Resource targets;

    @Autowired 
    public WebScraperService(
        ScraperDatabaseService databaseService
    ) {
        this.databaseService = databaseService;
    }

    /**
     * Runs immediately when the app starts.
     * Checks if the database is empty and populates it if true.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void checkAndScrapeOnStartup() {
        if (databaseService.isDatabaseNew() || bypassTimeDebug) {
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
                currentScraper = scrapers.get(scraperData.supermarket());
                currentScraper.setupCookies();

                for (Map<String, String> storeInfo : scraperData.branches()) {
                    // change client's store region
                    if (!currentScraper.changeStore(storeInfo.get("region_cookie"))) {
                        // error specifics output in changeStore()
                        continue;
                    }

                    Store currentStore = databaseService.addStore(
                        scraperData.supermarket() + " " + storeInfo.get("store_name"),
                        storeInfo.get("store_name"),
                        storeInfo.get("address"),
                        storeInfo.get("region_cookie"));

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
    private void randomRequestDelay() {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(SCRAPE_DELAY_MIN, SCRAPE_DELAY_MAX + 1L));
        } catch (InterruptedException exc) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Scrapes provided links given a Store object to correspond the prices to.
     * @param links the list of links to scrape
     * @param store the store to map prices to (regional pricing)
     */
    private void scrapeLinks(List<String> links, Store store) {
        for (String link : links) {
            randomRequestDelay(); // apply random pause for no rate limiting

            HttpRequest productLink = HttpRequest.newBuilder()
                .uri(URI.create(link))
                .headers(HttpUtils.getGenericHeaders())
                .headers(HttpUtils.getGetHeaders())
                .GET()
                .build();

            HttpResponse<String> response = currentScraper.sendRequestWithAuth(
                productLink, 
                HttpResponse.BodyHandlers.ofString()
            );

            // reads html dom and extracts all products into DTO
            Document doc = Jsoup.parse(response.body());
            List<ProductInfo> productData = currentScraper.extractProducts(doc);

            // check each product exists (add to db if doesn't), and map it to store and price value
            for (ProductInfo productInfo : productData) {
                randomRequestDelay();
                Product product = databaseService.createOrGetProduct(
                    productInfo.productName(),
                    productInfo.productSize(),
                    () -> {
                        Pair<Set<Allergen>, Set<Dietary>> allergenDiet = currentScraper.getProductInfo(
                            productInfo.productId(),
                            store.getCode());
                        return new Product(
                            productInfo.productName(),
                            productInfo.productSize(), 
                            allergenDiet.getFirst(), 
                            allergenDiet.getSecond());
                    }    
                );
                
                StorePrice productPrice = new StorePrice(product, store, productInfo.price());

                databaseService.saveProductPrice(productPrice);
            }
            System.out.println("Link fully scraped: " + link);
        }
    }

}

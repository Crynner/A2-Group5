package nz.ac.auckland.grocerfy.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.springframework.transaction.annotation.Transactional;

import nz.ac.auckland.grocerfy.dto.ProductInfo;
import nz.ac.auckland.grocerfy.model.Product;
import nz.ac.auckland.grocerfy.model.Store;
import nz.ac.auckland.grocerfy.model.StorePrice;
import nz.ac.auckland.grocerfy.repository.ProductRepository;
import nz.ac.auckland.grocerfy.repository.StorePriceRepository;
import nz.ac.auckland.grocerfy.repository.StoreRepository;

@Service
public class WebScraperService {
    private static final String PRODUCT_XPATH = "//*[@itemtype='https://schema.org/Product']";
    private static final String PRODUCT_NAME_XPATH = ".//*[@itemprop='name']";
    private static final String PRODUCT_PRICE_DOLLARS_XPATH = ".//*[@data-testid='price-dollars']";
    private static final String PRODUCT_PRICE_CENTS_XPATH = ".//*[@data-testid='price-cents']";
    private static final String PRODUCT_SIZE_XPATH = ".//*[@data-testid='product-subtitle']";

    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final StorePriceRepository storePriceRepository;

    private final CookieManager cookieManager = new CookieManager();
    private final HttpClient scraperClient;

    private final String paknsaveSessionAuth;

    @Value("classpath:mainlinks.txt")
    private Resource mainlinksResource;

    @Autowired 
    public WebScraperService(
        ProductRepository productRepository,
        StoreRepository storeRepository,
        StorePriceRepository storePriceRepository
    ) {
        this.productRepository = productRepository;
        this.storeRepository = storeRepository;
        this.storePriceRepository = storePriceRepository;

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
        Store store = bootstrap(); // remove later outside testing
        HttpResponse<String> storeResponse = setPaknsaveStore("9cd8eb60-3222-4efc-bd7c-50e03e6a81a4");
        if (storeResponse == null) {
            System.err.println("Store POST closed unexpectedly, see above error for details.");
        }
        if (storeResponse.statusCode() >= 400) {
            System.out.println("Store POST failed unexpectedly, code: " + storeResponse.statusCode());
        }
        
        // open file with links
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(mainlinksResource.getInputStream(), StandardCharsets.UTF_8))) {
            for (String link: reader.lines().toList()) {
                try {
                    HttpResponse<String> res = scraperClient.send(makeRequest(link), HttpResponse.BodyHandlers.ofString());
                    // convert list of productinfo to list of product (remove price)
                    List<ProductInfo> rawProducts = extractProducts(res);
                    
                    List<Product> productItems = new ArrayList<>();
                    List<StorePrice> productPrices = new ArrayList<>();
                    for (ProductInfo productInfo : rawProducts) {
                        Product product = new Product(productInfo.productName(), productInfo.productSize());
                        StorePrice productPrice = new StorePrice(product, store, productInfo.price());

                        saveProduct(product, productPrice);
                        productItems.add(product);
                        productPrices.add(productPrice);
                    }
                    
                    // transactional method call - writes to db
                    // saveProducts(productItems, productPrices);
                    
                } catch (Exception exc) {
                    exc.printStackTrace();
                }
                
            }
        } catch (IOException exc) {
            exc.printStackTrace();
        }
        // market pre-session setup
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

    private List<ProductInfo> extractProducts(HttpResponse<String> response) {
        List<ProductInfo> products = new ArrayList<>();
        Document dom = Jsoup.parse(response.body());
        Elements productRaws = dom.selectXpath(PRODUCT_XPATH);
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

    @Transactional
    public void saveProducts(List<Product> products, List<StorePrice> prices) {
        productRepository.saveAllAndFlush(products);
        storePriceRepository.saveAllAndFlush(prices);
    }

    @Transactional
    public void saveProduct(Product product, StorePrice price) {
        productRepository.save(product);
        storePriceRepository.save(price);
    }

    /** test script, puts in one store for price checking */
    @Transactional
    public Store bootstrap() {
        Store store = new Store("royal oak paknsave", "auckland", "123 place road");
        storeRepository.save(store);
        return store;
    }

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
            Pattern re = Pattern.compile("\"access_token\":\"(\\S*?)\"");
            HttpResponse<String> authResponse = scraperClient.send(authRequest, HttpResponse.BodyHandlers.ofString());
            System.out.println(authResponse.body());
            Matcher match = re.matcher(authResponse.body());
            match.find();
            return match.group(1);
        } catch (Exception exc) {
            exc.printStackTrace();
            return null;
        }
    }

    private HttpResponse<String> setPaknsaveStore(String storeId) {
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
            return scraperClient.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (Exception exc) {
            exc.printStackTrace();
            return null;
        }
        
    }
}

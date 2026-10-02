package nz.ac.auckland.grocerfy.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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

import jakarta.transaction.Transactional;
import nz.ac.auckland.grocerfy.dto.ProductInfo;
import nz.ac.auckland.grocerfy.model.Product;
import nz.ac.auckland.grocerfy.repository.ProductRepository;
import nz.ac.auckland.grocerfy.repository.StorePriceRepository;
import nz.ac.auckland.grocerfy.repository.StoreRepository;

@Service
public class WebScraperService {
    private static final String PRODUCT_XPATH = "//*[@itemtype='https://schema.org/Product']";
    private static final String PRODUCT_NAME_XPATH = ".//*[@itemprop='name']";
    private static final String PRODUCT_PRICE_XPATH = ".//*[@itemprop='price']";
    private static final String PRODUCT_SIZE_XPATH = ".//*[@data-testid='product-subtitle']";

    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final StorePriceRepository storePriceRepository;

    private final HttpClient scraperClient;

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

        this.scraperClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();
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
     * 'P2D' is ISO-8601 duration format for 2 days.
     * initialDelayString prevents running immediately on startup alongside the event listener.
     */
    @Scheduled(fixedRateString = "P2D", initialDelayString = "P2D")
    public void scheduledScrape() {
        executeScraping();
    }

    private synchronized void executeScraping() {
        // open file with links
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(mainlinksResource.getInputStream(), StandardCharsets.UTF_8))) {
            for (String link: reader.lines().toList()) {
                try {
                    HttpResponse<String> res = scraperClient.send(makeRequest(link), HttpResponse.BodyHandlers.ofString());
                    List<Product> modelProducts = extractProducts(res)
                        .stream()
                        .map(productInfo -> new Product(
                            productInfo.productName(),
                            productInfo.productSize()
                        )).toList();
                    
                    // transactional method call - writes to db
                    saveProducts(modelProducts);
                    
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

            // "content" attribute associated with price number
            Element priceElement = product.selectXpath(PRODUCT_PRICE_XPATH).first();
            BigDecimal price = priceElement == null ? null : new BigDecimal(priceElement.attr("content"));

            Element sizeElement = product.selectXpath(PRODUCT_SIZE_XPATH).first();
            String size = sizeElement == null ? null : sizeElement.text().trim();

            products.add(new ProductInfo(name, price, size));
        }

        return products;
    }

    @Transactional
    private void saveProducts(List<Product> products) {
        productRepository.saveAllAndFlush(products);
    }
}

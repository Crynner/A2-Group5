package nz.ac.auckland.grocerfy.scraper;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.util.List;
import java.util.Set;

import org.jsoup.nodes.Document;
import org.springframework.data.util.Pair;

import com.fasterxml.jackson.databind.ObjectMapper;

import nz.ac.auckland.grocerfy.dto.ProductInfo;
import nz.ac.auckland.grocerfy.model.Allergen;
import nz.ac.auckland.grocerfy.model.Dietary;

public abstract class SupermarketScraper {

    protected final ObjectMapper mapper = new ObjectMapper();

    /**
     * Sets up necessary boilerplate data to be able to properly access the website without
     * getting 401s or 403s. For example, getting an auth token to use in all subsequent requests.
     */
    public abstract void setupCookies();

    /**
     * Refreshes the cookies as required. For cases of authentication, they typically have a
     * short-spanned expiry, where scraping may not be completed in time, therefore a refresh
     * is required.
     */
    public abstract void refreshCookies();

    /**
     * A wrapper function to HttpUtils.sendHttpRequest that on receiving an error message,
     * first attempts to refreshCookies; if still fails, throws an error (unlikely future attempts
     * will work, meaning scraping must end).
     * @param <T> The expected return type of the HttpResponse body content
     * @param request The HttpRequest object we send
     * @param handler The HttpResponse.BodyHandler method to return body content
     * as a specific type
     * @return
     * @see nz.ac.auckland.grocerfy.scraper.SupermarketScraper#refreshCookies()
     * @see nz.ac.auckland.grocerfy.util.HttpUtils#sendHttpRequest()
     */
    public abstract <T> HttpResponse<T> sendRequestWithAuth(HttpRequest request, BodyHandler<T> handler);

    /**
     * Changes the store the scraper is currently on; important because most stores have
     * regional pricing (product costs change from store to store), allowing proper
     * comparison.
     * @param storeData The store-specific data interpretable to change the store (e.g. store code)
     * @return true if the store successfully changes, else false
     */
    public abstract boolean changeStore(String storeData);

    /**
     * Given a catalogue page (many products), extracts the name, size, price, and id
     * of each product, packaging into the ProductInfo DTO.
     * @param pageData the DOM of the product page to scrape
     * @return a list of ProductInfo DTOs, from the provided html page
     */
    public abstract List<ProductInfo> extractProducts(Document pageData);

    /**
     * Accesses extra product info (their own info page) to further see allergen info, ingredients, etc.
     * @param productId the id for the product being examined.
     * @param storeUuid the specific store data/code (foodstuff namely, biased)
     * @return
     */
    public abstract Pair<Set<Allergen>, Set<Dietary>> getProductInfo(String productId, String storeUuid);
}

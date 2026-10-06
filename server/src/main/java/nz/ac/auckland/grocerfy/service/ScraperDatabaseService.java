package nz.ac.auckland.grocerfy.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nz.ac.auckland.grocerfy.model.Product;
import nz.ac.auckland.grocerfy.model.Store;
import nz.ac.auckland.grocerfy.model.StorePrice;
import nz.ac.auckland.grocerfy.repository.ProductRepository;
import nz.ac.auckland.grocerfy.repository.StorePriceRepository;
import nz.ac.auckland.grocerfy.repository.StoreRepository;

/**
 ScraperDatabaseService is the database service for the webscraper script, allowing for proper enforcement
 of the @Transactional annotation.
 */
@Service 
public class ScraperDatabaseService {
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final StorePriceRepository storePriceRepository;

    private final Map<String, Product> productCache = new HashMap<>();
    private final Set<StorePrice> priceCache = new HashSet<>();

    @Autowired 
    public ScraperDatabaseService(
        ProductRepository productRepository,
        StoreRepository storeRepository,
        StorePriceRepository storePriceRepository
    ) {
        this.productRepository = productRepository;
        this.storeRepository = storeRepository;
        this.storePriceRepository = storePriceRepository;
    }

    /**
     * Abstraction method to check if database is newly created and has no products, must always scrape
     * @return whether the database needs scraping.
     */
    public boolean isDatabaseNew() {
        return productRepository.count() == 0;
    }

    /**
     * Adds store, given constructor parameters, to database. Returns db store if already exists
     * @param name the store name
     * @param region the region of the store
     * @param address the address of the store (number and street)
     * @return
     */
    @Transactional 
    public Store addStore(String name, String region, String address) {
        Store existingStore = storeRepository.findByStoreName(name);
        if (existingStore != null) {
            return existingStore;
        }
        return storeRepository.save(
            new Store(name, region, address)
        );
    }

    /**
     * Saves store price to database if not cached (already exists), locally enforcing uniqueness.
     * @param price The StorePrice object to add.
     */
    @Transactional
    public void saveProductPrice(StorePrice price) {
        if (!priceCache.contains(price)) {
            storePriceRepository.save(price);
            priceCache.add(price);
        }
    }

    /**
     * Adds a product to the database if doesn't exist, else returns the existing object.
     * @param name the name of the product
     * @param size the size factor of the product
     * @return The product instance used in the database
     */
    @Transactional
    public Product createOrGetProduct(String name, String size) {
        // TODO convert to equals() and hashCode() implementation
        String cacheName = name + ":~:" + size;
        if (productCache.containsKey(cacheName)) {
            return productCache.get(cacheName);
        }
        Product newProduct = productRepository.save(new Product(name, size));
        productCache.put(cacheName, newProduct);
        return newProduct;
    }

    public void generateProductCache() {
        for (Product product : productRepository.findAll()) {
            String cacheName = product.getProductName() + ":~:" + product.getSize();
            productCache.put(cacheName, product);
        }
    }

    @Transactional
    public void clearPrices() {
        storePriceRepository.deleteAllInBatch();
    }
}

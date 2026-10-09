package nz.ac.auckland.grocerfy.service;
 
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
 
import java.math.BigDecimal;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
 
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
 
import nz.ac.auckland.grocerfy.model.Product;
import nz.ac.auckland.grocerfy.model.Store;
import nz.ac.auckland.grocerfy.model.StorePrice;
import nz.ac.auckland.grocerfy.repository.ProductRepository;
import nz.ac.auckland.grocerfy.repository.StorePriceRepository;
import nz.ac.auckland.grocerfy.repository.StoreRepository;
 

@ExtendWith(MockitoExtension.class)
class ScraperDatabaseServiceTest {
 
    @Mock ProductRepository productRepository;
    @Mock StoreRepository storeRepository;
    @Mock StorePriceRepository storePriceRepository;
 
    ScraperDatabaseService service;
    Product product;
    Store store;
 
    @BeforeEach
    void setUp() {
        service = new ScraperDatabaseService(productRepository, storeRepository, storePriceRepository);
        product = new Product("Milk", "2L", Set.of(), Set.of());
        store = new Store("New World Albany", "Albany", "1 Example St", "uuid-1");
    }
 
    @Test
    void sameNameAndSizeIsOnlyCreatedOnce() {
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        AtomicInteger supplierCalls = new AtomicInteger();
        Supplier<Product> supplier = () -> {
            supplierCalls.incrementAndGet();
            return new Product("Milk", "2L", Set.of(), Set.of());
        };
 
        Product first = service.createOrGetProduct("Milk", "2L", supplier);
        Product second = service.createOrGetProduct("Milk", "2L", supplier);
 
        assertSame(first, second);
        assertEquals(1, supplierCalls.get()); // matters: the supplier does a network request
        verify(productRepository, times(1)).save(any(Product.class));
    }
 
    @Test
    void addStoreReturnsExistingStoreWithoutSaving() {
        when(storeRepository.findByStoreName("New World Albany")).thenReturn(store);
 
        Store result = service.addStore("New World Albany", "Albany", "1 Example St", "uuid-1");
 
        assertSame(store, result);
        verify(storeRepository, never()).save(any(Store.class));
    }
}
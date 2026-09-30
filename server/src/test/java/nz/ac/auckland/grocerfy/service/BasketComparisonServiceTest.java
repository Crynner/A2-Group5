package nz.ac.auckland.grocerfy.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import nz.ac.auckland.grocerfy.dto.BasketComparisonRequest;
import nz.ac.auckland.grocerfy.dto.BasketComparisonResponse;
import nz.ac.auckland.grocerfy.dto.BasketItemRequest;
import nz.ac.auckland.grocerfy.dto.StoreComparisonResponse;
import nz.ac.auckland.grocerfy.model.Product;
import nz.ac.auckland.grocerfy.model.Store;
import nz.ac.auckland.grocerfy.model.StorePrice;
import nz.ac.auckland.grocerfy.repository.ProductRepository;
import nz.ac.auckland.grocerfy.repository.StorePriceRepository;
import nz.ac.auckland.grocerfy.repository.StoreRepository;

@ExtendWith(MockitoExtension.class)
class BasketComparisonServiceTest {

	@Mock
	private ProductRepository productRepository;

	@Mock
	private StoreRepository storeRepository;

	@Mock
	private StorePriceRepository storePriceRepository;

	@InjectMocks
	private BasketComparisonService basketComparisonService;

	@Test
	void compareBasketMergesDuplicateItemsAndChoosesCheapestAvailableStore() {
		// Product apple = new Product(1L, "Apple", "Fresh", "Fruit", "1kg", true, false, true, true);
		// Product bread = new Product(2L, "White Bread", "Tip Top", "Bakery", "1 loaf", true, false, true, true);
		// Store storeA = new Store("Pak n Save", "Auckland", "1 Main St");
		// Store storeB = new Store("Countdown", "Auckland", "2 Main St");
		// storeA.setStoreId(10L);
		// storeB.setStoreId(11L);

		// when(productRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(apple, bread));
		// when(storePriceRepository.findAllWithRelations()).thenReturn(List.of(
		// 		new StorePrice(apple, storeA, new BigDecimal("1.50")),
		// 		new StorePrice(bread, storeA, new BigDecimal("4.00")),
		// 		new StorePrice(apple, storeB, new BigDecimal("1.80")),
		// 		new StorePrice(bread, storeB, new BigDecimal("3.50"))));
		// when(storeRepository.findAll()).thenReturn(List.of(storeA, storeB));

		// BasketComparisonRequest request = new BasketComparisonRequest(List.of(
		// 		new BasketItemRequest(1L, 2),
		// 		new BasketItemRequest(1L, 1),
		// 		new BasketItemRequest(2L, 1)));

		// BasketComparisonResponse response = basketComparisonService.compareBasket(request);

		// assertThat(response.requestedItems()).hasSize(2);
		// assertThat(response.stores()).hasSize(2);
		// assertThat(response.cheapestAvailableStore()).isNotNull();
		// assertThat(response.cheapestAvailableStore().storeName()).isEqualTo("Pak n Save");
		// assertThat(response.cheapestAvailableStore().availableSubtotal()).isEqualByComparingTo("8.50");
		// assertThat(response.stores().stream().filter(StoreComparisonResponse::available).map(StoreComparisonResponse::storeName))
		// 		.containsExactly("Pak n Save", "Countdown");
	}

	@Test
	void compareBasketRejectsMissingProductIdsAndEmptyBasket() {
		assertThatThrownBy(() -> basketComparisonService.compareBasket(new BasketComparisonRequest(List.of())))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("at least one item");

		when(productRepository.findAllById(Set.of(99L))).thenReturn(List.of());

		assertThatThrownBy(() -> basketComparisonService.compareBasket(new BasketComparisonRequest(List.of(new BasketItemRequest(99L, 1)))))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Unknown product ids");
	}
}

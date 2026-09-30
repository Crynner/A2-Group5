package nz.ac.auckland.grocerfy;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import nz.ac.auckland.grocerfy.dto.BasketComparisonRequest;
import nz.ac.auckland.grocerfy.dto.BasketComparisonResponse;
import nz.ac.auckland.grocerfy.dto.BasketItemRequest;
import nz.ac.auckland.grocerfy.dto.MissingItemResponse;
import nz.ac.auckland.grocerfy.dto.ProductSearchResponse;
import nz.ac.auckland.grocerfy.dto.ProductSummaryResponse;
import nz.ac.auckland.grocerfy.dto.StoreComparisonResponse;
import nz.ac.auckland.grocerfy.dto.StoreLineItemResponse;
import nz.ac.auckland.grocerfy.model.Product;
import nz.ac.auckland.grocerfy.model.Store;
import nz.ac.auckland.grocerfy.model.StorePrice;

class BackendDtoAndModelTest {

	// @Test
	// void recordsExposeTheirFieldsAndModelDisplayHelpersFormatValues() {
	// 	BasketComparisonRequest request = new BasketComparisonRequest(List.of(new BasketItemRequest(1L, 2)));
	// 	BasketComparisonResponse response = new BasketComparisonResponse(
	// 			List.of(new ProductSummaryResponse(1L, "Apple")),
	// 			List.of(new StoreComparisonResponse(3L, "Pak n Save", "Auckland", "1 Main St", true,
	// 					new BigDecimal("3.50"),
	// 					List.of(new StoreLineItemResponse(1L, "Apple", 2, new BigDecimal("1.50"), new BigDecimal("3.00"))),
	// 					List.of())),
	// 			null,
	// 			null);
	// 	MissingItemResponse missing = new MissingItemResponse(2L, "Bread", 1);
	// 	ProductSearchResponse search = new ProductSearchResponse(4L, "Apple", "Fresh Apple", "Fresh", "Fruit", "1kg", true, false, true, true);
	// 	Product product = new Product(4L, "Apple", "Fresh", "Fruit", "1kg", true, false, true, true);
	// 	Store store = new Store("Local Market", "Auckland", "2 High St");
	// 	store.setStoreId(5L);
	// 	StorePrice price = new StorePrice(product, store, new BigDecimal("2.10"));

	// 	assertThat(request.items()).hasSize(1);
	// 	assertThat(response.requestedItems()).hasSize(1);
	// 	assertThat(missing.productName()).isEqualTo("Bread");
	// 	assertThat(search.displayName()).isEqualTo("Fresh Apple");
	// 	assertThat(product.getDisplayName()).isEqualTo("Fresh Apple");
	// 	assertThat(store.getDisplayName()).isEqualTo("Local Market - Auckland");
	// 	assertThat(price.getPriceLabel()).isEqualTo("$2.10");
	// }
}

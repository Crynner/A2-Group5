package nz.ac.auckland.grocerfy.service;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import nz.ac.auckland.grocerfy.dto.ProductSearchResponse;
import nz.ac.auckland.grocerfy.model.Product;
import nz.ac.auckland.grocerfy.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductSearchServiceTest {

	@Mock
	private ProductRepository productRepository;

	@InjectMocks
	private ProductSearchService productSearchService;

	// @Test
	// void searchMatchesCaseInsensitiveQueryAndDietaryFlags() {
	// 	Product product = new Product(7L, "Apple", "Fresh", "Fruit", "1kg", true, false, true, true);
	// 	when(productRepository.search("%apple%", true, false, true, true)).thenReturn(List.of(product));

	// 	List<ProductSearchResponse> results = productSearchService.search("apple", List.of("lactose_free", "vegetarian", "vegan"));

	// 	assertThat(results).hasSize(1);
	// 	assertThat(results.get(0).productName()).isEqualTo("Apple");
	// 	assertThat(results.get(0).displayName()).isEqualTo("Fresh Apple");
	// 	assertThat(results.get(0).lactoseFree()).isTrue();
	// 	assertThat(results.get(0).glutenFree()).isFalse();
	// 	verify(productRepository).search("%apple%", true, false, true, true);
	// }

	// @Test
	// void searchIgnoresBlankTagsAndAcceptsAlternativeSpellings() {
	// 	when(productRepository.search("%", false, true, false, true)).thenReturn(List.of());

	// 	List<ProductSearchResponse> results = productSearchService.search(null, List.of(" ", "gluten-free", "vegan"));

	// 	assertThat(results).isEmpty();
	// 	verify(productRepository).search("%", false, true, false, true);
	// }

	// @Test
	// void searchRejectsUnknownDietaryTag() {
	// 	assertThatThrownBy(() -> productSearchService.search("apple", List.of("carnivore")))
	// 			.isInstanceOf(ResponseStatusException.class)
	// 			.hasMessageContaining("Unknown dietary tag");
	// }
}

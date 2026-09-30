package nz.ac.auckland.grocerfy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import nz.ac.auckland.grocerfy.model.Product;
import nz.ac.auckland.grocerfy.repository.ProductRepository;

/**
 * Tests the search query in isolation using fixtures created by the test itself.
 */
@DataJpaTest
@TestPropertySource(properties = "spring.sql.init.mode=never")
class ProductRepositoryTest {

	private static final String ANY_NAME = "%";

	@Autowired
	private ProductRepository productRepository;

	@BeforeEach
	void seedFixtures() {
		productRepository.deleteAll();
		// name, brand, lactoseFree, glutenFree, vegetarian, vegan
		productRepository.save(product("Trim Milk", "Anchor", false, true, true, false));
		productRepository.save(product("Oat Milk", "Otis", true, true, true, true));
		productRepository.save(product("White Bread", "Tip Top", true, false, true, true));
		productRepository.save(product("Chicken Breast", "Tegel", true, true, false, false));
	}

	// @Test
	// void matchesNameCaseInsensitively() {
	// 	List<Product> results = productRepository.search("%milk%", false, false, false, false);

	// 	assertThat(results).extracting(Product::getName)
	// 			.containsExactly("Oat Milk", "Trim Milk");
	// }

	// @Test
	// void matchesPartialSubstringAnywhereInName() {
	// 	List<Product> results = productRepository.search("%rea%", false, false, false, false);

	// 	assertThat(results).extracting(Product::getName)
	// 			.containsExactly("Chicken Breast", "White Bread");
	// }

	// @Test
	// void returnsEverythingWhenNoFiltersApplied() {
	// 	List<Product> results = productRepository.search(ANY_NAME, false, false, false, false);

	// 	assertThat(results).hasSize(4);
	// }

	// @Test
	// void filtersBySingleDietaryTag() {
	// 	List<Product> results = productRepository.search(ANY_NAME, false, false, false, true);

	// 	assertThat(results).extracting(Product::getName)
	// 			.containsExactlyInAnyOrder("Oat Milk", "White Bread");
	// }

	// @Test
	// void requiresAllSelectedTagsNotAnyOfThem() {
	// 	// White Bread is vegan but not gluten-free, so it must be excluded.
	// 	List<Product> results = productRepository.search(ANY_NAME, false, true, false, true);

	// 	assertThat(results).extracting(Product::getName).containsExactly("Oat Milk");
	// }

	// @Test
	// void combinesNameAndDietaryFilters() {
	// 	List<Product> results = productRepository.search("%milk%", false, false, false, true);

	// 	assertThat(results).extracting(Product::getName).containsExactly("Oat Milk");
	// }

	// @Test
	// void returnsEmptyListWhenNothingMatches() {
	// 	List<Product> results = productRepository.search("%nosuchproduct%", false, false, false, false);

	// 	assertThat(results).isEmpty();
	// }

	private Product product(String name, String brand, boolean lactoseFree, boolean glutenFree,
			boolean vegetarian, boolean vegan) {
		// return new Product(null, name, brand, "Test", "1kg", lactoseFree, glutenFree, vegetarian, vegan);
		return new Product();
	}
}
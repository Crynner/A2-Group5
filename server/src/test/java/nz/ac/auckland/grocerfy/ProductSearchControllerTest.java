package nz.ac.auckland.grocerfy;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * End to end tests for the product search endpoint
 */
// @SpringBootTest
@AutoConfigureMockMvc
class ProductSearchControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void returnsWholeCatalogueWhenNoParametersGiven() throws Exception {
		mockMvc.perform(get("/api/product"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.productName == 'Apple')]").exists())
				.andExpect(jsonPath("$[?(@.productName == 'Chicken Breast')]").exists());
	}

	@Test
	void matchesProductNameCaseInsensitively() throws Exception {
		mockMvc.perform(get("/api/product").param("query", "PIZZA"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].productName").value("Pizza"))
				.andExpect(jsonPath("$[1].productName").value("Vegetarian Pizza"));
	}

	@Test
	void matchesPartialSubstringAnywhereInName() throws Exception {
		mockMvc.perform(get("/api/product").param("query", "rea"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.productName == 'Chicken Breast')]").exists())
				.andExpect(jsonPath("$[?(@.productName == 'White Bread')]").exists());
	}

	@Test
	void appliesEverySelectedDietaryTag() throws Exception {
		// White Bread is vegan but not gluten-free, so a strict AND must exclude it.
		mockMvc.perform(get("/api/product").param("dietary", "gluten_free").param("dietary", "vegan"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.glutenFree == false)]").isEmpty())
				.andExpect(jsonPath("$[?(@.vegan == false)]").isEmpty())
				.andExpect(jsonPath("$[?(@.productName == 'White Bread')]").isEmpty())
				.andExpect(jsonPath("$[?(@.productName == 'Apple')]").exists());
	}

	@Test
	void acceptsCommaSeparatedTagList() throws Exception {
		mockMvc.perform(get("/api/product").param("dietary", "gluten_free,vegan"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.glutenFree == false)]").isEmpty())
				.andExpect(jsonPath("$[?(@.vegan == false)]").isEmpty());
	}

	@Test
	void acceptsAlternativeTagSpellings() throws Exception {
		mockMvc.perform(get("/api/product").param("dietary", "gluten-free"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.glutenFree == false)]").isEmpty());
	}

	@Test
	void combinesNameAndDietaryFilters() throws Exception {
		mockMvc.perform(get("/api/product").param("query", "pizza").param("dietary", "vegetarian"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].productName").value("Vegetarian Pizza"));
	}

	@Test
	void returnsEmptyArrayWhenNothingMatches() throws Exception {
		mockMvc.perform(get("/api/product").param("query", "notarealproduct"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void rejectsUnknownDietaryTag() throws Exception {
		mockMvc.perform(get("/api/product").param("dietary", "carnivore"))
				.andExpect(status().isBadRequest());
	}
}
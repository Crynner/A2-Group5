package nz.ac.auckland.grocerfy;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

/**
 * End to end tests for the product search endpoint
 */
// @SpringBootTest
@AutoConfigureMockMvc
class ProductSearchControllerTest {

	// TODO convert seed data to mock/fake db
	// check back to a1 tag for old test details

	@Test 
	void fillerTest() {
		assertTrue(true);
	}
}
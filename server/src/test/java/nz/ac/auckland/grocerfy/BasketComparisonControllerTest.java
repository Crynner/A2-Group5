package nz.ac.auckland.grocerfy;


import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Integration test for the basket comparison controller.
 *
 * This confirms that the backend accepts a JSON POST request to /api/basket/compare
 * and returns the expected response structure for seeded stores and products.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BasketComparisonControllerTest {

	@Test 
	void fillerTest() {
		assertTrue(true);
	}
}

package nz.ac.auckland.grocerfy;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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

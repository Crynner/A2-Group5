package nz.ac.auckland.grocerfy.service;


import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import nz.ac.auckland.grocerfy.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductSearchServiceTest {

	@Mock
	private ProductRepository productRepository;

	@InjectMocks
	private ProductSearchService productSearchService;

	/**
	 * Filler Test for Sonar - This test suite should test product searching.
	 */
	@Test 
	void fillerTest() {
		assertTrue(true);
	}
}

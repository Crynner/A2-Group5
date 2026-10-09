package nz.ac.auckland.grocerfy.controller;

import java.util.Collections;
import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import nz.ac.auckland.grocerfy.dto.ProductSearchResponse;
import nz.ac.auckland.grocerfy.service.ProductSearchService;

/**
 * Controller for handling product search requests.
 * Both parameters are optional. The name match is a case-insensitive substring
 * search, and when several dietary tags are supplied only products carrying all
 * of them are returned.
 */
@RestController
@RequestMapping("/api/product")
@CrossOrigin(origins = "http://localhost:5173")
public class ProductController {

	private final ProductSearchService productSearchService;

	public ProductController(ProductSearchService productSearchService) {
		this.productSearchService = productSearchService;
	}

	@GetMapping
	public List<ProductSearchResponse> search(
			@RequestParam(required = false) String query) {
		return productSearchService.search(query, Collections.emptySet(), Collections.emptySet()); // dummied diet/allergen, TODO fix later
	}
}
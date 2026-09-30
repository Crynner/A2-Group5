package nz.ac.auckland.grocerfy.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nz.ac.auckland.grocerfy.dto.ProductSearchResponse;
import nz.ac.auckland.grocerfy.model.Product;
import nz.ac.auckland.grocerfy.repository.ProductRepository;

/**
 * Service backing the product search endpoint. It is responsible for querying the repository and mapping the results onto the response DTO. 
 * It also validates and normalises the caller's dietary tag names.
 */
@Service
public class ProductSearchService {

	private final ProductRepository productRepository;

	public ProductSearchService(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	/**
	 * Find products whose name contains the given query and which carry every one
	 * of the requested dietary tags.
	 * @param query   substring to match against the product name, case-insensitive
	 * @param dietary dietary tag names, matched products must carry all of them
	 * @return matching products ordered by name
	 */
	@Transactional(readOnly = true)
	public List<ProductSearchResponse> search(String query) {

		String pattern = (query == null || query.isBlank())
				? "%"
				: "%" + query.trim().toLowerCase() + "%";

		return productRepository.search(
				pattern)
				.stream()
				.map(this::toResponse)
				.toList();
	}

	private ProductSearchResponse toResponse(Product product) {
		return new ProductSearchResponse(
				product.getId(),
				product.getProductName(),
				product.getSize());
	}
}
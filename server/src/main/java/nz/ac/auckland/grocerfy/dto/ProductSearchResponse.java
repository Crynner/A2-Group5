package nz.ac.auckland.grocerfy.dto;

/**
 * The dietary flags are included so the client can render tag badges and show
 * why a product matched the selected filters.
 */
public record ProductSearchResponse(
		Long productId,
		String productName,
		String packageSize) {
}
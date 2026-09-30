package nz.ac.auckland.grocerfy.dto;

/**
 * Summary information for each product included in the request.
 * Used so the response can echo back the requested basket contents.
 */
public record ProductSummaryResponse(Long productId, String productName) {
}
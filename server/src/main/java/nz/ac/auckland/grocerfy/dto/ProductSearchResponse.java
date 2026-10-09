package nz.ac.auckland.grocerfy.dto;

import java.util.Set;


import nz.ac.auckland.grocerfy.model.Allergen;
import nz.ac.auckland.grocerfy.model.Dietary;

/**
 * The dietary flags are included so the client can render tag badges and show
 * why a product matched the selected filters.
 */
public record ProductSearchResponse(
		Long productId,
		String productName,
		String packageSize,
		Set<Allergen> allergens,
		Set<Dietary> dietInfo
	) {
}
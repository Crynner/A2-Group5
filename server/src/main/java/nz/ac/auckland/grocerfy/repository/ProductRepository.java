package nz.ac.auckland.grocerfy.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import nz.ac.auckland.grocerfy.model.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Search products by name substring.
     *
     * The name pattern is matched case-insensitively, callers pass an
     * already-lowercased LIKE pattern (for example "%milk%", or "%" to match
     * everything).
     * @param pattern     lowercased SQL LIKE pattern for the product name
     * @return matching products ordered by name
     */
    @Query("""
			SELECT p FROM Product p
			WHERE p.productName ILIKE :pattern
			ORDER BY p.productName
			""")
    List<Product> search(@Param("pattern") String pattern);
}
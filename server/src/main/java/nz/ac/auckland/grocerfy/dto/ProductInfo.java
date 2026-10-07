package nz.ac.auckland.grocerfy.dto;

import java.math.BigDecimal;

public record ProductInfo(String productName, BigDecimal price, String productSize, String productId) {
    
}

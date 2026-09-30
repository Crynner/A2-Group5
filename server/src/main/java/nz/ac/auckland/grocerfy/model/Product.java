package nz.ac.auckland.grocerfy.model;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity 
@Immutable 
@Table(name = "products")
public class Product {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String productName;

    @Column(nullable = false, length = 20)
    private String size;

    @ElementCollection
    @Enumerated(EnumType.STRING)
    private Set<Allergen> allergens = new HashSet<>(); 

    public Product() { }

    public Product(String productName, String size) {
        this.productName = productName;
        this.size = size;
    }

    public Long getId() {
        return id;
    }
    
    public String getProductName() {
        return productName;
    }

    public String getSize() {
        return size;
    }
}

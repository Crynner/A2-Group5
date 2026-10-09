package nz.ac.auckland.grocerfy.model;

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
import jakarta.persistence.UniqueConstraint;

@Entity 
@Immutable 
@Table(name = "products",
    uniqueConstraints = @UniqueConstraint(columnNames = {"productName", "size"})
)
public class Product {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String productName;

    @Column(nullable = false, length = 20)
    private String size;

    @ElementCollection
    @Enumerated(EnumType.STRING)
    private Set<Allergen> allergens;
    
    @ElementCollection 
    @Enumerated (EnumType.STRING)
    private Set<Dietary> dietInfo;

    public Product() { }

    public Product(String productName, String size, Set<Allergen> allergens, Set<Dietary> dietInfo) {
        this.productName = productName;
        this.size = size;
        this.allergens = allergens;
        this.dietInfo = dietInfo;
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

    public Set<Allergen> getAllergens() {
        return allergens;
    }

    public Set<Dietary> getDietInfo() {
        return dietInfo;
    }
}

package nz.ac.auckland.grocerfy.model;

import java.math.BigDecimal;
import java.util.Objects;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Immutable
@Table(name = "store_prices",
    uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "store_id"})
)
public class StorePrice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    private BigDecimal price;

    public StorePrice() { }

    public StorePrice(Product product, Store store, BigDecimal price) {
        this.product = product;
        this.store = store;
        this.price = price;
    }

    public Product getProduct() {
        return product;
    }

    public Store getStore() {
        return store;
    }

    public BigDecimal getPrice() {
        return price;
    }

    @Override 
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof StorePrice)) {
            return false;
        }

        StorePrice compPrice = (StorePrice) obj;
        return (getProduct().getId().equals(compPrice.getProduct().getId())
                && getStore().getId().equals(compPrice.getStore().getId()));
    }

    @Override 
    public int hashCode() {
        return Objects.hash(getProduct(), getStore());
    }
}

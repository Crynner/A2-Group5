package nz.ac.auckland.grocerfy.model;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Immutable
@Table(name = "stores")
public class Store {
    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String storeName;

    @Column(nullable = false)
    private String region; // change to enum later?

    @Column(nullable = false)
    private String address; // composite/record?

    @Column(nullable = false)
    private String storeCode;

    public Store() { }

    public Store(String name, String region, String address, String code) {
        this.storeName = name;
        this.region = region;
        this.address = address;
        this.storeCode = code;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return storeName;
    }

    public String getRegion() {
        return region;
    }

    public String getAddress() {
        return address;
    }

    public String getCode() {
        return storeCode;
    }

}

package nz.ac.auckland.grocerfy.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import nz.ac.auckland.grocerfy.model.Store;

public interface StoreRepository extends JpaRepository<Store, Long> {
    Store findByStoreName(String storeName);
}
package com.abd.ecommerce.repository;

import org.springframework.data.repository.CrudRepository;

import com.abd.ecommerce.model.Product;

public interface ProductRepository extends CrudRepository<Product, Long> {
}

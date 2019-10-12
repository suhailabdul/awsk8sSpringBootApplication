package com.abd.ecommerce.repository;

import org.springframework.data.repository.CrudRepository;

import com.abd.ecommerce.model.Order;

public interface OrderRepository extends CrudRepository<Order, Long> {
}

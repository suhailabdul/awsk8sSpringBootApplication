package com.abd.ecommerce.repository;

import org.springframework.data.repository.CrudRepository;

import com.abd.ecommerce.model.OrderProduct;
import com.abd.ecommerce.model.OrderProductPK;

public interface OrderProductRepository extends CrudRepository<OrderProduct, OrderProductPK> {
}

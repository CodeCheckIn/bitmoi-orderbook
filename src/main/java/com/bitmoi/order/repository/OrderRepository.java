package com.bitmoi.order.repository;

import com.bitmoi.order.domain.Order;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends ReactiveCrudRepository<Order,Long> {
}

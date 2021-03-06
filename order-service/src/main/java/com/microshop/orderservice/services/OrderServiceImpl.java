package com.microshop.orderservice.services;

import com.microshop.orderservice.dto.CustomerDTO;
import com.microshop.orderservice.dto.OrderDTO;
import com.microshop.orderservice.dto.PaymentDTO;
import com.microshop.orderservice.entity.Order;
import com.microshop.orderservice.repositories.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CustomerService customerService;
    private final PaymentService paymentService;

    public OrderServiceImpl(final OrderRepository orderRepository, final CustomerService customerService, final PaymentService paymentService) {
        this.orderRepository = orderRepository;
        this.customerService = customerService;
        this.paymentService = paymentService;
    }

    @Override
    public Iterable<Order> findAll() {
        return orderRepository.findAll();
    }

    @Override
    public OrderDTO findById(final Long id) {
        Order order = findPrunedById(id);
        if (order == null) {
            return null;
        }
        CustomerDTO customer = customerService.findById(order.getCustomerId());
        PaymentDTO payment = paymentService.findById(order.getPaymentId());
        return new OrderDTO(order.getId(), order.getStatus(), customer, payment);
    }

    @Override
    public Order findPrunedById(final Long id) {
        return orderRepository.findById(id).orElse(null);
    }

    @Override
    public Iterable<Order> findByCustomerId(final Long customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    @Override
    public Order save(final Order order) {
        return orderRepository.save(order);
    }
}

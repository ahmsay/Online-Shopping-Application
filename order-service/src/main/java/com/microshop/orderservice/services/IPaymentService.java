package com.microshop.orderservice.services;

import com.microshop.orderservice.wrapper.Payment;

public interface IPaymentService {

    Payment findById(Long id);
}

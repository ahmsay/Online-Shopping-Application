package com.microshop.orderservice.wrapper;

public class Payment {

    private Long id;
    private Double totalCharge;

    public Payment() {}

    public Long getId() {
        return id;
    }

    public Double getTotalCharge() {
        return totalCharge;
    }
}
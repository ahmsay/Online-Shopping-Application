package com.shopping.inventoryservice.services;

import com.shopping.inventoryservice.entity.Product;

import java.util.List;

public interface IProductService {

    List<Product> getAllProducts();

    Product getProductById(long id);

    List<Product> getProductsOfPayment(long paymentId);
}

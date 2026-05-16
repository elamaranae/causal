package com.causal.product;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductController {

  @GetMapping("/products")
  public Product getProduct() {
    return new Product(1L, "Laptop", "Live Reload");
  } 

  @GetMapping("/products/{id}")
  public Product getProduct(@PathVariable("id") Long id) {
    return new Product(1L, "Laptop", "A computer on the lap.");
  } 
}

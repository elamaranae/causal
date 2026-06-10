package com.causal.product.controller;

import com.causal.product.config.GlobalExceptionHandler;
import com.causal.product.config.SecurityConfig;
import com.causal.product.dto.response.ProductListingResponse;
import com.causal.product.dto.response.ProductShowResponse;
import com.causal.product.service.ProductService;
import com.causal.product.service.SkuService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({ProductController.class, SkuController.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private SkuService skuService;

    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwt() {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(j -> j.subject("1").claim("email", "test@test.com"));
    }

    @Test
    void getProduct_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/products/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getProduct_found_returns200() throws Exception {
        ProductShowResponse response = new ProductShowResponse(1L, "Widget", "A widget", null, 1L, null, Map.of(), 1L, List.of());
        when(productService.getProduct(1L)).thenReturn(response);

        mockMvc.perform(get("/products/1").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Widget"));
    }

    @Test
    void getProduct_notFound_returns404() throws Exception {
        when(productService.getProduct(99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        mockMvc.perform(get("/products/99").with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void trending_returns200() throws Exception {
        when(productService.getTrendingProducts()).thenReturn(List.of());

        mockMvc.perform(get("/products/trending").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void filter_requiresCategoryId() throws Exception {
        mockMvc.perform(get("/products/filter").with(jwt()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void filter_withCategoryId_returns200() throws Exception {
        Page<ProductListingResponse> page = new PageImpl<>(List.of());
        when(productService.filterProducts(eq(1L), any())).thenReturn(page);

        mockMvc.perform(get("/products/filter?categoryId=1").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void skusBulk_validRequest_returns200() throws Exception {
        when(skuService.getSkus(List.of(1L, 2L))).thenReturn(List.of());

        mockMvc.perform(post("/products/skus/bulk")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ids": [1, 2]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void skusBulk_emptyIds_returns400() throws Exception {
        mockMvc.perform(post("/products/skus/bulk")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ids": []}
                                """))
                .andExpect(status().isBadRequest());
    }
}

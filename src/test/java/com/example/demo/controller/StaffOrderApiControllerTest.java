package com.example.demo.controller;

import com.example.demo.config.JpaConfig;
import com.example.demo.dto.OrderDTO;
import com.example.demo.entity.enums.OrderStatus;
import com.example.demo.service.CatalogService;
import com.example.demo.repository.FollowRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.CartService;
import com.example.demo.service.OrderService;
import com.example.demo.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = {StaffOrderApiController.class},
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaConfig.class)
)
class StaffOrderApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @MockBean
    private CatalogService catalogService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private CartService cartService;

    @MockBean
    private FollowRepository followRepository;

    @MockBean
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "staff@example.com", roles = {"STAFF"})
    @DisplayName("STAFF can update order status via API")
    void staffCanUpdateStatus() throws Exception {
        OrderDTO mock = new OrderDTO();
        given(orderService.updateOrderStatus(eq("ORD123"), any(OrderStatus.class))).willReturn(mock);

        var body = objectMapper.writeValueAsString(java.util.Map.of("status", "PROCESSING"));
        mockMvc.perform(put("/api/staff/orders/ORD123/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }
}



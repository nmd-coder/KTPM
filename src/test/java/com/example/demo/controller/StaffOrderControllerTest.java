package com.example.demo.controller;

import com.example.demo.config.JpaConfig;
import com.example.demo.service.CatalogService;
import com.example.demo.repository.FollowRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.CartService;
import com.example.demo.service.OrderService;
import com.example.demo.service.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@WebMvcTest(
    controllers = {StaffOrderController.class},
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaConfig.class)
)
class StaffOrderControllerTest {

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

    @BeforeEach
    void setUp() {
        given(orderService.getAllOrders(any(Pageable.class)))
                .willReturn(Page.empty(PageRequest.of(0, 20)));
    }

    @Test
    @DisplayName("deny anonymous access to staff orders page")
    void anonymousDenied() throws Exception {
        mockMvc.perform(get("/staff/orders"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "staff@example.com", roles = {"STAFF"})
    @DisplayName("allow STAFF to access staff orders page")
    void staffCanAccess() throws Exception {
        mockMvc.perform(get("/staff/orders"))
                .andExpect(status().isOk());
    }
}



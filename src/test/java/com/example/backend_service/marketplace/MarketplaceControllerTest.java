package com.example.backend_service.marketplace;

import com.example.backend_service.marketplace.controller.MarketplaceController;
import com.example.backend_service.marketplace.dto.MarketplaceItemResponse;
import com.example.backend_service.marketplace.service.MarketplaceService;
import com.example.backend_service.marketplace.service.SellerJwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class MarketplaceControllerTest {

    private MarketplaceService marketplaceService;
    private SellerJwtService sellerJwtService;
    private MarketplaceController controller;
    private MockMvc mockMvc;
    private ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        marketplaceService = Mockito.mock(MarketplaceService.class);
        sellerJwtService = Mockito.mock(SellerJwtService.class);
        controller = new MarketplaceController();
        com.example.backend_service.TestUtils.setField(controller, "marketplaceService", marketplaceService);
        com.example.backend_service.TestUtils.setField(controller, "sellerJwtService", sellerJwtService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getAllItems_returnsOk() throws Exception {
        when(marketplaceService.getAllItems()).thenReturn(List.of(new MarketplaceItemResponse()));
        mockMvc.perform(get("/api/marketplace/items")).andExpect(status().isOk());
    }
}

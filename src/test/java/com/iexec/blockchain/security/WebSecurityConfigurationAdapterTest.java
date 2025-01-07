package com.iexec.blockchain.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(MockitoExtension.class)
class WebSecurityConfigurationAdapterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testSwaggerUiAccess() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void testConfigChainAccess() throws Exception {
        mockMvc.perform(get("/config/chain"))
                .andExpect(status().isOk());
    }

    @Test
    void testProtectedEndpointWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/protected-endpoint"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void testProtectedEndpointWithAuthentication() throws Exception {
        mockMvc.perform(get("/metrics"))
                .andExpect(status().isOk());
    }
}

package com.example.backend_service.todo;

import com.example.backend_service.AzureBlobService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class TodoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper mapper;

    @MockBean
    private AzureBlobService azureBlobService;

    @org.springframework.security.test.context.support.WithMockUser
    @Test
    void createAndGetTodos_endToEnd() throws Exception {
        Todo t = new Todo();
        t.setStudentId(123L);
        t.setTitle("Integration Todo");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/todos")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(t)))
                .andExpect(status().isOk());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/todos/student/123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentId").value(123));
    }
}

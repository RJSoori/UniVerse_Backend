package com.example.backend_service.jobhub;

import com.example.backend_service.jobhub.controller.JobHubController;
import com.example.backend_service.jobhub.dto.JobRequest;
import com.example.backend_service.jobhub.enums.JobStatus;
import com.example.backend_service.jobhub.model.Job;
import com.example.backend_service.jobhub.model.Recruiter;
import com.example.backend_service.jobhub.repository.JobRepository;
import com.example.backend_service.jobhub.repository.RecruiterRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class JobHubControllerTest {

    private JobRepository jobRepository;
    private RecruiterRepository recruiterRepository;
    private JobHubController controller;
    private MockMvc mockMvc;
    private ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        jobRepository = Mockito.mock(JobRepository.class);
        recruiterRepository = Mockito.mock(RecruiterRepository.class);
        controller = new JobHubController();
        // inject mocks via reflection since controller uses field injection
        com.example.backend_service.TestUtils.setField(controller, "jobRepository", jobRepository);
        com.example.backend_service.TestUtils.setField(controller, "recruiterRepository", recruiterRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void postJob_withVerifiedRecruiter_returnsOk() throws Exception {
        Recruiter r = new Recruiter();
        r.setId(5L);
        r.setStatus(com.example.backend_service.RecruiterStatus.VERIFIED);
        when(recruiterRepository.findById(5L)).thenReturn(Optional.of(r));

        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> {
            Job j = inv.getArgument(0);
            j.setId(10L);
            return j;
        });

        JobRequest req = new JobRequest("Dev","desc","req","skills","0","remote","fulltime","now",5L);

        mockMvc.perform(post("/api/jobs/post")
                .contentType("application/json")
                .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void getAllJobs_callsRepository() throws Exception {
        when(jobRepository.findByStatus(JobStatus.APPROVED)).thenReturn(List.of(new Job()));
        mockMvc.perform(get("/api/jobs/all")).andExpect(status().isOk());
    }
}

package com.example.backend_service.jobhub;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.backend_service.jobhub.model.Job;
import com.example.backend_service.jobhub.model.Recruiter;
import com.example.backend_service.jobhub.repository.JobRepository;
import com.example.backend_service.jobhub.repository.RecruiterRepository;


public class JobRepositoryTest {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private RecruiterRepository recruiterRepository;

    @Test
    void saveAndFindByRecruiter() {
        Recruiter r = new Recruiter();
        r.setCompanyName("Acme Co");
        r.setEmail("recruiter@acme.test");
        recruiterRepository.save(r);

        Job job = new Job();
        job.setTitle("Software Engineer");
        job.setDescription("Develop features");
        job.setRecruiter(r);
        jobRepository.save(job);

        List<Job> byRecruiter = jobRepository.findByRecruiterId(r.getId());
        assertThat(byRecruiter).isNotEmpty();
        assertThat(byRecruiter.get(0).getTitle()).isEqualTo("Software Engineer");

        var found = jobRepository.findByIdAndRecruiterId(job.getId(), r.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getRecruiter().getId()).isEqualTo(r.getId());
    }
}

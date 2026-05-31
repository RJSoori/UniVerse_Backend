package com.example.backend_service.schedule;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class ScheduleEventRepositoryTest {

    @Autowired
    private ScheduleEventRepository repository;

    @Test
    void findByStudentIdOrderByDateAsc_returnsEvents() {
        ScheduleEvent e1 = new ScheduleEvent();
        e1.setStudentId(2L);
        e1.setTitle("Event A");
        e1.setDate(LocalDate.now().plusDays(1));

        ScheduleEvent e2 = new ScheduleEvent();
        e2.setStudentId(2L);
        e2.setTitle("Event B");
        e2.setDate(LocalDate.now().plusDays(2));

        repository.save(e2);
        repository.save(e1);

        List<ScheduleEvent> events = repository.findByStudentIdOrderByDateAsc(2L);
        assertThat(events).hasSize(2);
        assertThat(events.get(0).getTitle()).isEqualTo("Event A");
    }
}

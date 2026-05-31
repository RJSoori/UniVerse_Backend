package com.example.backend_service.habits;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class PersonalHabitRepositoryTest {

    @Autowired
    private PersonalHabitRepository repository;

    @Test
    void findByStudentId_returnsSaved() {
        PersonalHabit h = new PersonalHabit();
        h.setStudentId(7L);
        h.setName("Read 30m");
        repository.save(h);

        List<PersonalHabit> found = repository.findByStudentId(7L);
        assertThat(found).isNotEmpty();
        assertThat(found.get(0).getName()).isEqualTo("Read 30m");
    }
}

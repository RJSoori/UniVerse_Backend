package com.example.backend_service.todo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class TodoRepositoryTest {

    @Autowired
    private TodoRepository repository;

    @Test
    void saveAndFindByStudentId() {
        Todo t = new Todo();
        t.setStudentId(99L);
        t.setTitle("Test Todo");
        t.setDescription("Do something");
        repository.save(t);

        List<Todo> todos = repository.findByStudentId(99L);
        assertThat(todos).isNotEmpty();
        assertThat(todos.get(0).getTitle()).isEqualTo("Test Todo");

        repository.deleteByStudentId(99L);
        assertThat(repository.findByStudentId(99L)).isEmpty();
    }
}

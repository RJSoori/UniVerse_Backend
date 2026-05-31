package com.example.backend_service.habits;

/*
 * Repository tests adjusted to exercise the `findVisibleToStudent` query
 * which ensures groups owned by, or joined by, a student are returned.
 */

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class GroupHabitRepositoryTest {

    @Autowired
    private GroupHabitRepository repository;

    @Test
    void findByStudentIdAndFindByCode() {
        GroupHabit g = new GroupHabit();
        g.setStudentId(42L);
        g.setName("Test Group");
        g.setCode("UNIT-ABC-123");
        g.setMembersJson("[{\"id\":\"42\",\"name\":\"Owner\",\"role\":\"owner\"}]");
        repository.save(g);

        List<GroupHabit> byStudent = repository.findByStudentId(42L);
        assertThat(byStudent).isNotEmpty();
        assertThat(byStudent.get(0).getCode()).isEqualTo("UNIT-ABC-123");

        var byCode = repository.findByCode("UNIT-ABC-123");
        assertThat(byCode).isPresent();
        assertThat(byCode.get().getStudentId()).isEqualTo(42L);

        var byCodeIgnoreCase = repository.findByCodeIgnoreCase("unit-abc-123");
        assertThat(byCodeIgnoreCase).isPresent();
        assertThat(byCodeIgnoreCase.get().getId()).isEqualTo(g.getId());

        var visible = repository.findVisibleToStudent(42L, "42");
        assertThat(visible).isNotEmpty();
        assertThat(visible.get(0).getId()).isEqualTo(g.getId());
    }

    @Test
    void findVisibleToStudent_includesJoinedMemberGroups() {
        GroupHabit ownerGroup = new GroupHabit();
        ownerGroup.setStudentId(11L);
        ownerGroup.setName("Owner Group");
        ownerGroup.setCode("OWNER-11");
        ownerGroup.setMembersJson("[{\"id\":\"11\",\"name\":\"Owner\",\"role\":\"owner\"}]");
        repository.save(ownerGroup);

        GroupHabit joinedGroup = new GroupHabit();
        joinedGroup.setStudentId(77L);
        joinedGroup.setName("Joined Group");
        joinedGroup.setCode("JOIN-77");
        joinedGroup.setMembersJson("[{\"id\":\"11\",\"name\":\"Alice\",\"role\":\"member\"}]");
        repository.save(joinedGroup);

        List<GroupHabit> visible = repository.findVisibleToStudent(11L, "11");
        assertThat(visible).extracting(GroupHabit::getName).contains("Owner Group", "Joined Group");
    }
}

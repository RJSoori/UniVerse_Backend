package com.example.backend_service.habits;

import com.example.backend_service.Student;
import com.example.backend_service.StudentRepository;
import com.example.backend_service.common.email.EmailService;
import com.example.backend_service.common.exception.BadRequestException;
import com.example.backend_service.common.exception.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Only the group owner may send an invite email — a member (or a non-member entirely) must be
 * rejected, and the generated message must include the sender's name and the group name.
 */
@ExtendWith(MockitoExtension.class)
class GroupHabitInviteServiceTest {

    @Mock
    private GroupHabitRepository groupHabitRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private EmailService emailService;

    private GroupHabitInviteService service;

    @BeforeEach
    void setUp() {
        service = new GroupHabitInviteService(groupHabitRepository, studentRepository, emailService);
    }

    private GroupHabit group() {
        GroupHabit group = new GroupHabit();
        group.setId(1L);
        group.setStudentId(10L); // owner
        group.setName("Morning Runners");
        group.setHabitName("Run 5k");
        group.setInviteLink("https://universe.app/habits/join?group=1&code=ABC123");
        group.setCode("ABC123");
        return group;
    }

    @Test
    void sendInvite_ownerCanSendToValidEmail() {
        when(groupHabitRepository.findById(1L)).thenReturn(Optional.of(group()));
        Student owner = new Student();
        owner.setId(10L);
        owner.setName("Sam Soori");
        when(studentRepository.findById(10L)).thenReturn(Optional.of(owner));

        service.sendInvite(10L, 1L, "friend@example.com");

        verify(emailService).sendMultipartHtml(eq("friend@example.com"), contains("Sam Soori"), contains("Morning Runners"));
    }

    @Test
    void sendInvite_nonOwnerMemberIsForbidden() {
        when(groupHabitRepository.findById(1L)).thenReturn(Optional.of(group()));

        assertThatThrownBy(() -> service.sendInvite(20L, 1L, "friend@example.com"))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(emailService);
    }

    @Test
    void sendInvite_rejectsInvalidEmail() {
        assertThatThrownBy(() -> service.sendInvite(10L, 1L, "not-an-email"))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(emailService);
    }
}

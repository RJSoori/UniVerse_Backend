package com.example.backend_service.habits;

import com.example.backend_service.StudentRepository;
import com.example.backend_service.common.exception.ForbiddenException;
import com.example.backend_service.notifications.PushNotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Covers the owner-vs-member authorization split in {@link GroupHabitController#updateGroupHabit}:
 * only the owner may edit group details or see the invite code/link, a regular member may only
 * record their own completion dates or leave, and ownership transfers to a remaining member when
 * the owner leaves.
 */
@ExtendWith(MockitoExtension.class)
class GroupHabitControllerTest {

    @Mock
    private GroupHabitRepository groupHabitRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private PushNotificationService pushNotificationService;
    @Mock
    private GroupHabitInviteService groupHabitInviteService;

    @InjectMocks
    private GroupHabitController controller;

    private GroupHabit groupWithOwnerAndMember() {
        GroupHabit group = new GroupHabit();
        group.setId(1L);
        group.setStudentId(10L); // owner
        group.setName("Morning Runners");
        group.setHabitName("Run 5k");
        group.setCode("ABC123");
        group.setInviteLink("https://universe.app/habits/join?group=1&code=ABC123");
        group.setMembersJson("[{\"id\":\"10\",\"name\":\"Owner\",\"role\":\"owner\"},{\"id\":\"20\",\"name\":\"Member\",\"role\":\"member\"}]");
        group.setMemberProgressJson("{\"10\":[\"2026-08-20\"],\"20\":[]}");
        group.setCompletedDatesJson("[\"2026-08-20\"]");
        return group;
    }

    @Test
    void updateGroupHabit_memberCanRecordOwnProgress_butNotRenameGroupOrSeeInviteDetails() {
        GroupHabit group = groupWithOwnerAndMember();
        when(groupHabitRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupHabitRepository.save(any(GroupHabit.class))).thenAnswer(inv -> inv.getArgument(0));

        GroupHabitDto dto = new GroupHabitDto();
        dto.setName("Hacked Name"); // a non-owner can't rename the group
        dto.setCode("HACKED"); // or rotate the invite code
        dto.setMemberProgress(Map.of("20", List.of("2026-08-21")));

        GroupHabitDto body = controller.updateGroupHabit(20L, 1L, dto).getBody();

        assertThat(body.getName()).isEqualTo("Morning Runners");
        assertThat(body.getCode()).isNull(); // hidden from non-owner responses too
        assertThat(body.getInviteLink()).isNull();
        assertThat(body.getMemberProgress().get("20")).containsExactly("2026-08-21");
        assertThat(body.getMemberProgress().get("10")).containsExactly("2026-08-20"); // owner's entry untouched
    }

    @Test
    void updateGroupHabit_memberCanLeave_withoutAffectingOwnership() {
        GroupHabit group = groupWithOwnerAndMember();
        when(groupHabitRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupHabitRepository.save(any(GroupHabit.class))).thenAnswer(inv -> inv.getArgument(0));

        GroupHabitDto.MemberDto ownerOnly = new GroupHabitDto.MemberDto();
        ownerOnly.setId("10");
        ownerOnly.setName("Owner");
        ownerOnly.setRole("owner");

        GroupHabitDto dto = new GroupHabitDto();
        dto.setMembers(List.of(ownerOnly)); // member 20 removes themselves

        GroupHabitDto body = controller.updateGroupHabit(20L, 1L, dto).getBody();

        assertThat(body.getMembers()).extracting(GroupHabitDto.MemberDto::getId).containsExactly("10");
        assertThat(body.getMemberProgress()).doesNotContainKey("20");
        assertThat(group.getStudentId()).isEqualTo(10L); // owner unchanged
    }

    @Test
    void updateGroupHabit_ownerLeavingTransfersOwnershipToRemainingMember() {
        GroupHabit group = groupWithOwnerAndMember();
        when(groupHabitRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupHabitRepository.save(any(GroupHabit.class))).thenAnswer(inv -> inv.getArgument(0));

        GroupHabitDto.MemberDto remaining = new GroupHabitDto.MemberDto();
        remaining.setId("20");
        remaining.setName("Member");
        remaining.setRole("member");

        GroupHabitDto dto = new GroupHabitDto();
        dto.setMembers(List.of(remaining)); // owner (10) removes themselves
        dto.setOwnerId("20");

        GroupHabitDto body = controller.updateGroupHabit(10L, 1L, dto).getBody();

        assertThat(group.getStudentId()).isEqualTo(20L);
        assertThat(body.getOwnerId()).isEqualTo("20");
    }

    @Test
    void updateGroupHabit_nonMemberIsForbidden() {
        GroupHabit group = groupWithOwnerAndMember();
        when(groupHabitRepository.findById(1L)).thenReturn(Optional.of(group));

        GroupHabitDto dto = new GroupHabitDto();

        assertThatThrownBy(() -> controller.updateGroupHabit(99L, 1L, dto))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getGroupHabit_hidesInviteCodeAndLinkFromNonOwnerMember() {
        GroupHabit group = groupWithOwnerAndMember();
        when(groupHabitRepository.findById(1L)).thenReturn(Optional.of(group));

        GroupHabitDto body = controller.getGroupHabit(20L, 1L).getBody();

        assertThat(body.getCode()).isNull();
        assertThat(body.getInviteLink()).isNull();
    }

    @Test
    void getGroupHabit_showsInviteCodeAndLinkToOwner() {
        GroupHabit group = groupWithOwnerAndMember();
        when(groupHabitRepository.findById(1L)).thenReturn(Optional.of(group));

        GroupHabitDto body = controller.getGroupHabit(10L, 1L).getBody();

        assertThat(body.getCode()).isEqualTo("ABC123");
        assertThat(body.getInviteLink()).isNotBlank();
    }
}

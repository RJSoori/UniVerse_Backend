package com.example.backend_service.habits;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data object for group habit API communication.
 */
public class GroupHabitDto {
    private Long id;
    private String name;
    private String habitName;
    private String description;
    private String code;
    private String inviteLink;
    @JsonProperty("iconId")
    private String iconId;
    private String ownerId;
    private String createdAt;
    private List<MemberDto> members;
    @JsonProperty("completedDates")
    private List<String> completedDates;
    @JsonProperty("memberProgress")
    private Map<String, List<String>> memberProgress;

    public GroupHabitDto() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getHabitName() { return habitName; }
    public void setHabitName(String habitName) { this.habitName = habitName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getInviteLink() { return inviteLink; }
    public void setInviteLink(String inviteLink) { this.inviteLink = inviteLink; }

    public String getIconId() { return iconId; }
    public void setIconId(String iconId) { this.iconId = iconId; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public List<MemberDto> getMembers() { return members; }
    public void setMembers(List<MemberDto> members) { this.members = members; }

    public List<String> getCompletedDates() { return completedDates; }
    public void setCompletedDates(List<String> completedDates) { this.completedDates = completedDates; }

    public Map<String, List<String>> getMemberProgress() { return memberProgress; }
    public void setMemberProgress(Map<String, List<String>> memberProgress) { this.memberProgress = memberProgress; }

    public static class MemberDto {
        private String id;
        private String name;
        private String role;
        private String joinedAt;

        public MemberDto() {}

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public String getJoinedAt() { return joinedAt; }
        public void setJoinedAt(String joinedAt) { this.joinedAt = joinedAt; }
    }
}
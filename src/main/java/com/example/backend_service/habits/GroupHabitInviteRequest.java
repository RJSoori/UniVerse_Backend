package com.example.backend_service.habits;

/**
 * Request body for {@code POST /api/students/{studentId}/group-habits/{groupHabitId}/invite}:
 * the email address the group owner wants to invite.
 */
public class GroupHabitInviteRequest {

    private String email;

    public GroupHabitInviteRequest() {}

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}

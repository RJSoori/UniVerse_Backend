package com.example.backend_service.habits;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Database entity for group habits.
 */
/**
 * Represents a habit that a group of students shares.
 * Stores group members, invite code, and shared completion tracking.
 */
@Entity
@Table(name = "group_habits")
public class GroupHabit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "habit_name")
    private String habitName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "code", unique = true)
    private String code;

    @Column(name = "invite_link")
    private String inviteLink;

    @Column(name = "icon_id")
    private String iconId;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Lob
    @Column(name = "members_json", columnDefinition = "LONGTEXT")
    private String membersJson; // JSON array of members

    @Lob
    @Column(name = "completed_dates_json", columnDefinition = "LONGTEXT")
    private String completedDatesJson; // JSON array of dates

    @Lob
    @Column(name = "member_progress_json", columnDefinition = "LONGTEXT")
    private String memberProgressJson; // JSON map of memberId -> completed dates

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public GroupHabit() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getMembersJson() { return membersJson; }
    public void setMembersJson(String membersJson) { this.membersJson = membersJson; }

    public String getCompletedDatesJson() { return completedDatesJson; }
    public void setCompletedDatesJson(String completedDatesJson) { this.completedDatesJson = completedDatesJson; }

    public String getMemberProgressJson() { return memberProgressJson; }
    public void setMemberProgressJson(String memberProgressJson) { this.memberProgressJson = memberProgressJson; }
}
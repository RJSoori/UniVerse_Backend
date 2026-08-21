package com.example.backend_service.habits;

import com.example.backend_service.Student;
import com.example.backend_service.StudentRepository;
import com.example.backend_service.common.email.EmailService;
import com.example.backend_service.common.exception.BadRequestException;
import com.example.backend_service.common.exception.ForbiddenException;
import com.example.backend_service.common.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Sends group-habit invite emails on behalf of the group owner. Only the current owner
 * (the student who created the group, i.e. {@link GroupHabit#getStudentId()}) may send
 * invites — members never see the invite link/code either, so they can't hand them out. The
 * message is generated server-side (subject/body live here, not in the client) so the sender
 * only ever supplies the recipient's address; the group name, habit name, invite link/code,
 * and the sender's own name are filled in automatically from the group and student records.
 */
@Service
public class GroupHabitInviteService {

    private static final Logger log = LoggerFactory.getLogger(GroupHabitInviteService.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final GroupHabitRepository groupHabitRepository;
    private final StudentRepository studentRepository;
    private final EmailService emailService;

    public GroupHabitInviteService(
            GroupHabitRepository groupHabitRepository,
            StudentRepository studentRepository,
            @Qualifier("studentEmailService") EmailService emailService) {
        this.groupHabitRepository = groupHabitRepository;
        this.studentRepository = studentRepository;
        this.emailService = emailService;
    }

    public void sendInvite(Long senderStudentId, Long groupHabitId, String rawEmail) {
        String email = rawEmail == null ? "" : rawEmail.trim();
        if (email.isEmpty() || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new BadRequestException("Please enter a valid email address.");
        }

        GroupHabit group = groupHabitRepository.findById(groupHabitId)
                .orElseThrow(NotFoundException::new);
        if (!senderStudentId.equals(group.getStudentId())) {
            throw new ForbiddenException("Only the group owner can send invites.");
        }

        String senderName = studentRepository.findById(senderStudentId)
                .map(Student::getName)
                .filter(name -> name != null && !name.isBlank())
                .orElse("A UniVerse student");

        String groupName = group.getName() == null || group.getName().isBlank() ? "a group" : group.getName();
        String habitName = group.getHabitName() == null || group.getHabitName().isBlank() ? null : group.getHabitName();

        String subject = senderName + " invited you to join \"" + groupName + "\" on UniVerse";
        String html = buildInviteEmailHtml(senderName, groupName, habitName, group.getInviteLink(), group.getCode());

        emailService.sendMultipartHtml(email, subject, html);
        log.info("Group habit invite sent by studentId={} for groupHabitId={} to {}", senderStudentId, groupHabitId, email);
    }

    private String buildInviteEmailHtml(String senderName, String groupName, String habitName, String inviteLink, String code) {
        String habitLine = habitName == null
                ? ""
                : "<p>The group is tracking the habit: <strong>" + escapeHtml(habitName) + "</strong></p>";
        String linkButton = inviteLink == null || inviteLink.isBlank()
                ? ""
                : """
                        <p style="text-align: center; margin: 24px 0;">
                            <a href="%s" style="background: #4f46e5; color: #ffffff; text-decoration: none; padding: 12px 24px; border-radius: 10px; font-weight: 600; display: inline-block;">Join the group</a>
                        </p>
                        <p style="word-break: break-all; font-size: 12px; color: #6b7280;">Or open this link directly: %s</p>
                        """.formatted(inviteLink, escapeHtml(inviteLink));
        String codeBlock = code == null || code.isBlank()
                ? ""
                : """
                        <p>Or join with invite code:</p>
                        <div style="font-size: 24px; font-weight: bold; letter-spacing: 6px; background: #f3f4f6; padding: 12px 20px; border-radius: 12px; text-align: center; margin: 16px 0;">
                            %s
                        </div>
                        """.formatted(escapeHtml(code));

        return """
                <div style="font-family: Arial, sans-serif; max-width: 480px; margin: 0 auto; color: #1f2937;">
                    <h2 style="color: #4f46e5;">UniVerse</h2>
                    <p><strong>%s</strong> has invited you to join the group habit <strong>"%s"</strong> on UniVerse.</p>
                    %s
                    %s
                    %s
                    <p style="color: #6b7280; font-size: 12px; margin-top: 32px;">UniVerse &bull; Do not reply to this automated email.</p>
                </div>
                """.formatted(escapeHtml(senderName), escapeHtml(groupName), habitLine, linkButton, codeBlock);
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}

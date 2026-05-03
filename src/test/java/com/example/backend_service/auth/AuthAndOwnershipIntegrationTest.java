package com.example.backend_service.auth;

import com.example.backend_service.StudentRepository;
import com.example.backend_service.gpacalculator.repository.GPASemesterRepository;
import com.example.backend_service.gpacalculator.repository.GPASettingsRepository;
import com.example.backend_service.gpacalculator.repository.GPASubjectRepository;
import com.example.backend_service.moneymanager.repository.TransactionRepository;
import com.example.backend_service.moneymanager.repository.WalletRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthAndOwnershipIntegrationTest {

    @LocalServerPort
    private int port;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private GPASubjectRepository subjectRepository;

    @Autowired
    private GPASemesterRepository semesterRepository;

    @Autowired
    private GPASettingsRepository settingsRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private WalletRepository walletRepository;

    private final HttpClient http = HttpClient.newHttpClient();

    @BeforeEach
    void cleanDatabase() {
        subjectRepository.deleteAll();
        semesterRepository.deleteAll();
        settingsRepository.deleteAll();
        transactionRepository.deleteAll();
        walletRepository.deleteAll();
        studentRepository.deleteAll();
    }

    @Test
    void registerReturnsTokenAndMeReturnsSameUser() throws Exception {
        AuthSession session = register("alpha");

        TestResponse response = getJson("/api/auth/me", session.token());

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.json().path("id").asLong()).isEqualTo(session.userId());
        assertThat(response.json().path("username").asText()).isEqualTo("user-alpha");
    }

    @Test
    void loginWithBadPasswordReturnsUnauthorized() throws Exception {
        register("bad-password");

        TestResponse response = postJson("/api/auth/login", null, Map.of(
                "username", "user-bad-password",
                "password", "wrong-password"));

        assertThat(response.status()).isEqualTo(401);
        assertThat(response.json().path("error").asText()).isEqualTo("Invalid credentials");
    }

    @Test
    void duplicateUsernameRegistrationReturnsConflict() throws Exception {
        register("duplicate");

        TestResponse response = postJson("/api/auth/register", null, Map.of(
                "name", "Second Student",
                "degree", "Computer Science",
                "email", "second-duplicate@example.com",
                "username", "user-duplicate",
                "password", "password123"));

        assertThat(response.status()).isEqualTo(409);
    }

    @Test
    void gpaSemestersAreIsolatedByStudent() throws Exception {
        AuthSession studentA = register("gpa-a");
        AuthSession studentB = register("gpa-b");

        String semesterId = createSemester(studentA.token(), "2026", "Semester 1");

        TestResponse response = getJson("/api/gpa/semesters", studentB.token());

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.body()).doesNotContain(semesterId);
        assertThat(response.json().size()).isZero();
    }

    @Test
    void moneyManagerRowsAreIsolatedAndBulkReplaceDoesNotDeleteOtherStudentsRows() throws Exception {
        AuthSession studentA = register("money-a");
        AuthSession studentB = register("money-b");

        String walletId = createWallet(studentA.token(), "Cash");
        createTransaction(studentA.token(), walletId);

        TestResponse studentBWallets = getJson("/api/money-manager/wallets", studentB.token());
        TestResponse studentBTransactions = getJson("/api/money-manager/transactions", studentB.token());
        assertThat(studentBWallets.status()).isEqualTo(200);
        assertThat(studentBWallets.json().size()).isZero();
        assertThat(studentBTransactions.status()).isEqualTo(200);
        assertThat(studentBTransactions.json().size()).isZero();

        TestResponse replace = putJson("/api/money-manager/transactions/bulk", studentB.token(), "[]");
        assertThat(replace.status()).isEqualTo(200);

        assertThat(getJson("/api/money-manager/wallets", studentA.token()).json().size()).isEqualTo(1);
        assertThat(getJson("/api/money-manager/transactions", studentA.token()).json().size()).isEqualTo(1);
    }

    @Test
    void gpaSubjectCreationRejectsForeignSemesterId() throws Exception {
        AuthSession studentA = register("subject-a");
        AuthSession studentB = register("subject-b");

        String foreignSemesterId = createSemester(studentA.token(), "2026", "Semester 1");

        TestResponse rejected = postJson("/api/gpa/subjects", studentB.token(),
                subjectPayload(foreignSemesterId, "Data Structures"));

        assertThat(rejected.status()).isEqualTo(404);
        assertThat(subjectRepository.findAll()).isEmpty();

        String ownSemesterId = createSemester(studentB.token(), "2026", "Semester 2");
        TestResponse savedSubject = postJson("/api/gpa/subjects", studentB.token(),
                subjectPayload(ownSemesterId, "Algorithms"));

        assertThat(savedSubject.status()).isEqualTo(201);
        assertThat(savedSubject.json().path("semester").path("id").asText()).isEqualTo(ownSemesterId);
        assertThat(subjectRepository.findAll()).hasSize(1);

        TestResponse semesters = getJson("/api/gpa/semesters", studentB.token());
        assertThat(semesters.status()).isEqualTo(200);
        assertThat(semesters.json().get(0).path("subjects").size()).isEqualTo(1);
        assertThat(semesters.json().get(0).path("subjects").get(0).path("name").asText()).isEqualTo("Algorithms");
    }

    @Test
    void moneyTransactionCreationRejectsForeignWalletId() throws Exception {
        AuthSession studentA = register("wallet-a");
        AuthSession studentB = register("wallet-b");

        String foreignWalletId = createWallet(studentA.token(), "A Cash");

        TestResponse rejected = postJson("/api/money-manager/transactions", studentB.token(),
                transactionPayload(foreignWalletId));

        assertThat(rejected.status()).isEqualTo(404);
        assertThat(transactionRepository.findAll()).isEmpty();
    }

    private AuthSession register(String suffix) throws Exception {
        TestResponse response = postJson("/api/auth/register", null, Map.of(
                "name", "Student " + suffix,
                "degree", "Computer Science",
                "email", suffix + "@example.com",
                "username", "user-" + suffix,
                "password", "password123"));

        assertThat(response.status()).isEqualTo(201);
        assertThat(response.json().path("token").asText()).isNotBlank();
        assertThat(response.json().path("user").path("username").asText()).isEqualTo("user-" + suffix);

        return new AuthSession(
                response.json().path("token").asText(),
                response.json().path("user").path("id").asLong());
    }

    private String createSemester(String token, String year, String semester) throws Exception {
        TestResponse response = postJson("/api/gpa/semesters", token, Map.of("year", year, "semester", semester));
        assertThat(response.status()).isEqualTo(201);
        return response.json().path("id").asText();
    }

    private String createWallet(String token, String name) throws Exception {
        TestResponse response = postJson("/api/money-manager/wallets", token, Map.of(
                "name", name,
                "balance", 100,
                "createdDate", "2026-05-03",
                "type", "CASH",
                "includeInTotal", true));
        assertThat(response.status()).isEqualTo(201);
        return response.json().path("id").asText();
    }

    private void createTransaction(String token, String walletId) throws Exception {
        assertThat(postJson("/api/money-manager/transactions", token, transactionPayload(walletId)).status())
                .isEqualTo(201);
    }

    private Map<String, Object> subjectPayload(String semesterId, String name) {
        return Map.of(
                "name", name,
                "credits", 3.0,
                "grade", "A",
                "isGpa", true,
                "semester", Map.of("id", semesterId));
    }

    private Map<String, Object> transactionPayload(String walletId) {
        return Map.of(
                "type", "EXPENSE",
                "amount", 25,
                "category", "Food",
                "walletId", walletId,
                "description", "Lunch",
                "date", "2026-05-03",
                "time", "12:30",
                "isRecurring", false);
    }

    private TestResponse getJson(String path, String token) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path)).GET();
        addHeaders(request, token);
        return send(request.build());
    }

    private TestResponse postJson(String path, String token, Object body) throws Exception {
        return sendWithBody("POST", path, token, objectMapper.writeValueAsString(body));
    }

    private TestResponse putJson(String path, String token, String jsonBody) throws Exception {
        return sendWithBody("PUT", path, token, jsonBody);
    }

    private TestResponse sendWithBody(String method, String path, String token, String jsonBody) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path))
                .method(method, HttpRequest.BodyPublishers.ofString(jsonBody));
        addHeaders(request, token);
        return send(request.build());
    }

    private void addHeaders(HttpRequest.Builder request, String token) {
        request.header("Content-Type", "application/json");
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
    }

    private TestResponse send(HttpRequest request) throws Exception {
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        return new TestResponse(response.statusCode(), response.body());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private record AuthSession(String token, Long userId) {
    }

    private record TestResponse(int status, String body) {
        JsonNode json() throws Exception {
            return body == null || body.isBlank() ? null : new ObjectMapper().readTree(body);
        }
    }
}

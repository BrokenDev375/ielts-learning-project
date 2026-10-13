package vn.edu.ptit.ielts;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.assertj.core.api.Assertions.assertThat;
import vn.edu.ptit.ielts.dto.UserRecord;
import vn.edu.ptit.ielts.enums.Role;
import vn.edu.ptit.ielts.repository.AssetJpaRepository;
import vn.edu.ptit.ielts.repository.AssignmentJpaRepository;
import vn.edu.ptit.ielts.repository.AssignmentRecipientJpaRepository;
import vn.edu.ptit.ielts.repository.ClassMemberJpaRepository;
import vn.edu.ptit.ielts.repository.ClassroomJpaRepository;
import vn.edu.ptit.ielts.repository.ResourceAssetJpaRepository;
import vn.edu.ptit.ielts.repository.ResourceJpaRepository;
import vn.edu.ptit.ielts.repository.ResourceLabelJpaRepository;
import vn.edu.ptit.ielts.repository.ResourceVersionJpaRepository;
import vn.edu.ptit.ielts.repository.UserJpaRepository;
import vn.edu.ptit.ielts.repository.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "app.demo.enabled=false")
@Testcontainers
class ModuleTwoSpeakingApiTest {
    @Container static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
        .withDatabaseName("ielts_module2_tests").withUsername("test_app").withPassword("test-password");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> MYSQL.getJdbcUrl() + "?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true");
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }
    @LocalServerPort int port;
    @Autowired ObjectMapper json;
    @Autowired AssignmentRecipientJpaRepository recipientEntities;
    @Autowired AssignmentJpaRepository assignmentEntities;
    @Autowired ResourceAssetJpaRepository resourceAssetEntities;
    @Autowired ResourceLabelJpaRepository resourceLabelEntities;
    @Autowired ResourceVersionJpaRepository versionEntities;
    @Autowired ResourceJpaRepository resourceEntities;
    @Autowired AssetJpaRepository assetEntities;
    @Autowired ClassMemberJpaRepository memberEntities;
    @Autowired ClassroomJpaRepository classroomEntities;
    @Autowired UserJpaRepository userEntities;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;
    UserRecord teacher, otherTeacher, student;
    String hash;

    @BeforeEach void fixtures() {
        // This database belongs to the disposable test container, not the development database.
        recipientEntities.deleteAllInBatch();
        assignmentEntities.deleteAllInBatch();
        resourceAssetEntities.deleteAllInBatch();
        resourceLabelEntities.deleteAllInBatch();
        versionEntities.deleteAllInBatch();
        resourceEntities.deleteAllInBatch();
        assetEntities.deleteAllInBatch();
        memberEntities.deleteAllInBatch();
        classroomEntities.deleteAllInBatch();
        userEntities.deleteAllInBatch();
        if (hash == null) hash = passwords.encode("TestPass@123");
        teacher = users.create("teacher@test.local", hash, "Giảng viên Một", Role.TEACHER);
        otherTeacher = users.create("other@test.local", hash, "Giảng viên Hai", Role.TEACHER);
        student = users.create("student@test.local", hash, "Học viên Một", Role.STUDENT);
    }

    final class Client {
        final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        final HttpClient http = HttpClient.newBuilder().cookieHandler(cookies).connectTimeout(Duration.ofSeconds(5)).build();
        String token;
        void csrf() throws Exception {
            var response = request("GET", "/api/auth/csrf", null, false);
            assertThat(response.statusCode()).isEqualTo(200);
            token = body(response).get("token").asText();
        }
        Client login(String email) throws Exception {
            csrf();
            var response = request("POST", "/api/auth/login", Map.of("email", email, "password", "TestPass@123"), true);
            assertThat(response.statusCode()).isEqualTo(200);
            csrf();
            return this;
        }
        HttpResponse<String> request(String method, String path, Object payload, boolean useCsrf) throws Exception {
            var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(15)).header("Accept", "application/json");
            if (useCsrf) builder.header("X-CSRF-TOKEN", token);
            if (payload != null) builder.header("Content-Type", "application/json");
            builder.method(method, payload == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(payload)));
            return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        }
        HttpResponse<String> send(String method, String path, Object payload) throws Exception {
            return request(method, path, payload, !method.equals("GET"));
        }
    }
    JsonNode body(HttpResponse<String> response) throws Exception { return json.readTree(response.body()); }
    JsonNode create(Client client, String name) throws Exception {
        var response = client.send("POST", "/api/classes", Map.of("name", name, "description", "Lớp thử nghiệm", "requestId", UUID.randomUUID().toString()));
        assertThat(response.statusCode()).isEqualTo(201);
        return body(response);
    }


    com.fasterxml.jackson.databind.node.ObjectNode input(String title,String type) {
        var root=json.createObjectNode().put("title",title).put("skill","SPEAKING")
            .put("resourceKind",type.equals("MAIN")?"MAIN":"REMEDIAL")
            .put("activityType",type.equals("MAIN")?"SPEAKING_RECORDING":type.equals("DEVELOPMENT")?"OUTLINE":"PRONUNCIATION")
            .put("topic","Hobbies").put("levelFramework","IELTS_PRACTICE").put("levelLabel","").put("supportLevel","GUIDED");
        var c=root.putObject("content").put("taskType",type).put("part",0).put("prompt","Talk about a hobby.")
            .put("instructions","Explain your reasons.").put("preparationSeconds",60).put("speakingSeconds",120);
        c.putArray("checklist").add("Give a reason"); c.putArray("assessmentCriteria").add("Fluency and coherence");
        c.putArray("phonemes"); c.putArray("items");
        root.putObject("teaching").put("teacherNotes","PRIVATE_TEACHER_NOTE").putArray("outlineExamples").add("PRIVATE_OUTLINE");
        root.putObject("source").put("url","").put("note","Test original content");
        return root;
    }
    JsonNode draft(Client client,com.fasterxml.jackson.databind.node.ObjectNode input) throws Exception {
        var r=client.send("POST","/api/resources",input);assertThat(r.statusCode()).as(r.body()).isEqualTo(201);return body(r);
    }
    void publish(Client owner,JsonNode resource) throws Exception {
        var r=owner.send("POST","/api/resources/"+resource.get("id").asLong()+"/versions/"+resource.get("version").get("id").asLong()+"/publish",null);
        assertThat(r.statusCode()).as(r.body()).isEqualTo(200);
    }
    com.fasterxml.jackson.databind.node.ObjectNode assignment(JsonNode room,JsonNode resource,String mode,List<Long> recipients) {
        var p=json.createObjectNode().put("requestId",UUID.randomUUID().toString()).put("classroomId",room.get("id").asLong())
            .put("resourceVersionId",resource.get("version").get("id").asLong()).put("purpose",resource.get("version").get("resourceKind").asText().equals("MAIN")?"PRACTICE":"REMEDIAL")
            .put("recipientMode",mode).put("allowLate",false).put("maxAttempts",1).put("feedbackPolicy","TEACHER_RELEASE");
        p.putNull("opensAt");p.putNull("dueAt");p.set("studentIds",json.valueToTree(recipients));return p;
    }
    void join(Client learner,JsonNode room) throws Exception {
        assertThat(learner.send("POST","/api/classes/join",Map.of("code",room.get("joinCode").asText())).statusCode()).isEqualTo(200);
    }
    long upload(Client client,boolean valid) throws Exception {
        byte[] wave=new byte[44];System.arraycopy("RIFF".getBytes(),0,wave,0,4);System.arraycopy("WAVE".getBytes(),0,wave,8,4);
        var out=new java.io.ByteArrayOutputStream();
        out.write("--test-boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"sample.wav\"\r\nContent-Type: audio/wav\r\n\r\n".getBytes());
        out.write(valid?wave:"not audio".getBytes());out.write("\r\n--test-boundary--\r\n".getBytes());
        var req=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/assets")).header("X-CSRF-TOKEN",client.token)
            .header("Content-Type","multipart/form-data; boundary=test-boundary").POST(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray())).build();
        var r=client.http.send(req,HttpResponse.BodyHandlers.ofString());
        assertThat(r.statusCode()).as(r.body()).isEqualTo(valid?201:400);return valid?body(r).get("id").asLong():0;
    }
    @Test void resourceAndAssignmentOperationsRequireTeacherOwnership() throws Exception {
        var owner=new Client().login(teacher.email());var learner=new Client().login(student.email());var other=new Client().login(otherTeacher.email());
        var resource=draft(owner,input("Private resource","MAIN"));long id=resource.get("id").asLong(),version=resource.get("version").get("id").asLong();
        assertThat(learner.send("POST","/api/resources",input("Forged","MAIN")).statusCode()).isEqualTo(403);
        assertThat(learner.send("GET","/api/resources",null).statusCode()).isEqualTo(403);
        assertThat(other.send("GET","/api/resources/"+id,null).statusCode()).isEqualTo(403);
        assertThat(other.send("PUT","/api/resources/"+id+"/draft",input("Changed","MAIN")).statusCode()).isEqualTo(403);
        assertThat(other.send("POST","/api/resources/"+id+"/versions/"+version+"/publish",null).statusCode()).isEqualTo(403);
        assertThat(body(other.send("GET","/api/resources",null))).isEmpty();
        var room=create(owner,"Owned class");
        assertThat(learner.send("POST","/api/assignments",assignment(room,resource,"CLASS_SNAPSHOT",List.of())).statusCode()).isEqualTo(403);
        assertThat(owner.request("POST","/api/resources",input("No CSRF","MAIN"),false).statusCode()).isEqualTo(403);
    }
    @Test void validatesPublishedContentAndDoesNotAcceptUnsupportedSkillOrForeignAudio() throws Exception {
        var owner=new Client().login(teacher.email());var r=input("Incomplete","DEVELOPMENT");r.withObject("content").put("prompt","");
        var resource=draft(owner,r);
        String url="/api/resources/"+resource.get("id").asLong()+"/versions/"+resource.get("version").get("id").asLong()+"/publish";
        assertThat(owner.send("POST",url,null).statusCode()).isEqualTo(400);
        r=input("Wrong task","MAIN");r.put("skill","READING");assertThat(owner.send("POST","/api/resources",r).statusCode()).isEqualTo(400);
        r=input("Unknown field","MAIN");r.withObject("content").put("unsafeUnknownField","unexpected");assertThat(owner.send("POST","/api/resources",r).statusCode()).isEqualTo(400);
        r=input("Invalid source","MAIN");r.withObject("source").put("url","javascript:alert(1)");assertThat(owner.send("POST","/api/resources",r).statusCode()).isEqualTo(400);
        var other=new Client().login(otherTeacher.email());long foreign=upload(other,true);
        r=input("Foreign audio","PRONUNCIATION");r.withObject("content").withArray("phonemes").add("/θ/");
        r.withObject("content").withArray("items").addObject().put("text","think").put("ipa","/θɪŋk/").put("audioAssetId",foreign);
        assertThat(owner.send("POST","/api/resources",r).statusCode()).isEqualTo(400);
    }
    @Test void assignedVersionStaysImmutableAndStudentDoesNotReceiveTeachingReferences() throws Exception {
        var owner=new Client().login(teacher.email());var learner=new Client().login(student.email());
        var room=create(owner,"Versioned class");join(learner,room);var resource=draft(owner,input("Original topic","DEVELOPMENT"));publish(owner,resource);
        var assigned=owner.send("POST","/api/assignments",assignment(room,resource,"CLASS_SNAPSHOT",List.of()));assertThat(assigned.statusCode()).isEqualTo(201);long id=body(assigned).get("id").asLong();
        var edit=owner.send("PUT","/api/resources/"+resource.get("id").asLong()+"/draft",input("New topic","DEVELOPMENT"));
        assertThat(edit.statusCode()).isEqualTo(200);var next=body(edit);assertThat(next.get("version").get("versionNo").asInt()).isEqualTo(2);publish(owner,next);
        var detail=learner.send("GET","/api/assignments/"+id,null);assertThat(detail.statusCode()).isEqualTo(200);
        assertThat(body(detail).get("title").asText()).isEqualTo("Original topic");
        assertThat(body(detail).get("assignmentId").asLong()).isEqualTo(id);
        assertThat(body(detail).get("resourceId")).isEqualTo(resource.get("id"));
        assertThat(body(detail).get("resourceVersionId")).isEqualTo(resource.get("version").get("id"));
        assertThat(body(detail).get("skill").asText()).isEqualTo("SPEAKING");
        assertThat(body(detail).get("content").isObject()).isTrue();
        assertThat(body(detail).get("questions")).isEmpty();
        assertThat(detail.body()).doesNotContain("PRIVATE_TEACHER_NOTE","PRIVATE_OUTLINE","teaching");
        assertThat(body(owner.send("GET","/api/resources/"+resource.get("id").asLong()+"/versions",null))).hasSize(2);
        assertThat(owner.send("POST","/api/resources/"+resource.get("id").asLong()+"/versions/"+resource.get("version").get("id").asLong()+"/publish",null).statusCode()).isEqualTo(200);
    }
    @Test void recipientsAreSnapshotSelectedStudentsMustBelongToClassAndRemovalRevokesAccess() throws Exception {
        var owner=new Client().login(teacher.email());var learner=new Client().login(student.email());
        var room=create(owner,"Snapshot");join(learner,room);var resource=draft(owner,input("Snapshot topic","MAIN"));publish(owner,resource);
        var assigned=body(owner.send("POST","/api/assignments",assignment(room,resource,"CLASS_SNAPSHOT",List.of())));long id=assigned.get("id").asLong();
        var later=users.create("later@test.local",hash,"Later student",Role.STUDENT);var second=new Client().login(later.email());join(second,room);
        assertThat(body(second.send("GET","/api/assignments",null))).isEmpty();
        assertThat(second.send("GET","/api/assignments/"+id,null).statusCode()).isEqualTo(403);
        var outsider=users.create("outsider@test.local",hash,"Outside",Role.STUDENT);
        assertThat(owner.send("POST","/api/assignments",assignment(room,resource,"SELECTED",List.of(outsider.id()))).statusCode()).isEqualTo(400);
        var selected=owner.send("POST","/api/assignments",assignment(room,resource,"SELECTED",List.of(later.id())));assertThat(selected.statusCode()).isEqualTo(201);
        assertThat(learner.send("GET","/api/assignments/"+body(selected).get("id").asLong(),null).statusCode()).isEqualTo(403);
        owner.send("DELETE","/api/classes/"+room.get("id").asLong()+"/members/"+student.id(),null);
        assertThat(learner.send("GET","/api/assignments/"+id,null).statusCode()).isEqualTo(403);
        assertThat(recipientEntities.countByAssignmentId(id)).isEqualTo(1);
    }
    @Test void assignmentReplayIsIdempotentAndChangedPayloadIsRejected() throws Exception {
        var owner=new Client().login(teacher.email());var learner=new Client().login(student.email());var room=create(owner,"Replay");join(learner,room);
        var r=draft(owner,input("Replay topic","MAIN"));publish(owner,r);var p=assignment(room,r,"CLASS_SNAPSHOT",List.of());
        var first=owner.send("POST","/api/assignments",p);var replay=owner.send("POST","/api/assignments",p);
        assertThat(first.statusCode()).isEqualTo(201);assertThat(replay.statusCode()).isEqualTo(200);assertThat(body(first).get("id")).isEqualTo(body(replay).get("id"));
        p.put("maxAttempts",2);assertThat(owner.send("POST","/api/assignments",p).statusCode()).isEqualTo(409);
        assertThat(assignmentEntities.count()).isEqualTo(1);
    }
    @Test void concurrentReplayCreatesOneAssignment() throws Exception {
        var owner=new Client().login(teacher.email());var owner2=new Client().login(teacher.email());var learner=new Client().login(student.email());
        var room=create(owner,"Concurrent");join(learner,room);var resource=draft(owner,input("Concurrent topic","MAIN"));publish(owner,resource);
        var p=assignment(room,resource,"CLASS_SNAPSHOT",List.of());var barrier=new CyclicBarrier(2);var pool=Executors.newFixedThreadPool(2);
        try {
            var a=pool.submit(()->{barrier.await();return owner.send("POST","/api/assignments",p);});
            var b=pool.submit(()->{barrier.await();return owner2.send("POST","/api/assignments",p);});
            var first=a.get();var second=b.get();assertThat(List.of(first.statusCode(),second.statusCode())).containsExactlyInAnyOrder(201,200);
            assertThat(body(first).get("id")).isEqualTo(body(second).get("id"));
        } finally {pool.shutdownNow();}
    }
    @Test void scheduledAssignmentHidesContentAndRejectsInvalidDates() throws Exception {
        var owner=new Client().login(teacher.email());var learner=new Client().login(student.email());var room=create(owner,"Scheduled");join(learner,room);
        var r=draft(owner,input("Future topic","MAIN"));publish(owner,r);var p=assignment(room,r,"CLASS_SNAPSHOT",List.of());
        p.put("opensAt","2099-01-02T00:00:00Z").put("dueAt","2099-01-01T00:00:00Z");
        assertThat(owner.send("POST","/api/assignments",p).statusCode()).isEqualTo(400);
        p.put("dueAt","2099-01-03T00:00:00Z");var assigned=body(owner.send("POST","/api/assignments",p));
        assertThat(body(learner.send("GET","/api/assignments",null))).hasSize(1);
        var denied=learner.send("GET","/api/assignments/"+assigned.get("id").asLong(),null);
        assertThat(denied.statusCode()).isEqualTo(403);assertThat(body(denied).get("code").asText()).isEqualTo("ASSIGNMENT_NOT_OPEN");
    }
    @Test void pronunciationAudioIsValidatedAndAvailableOnlyToAssignedStudentsIncludingRangeRequests() throws Exception {
        var owner=new Client().login(teacher.email());var learner=new Client().login(student.email());
        upload(owner,false);long audio=upload(owner,true);
        assertThat(learner.send("GET","/api/assets/"+audio,null).statusCode()).isEqualTo(403);
        var room=create(owner,"Audio");join(learner,room);var p=input("Pronunciation","PRONUNCIATION");
        p.withObject("content").withArray("phonemes").add("/θ/");p.withObject("content").withArray("items").addObject().put("text","think").put("ipa","/θɪŋk/").put("audioAssetId",audio);
        var r=draft(owner,p);publish(owner,r);
        assertThat(owner.send("POST","/api/assignments",assignment(room,r,"CLASS_SNAPSHOT",List.of())).statusCode()).isEqualTo(201);
        var req=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/assets/"+audio)).header("Range","bytes=0-11").GET().build();
        var response=learner.http.send(req,HttpResponse.BodyHandlers.ofByteArray());
        assertThat(response.statusCode()).isEqualTo(206);assertThat(response.body()).hasSize(12);assertThat(response.headers().firstValue("Content-Type")).contains("audio/wav");
        owner.send("DELETE","/api/classes/"+room.get("id").asLong()+"/members/"+student.id(),null);
        assertThat(learner.send("GET","/api/assets/"+audio,null).statusCode()).isEqualTo(403);
    }
}

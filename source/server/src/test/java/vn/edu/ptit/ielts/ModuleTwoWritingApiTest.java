package vn.edu.ptit.ielts;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.edu.ptit.ielts.dto.UserRecord;
import vn.edu.ptit.ielts.enums.Role;
import vn.edu.ptit.ielts.repository.*;
import vn.edu.ptit.ielts.service.impl.WritingContentHandler;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties="app.demo.enabled=false")
@Testcontainers
class ModuleTwoWritingApiTest {
    @Container static final MySQLContainer<?> MYSQL=new MySQLContainer<>("mysql:8.4").withDatabaseName("ielts_writing_tests").withUsername("test_app").withPassword("test-password");
    @DynamicPropertySource static void database(DynamicPropertyRegistry r){
        r.add("spring.datasource.url",()->MYSQL.getJdbcUrl()+"?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true");
        r.add("spring.datasource.username",MYSQL::getUsername);r.add("spring.datasource.password",MYSQL::getPassword);
    }
    @LocalServerPort int port;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired UserJpaRepository userEntities;
    @Autowired ClassroomJpaRepository classroomEntities;
    @Autowired ClassMemberJpaRepository memberEntities;
    @Autowired ResourceJpaRepository resourceEntities;
    @Autowired ResourceVersionJpaRepository versionEntities;
    @Autowired ResourceAssetJpaRepository resourceAssetEntities;
    @Autowired ResourceLabelJpaRepository resourceLabelEntities;
    @Autowired AssignmentJpaRepository assignmentEntities;
    @Autowired AssignmentRecipientJpaRepository recipientEntities;
    @Autowired AssetJpaRepository assetEntities;
    @Autowired PasswordEncoder passwords;
    UserRecord teacher,otherTeacher,student;
    String hash;
    @BeforeEach void fixtures(){
        recipientEntities.deleteAllInBatch();assignmentEntities.deleteAllInBatch();
        resourceAssetEntities.deleteAllInBatch();resourceLabelEntities.deleteAllInBatch();
        versionEntities.deleteAllInBatch();resourceEntities.deleteAllInBatch();assetEntities.deleteAllInBatch();
        memberEntities.deleteAllInBatch();classroomEntities.deleteAllInBatch();userEntities.deleteAllInBatch();
        hash=passwords.encode("TestPass@123");
        teacher=users.create("teacher@test.local",hash,"Giảng viên Một",Role.TEACHER);
        otherTeacher=users.create("other@test.local",hash,"Giảng viên Hai",Role.TEACHER);
        student=users.create("student@test.local",hash,"Học viên Một",Role.STUDENT);
    }
    final class Client {
        final HttpClient http=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).connectTimeout(Duration.ofSeconds(5)).build();
        String token;
        void csrf()throws Exception{var r=send("GET","/api/auth/csrf",null);assertThat(r.statusCode()).isEqualTo(200);token=body(r).path("token").asText();}
        Client login(String email)throws Exception{csrf();assertThat(send("POST","/api/auth/login",Map.of("email",email,"password","TestPass@123")).statusCode()).isEqualTo(200);csrf();return this;}
        HttpResponse<String> send(String method,String path,Object payload)throws Exception{return request(method,path,payload,true);}
        HttpResponse<String> request(String method,String path,Object payload,boolean csrf)throws Exception{
            var b=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(20)).header("Accept","application/json");
            if(!method.equals("GET")&&csrf)b.header("X-CSRF-TOKEN",token);
            if(payload!=null)b.header("Content-Type","application/json");
            b.method(method,payload==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(payload)));
            return http.send(b.build(),HttpResponse.BodyHandlers.ofString());
        }
    }
    JsonNode body(HttpResponse<String> r)throws Exception{return json.readTree(r.body());}
    ObjectNode input(String title,String task,String activity){
        boolean main=activity.equals("MAIN");
        var root=json.createObjectNode().put("title",title).put("skill","WRITING").put("resourceKind",main?"MAIN":"REMEDIAL").put("activityType",main?"WRITING_ESSAY":"GUIDED")
            .put("topic","Education").put("levelFramework","IELTS_PRACTICE").put("levelLabel","").put("supportLevel",main?"INDEPENDENT":"GUIDED");
        var c=root.putObject("content").put("taskType",task).put("activity",activity).put("prompt","Discuss the role of education.").put("instructions","Explain and support your ideas.")
            .put("minimumWords",main?(task.equals("TASK_1")?150:250):0).put("writingSeconds",600).putNull("visualAssetId")
            .put("visualType",task.equals("TASK_1")?"BAR_CHART":"NONE").put("hasTimeDimension",false).put("visualUnit","")
            .put("originalSentence","").put("originalMeaning","").put("grammarSourceKind","DEMO_AUTHORING").put("teacherConfirmed",false);
        c.putArray("visualGroups");c.putArray("visualPeriods");c.putArray("checklist").add("Keep the original meaning");
        c.set("assessmentCriteria",json.valueToTree(WritingContentHandler.criteria(task)));c.putArray("vocabulary");c.putArray("activities");
        var t=root.putObject("teaching").put("teacherNotes","PRIVATE_WRITING_NOTE").put("modelResponse","PRIVATE_MODEL").put("rubricSourceUrl",WritingContentHandler.RUBRIC_URL);
        t.putArray("sampleAnswers");root.putObject("source").put("url","").put("note","Original test material");
        if(!main){
            String type=switch(activity){case "OVERVIEW_SELECTION"->"SELECT_FEATURES";case "DESCRIPTIVE_LANGUAGE"->"PARAPHRASE";case "TOPIC_VOCABULARY"->"VOCAB_CONTEXT";case "IDEA_LINKING"->"LINK_IDEAS";default->"REPAIR_SENTENCE";};
            var first=c.withArray("activities").addObject().put("id","guide-1").put("type",type).put("stage","GUIDED").put("instruction","Explain your choice.").put("context","Relevant context").put("example","");
            first.putArray("items").add("First idea").add("Second idea");
            var transfer=c.withArray("activities").addObject().put("id","transfer-1").put("type","TRANSFER").put("stage","TRANSFER").put("instruction","Apply to a new situation.").put("context","New context").put("example","");transfer.putArray("items");
            var sample=t.withArray("sampleAnswers").addObject().put("activityId","guide-1").put("explanation","PRIVATE_FEEDBACK");
            sample.putArray("acceptedExamples").add("PRIVATE_ACCEPTED_EXAMPLE");
            if(activity.equals("TOPIC_VOCABULARY"))c.withArray("vocabulary").addObject().put("term","practical skills").put("meaning","skills for practical tasks").put("collocation","develop practical skills").put("example","Students develop practical skills through projects.").put("level","");
            if(activity.equals("GRAMMAR_REPAIR"))c.put("originalSentence","Although courses are useful, but theory matters.").put("originalMeaning","Express contrast.").put("teacherConfirmed",true);
        }
        return root;
    }
    JsonNode draft(Client owner,ObjectNode input)throws Exception{var r=owner.send("POST","/api/resources",input);assertThat(r.statusCode()).as(r.body()).isEqualTo(201);return body(r);}
    String publishPath(JsonNode r){return "/api/resources/"+r.path("id").asLong()+"/versions/"+r.path("version").path("id").asLong()+"/publish";}
    void publish(Client owner,JsonNode r)throws Exception{var response=owner.send("POST",publishPath(r),null);assertThat(response.statusCode()).as(response.body()).isEqualTo(200);}
    JsonNode room(Client owner)throws Exception{var r=owner.send("POST","/api/classes",Map.of("name","Writing test","description","","requestId",UUID.randomUUID().toString()));assertThat(r.statusCode()).isEqualTo(201);return body(r);}
    void join(Client learner,JsonNode room)throws Exception{assertThat(learner.send("POST","/api/classes/join",Map.of("code",room.path("joinCode").asText())).statusCode()).isEqualTo(200);}
    ObjectNode assignment(JsonNode room,JsonNode resource,String mode,List<Long> students){
        var p=json.createObjectNode().put("requestId",UUID.randomUUID().toString()).put("classroomId",room.path("id").asLong()).put("resourceVersionId",resource.path("version").path("id").asLong())
            .put("purpose",resource.path("version").path("resourceKind").asText().equals("MAIN")?"PRACTICE":"REMEDIAL").put("recipientMode",mode)
            .put("allowLate",false).put("maxAttempts",1).put("feedbackPolicy","TEACHER_RELEASE").putNull("opensAt").putNull("dueAt");
        p.set("studentIds",json.valueToTree(students));return p;
    }
    byte[] image()throws Exception{var bytes=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(3,3,BufferedImage.TYPE_INT_RGB),"png",bytes);return bytes.toByteArray();}
    HttpResponse<String> upload(Client c,byte[] data,String route,boolean csrf)throws Exception{
        var bytes=new ByteArrayOutputStream();bytes.write("--image-boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"sample.png\"\r\nContent-Type: image/png\r\n\r\n".getBytes());
        bytes.write(data);bytes.write("\r\n--image-boundary--\r\n".getBytes());
        var b=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+route)).timeout(Duration.ofSeconds(30)).header("Content-Type","multipart/form-data; boundary=image-boundary");
        if(csrf)b.header("X-CSRF-TOKEN",c.token);
        return c.http.send(b.POST(HttpRequest.BodyPublishers.ofByteArray(bytes.toByteArray())).build(),HttpResponse.BodyHandlers.ofString());
    }
    long uploadImage(Client owner)throws Exception{var r=upload(owner,image(),"/api/assets/images",true);assertThat(r.statusCode()).as(r.body()).isEqualTo(201);return body(r).path("id").asLong();}

    @Test void ownershipAndCsrfAreCheckedOnServer()throws Exception{
        var owner=new Client().login(teacher.email());var learner=new Client().login(student.email());var other=new Client().login(otherTeacher.email());
        var r=draft(owner,input("Private","TASK_2","MAIN"));long id=r.path("id").asLong();
        assertThat(learner.send("POST","/api/resources",input("Forged","TASK_2","MAIN")).statusCode()).isEqualTo(403);
        assertThat(learner.send("GET","/api/resources",null).statusCode()).isEqualTo(403);
        assertThat(other.send("GET","/api/resources/"+id,null).statusCode()).isEqualTo(403);
        assertThat(other.send("PUT","/api/resources/"+id+"/draft",input("Changed","TASK_2","MAIN")).statusCode()).isEqualTo(403);
        assertThat(other.send("POST",publishPath(r),null).statusCode()).isEqualTo(403);
        assertThat(owner.request("POST","/api/resources",input("No CSRF","TASK_2","MAIN"),false).statusCode()).isEqualTo(403);
        assertThat(upload(learner,image(),"/api/assets/images",true).statusCode()).isEqualTo(403);
        assertThat(upload(owner,image(),"/api/assets/images",false).statusCode()).isEqualTo(403);
    }
    @Test void publishingRequiresTaskSpecificImageWordsAndRubric()throws Exception{
        var owner=new Client().login(teacher.email());var p=input("Incomplete","TASK_2","MAIN");p.withObject("content").put("prompt","");
        assertThat(owner.send("POST",publishPath(draft(owner,p)),null).statusCode()).isEqualTo(400);
        p=input("Task one","TASK_1","MAIN");var r=draft(owner,p);assertThat(owner.send("POST",publishPath(r),null).statusCode()).isEqualTo(400);
        p.withObject("content").put("visualAssetId",uploadImage(owner));var valid=draft(owner,p);publish(owner,valid);
        p=input("Wrong rubric","TASK_2","MAIN");p.withObject("content").set("assessmentCriteria",json.valueToTree(WritingContentHandler.criteria("TASK_1")));
        assertThat(owner.send("POST",publishPath(draft(owner,p)),null).statusCode()).isEqualTo(400);
        p=input("Wrong words","TASK_2","MAIN");p.withObject("content").put("minimumWords",150);
        assertThat(owner.send("POST",publishPath(draft(owner,p)),null).statusCode()).isEqualTo(400);
        p=input("Wrong visual","TASK_2","MAIN");p.withObject("content").put("visualAssetId",uploadImage(owner)).put("visualType","BAR_CHART");
        assertThat(owner.send("POST",publishPath(draft(owner,p)),null).statusCode()).isEqualTo(400);
    }
    @Test void onlyFiveApprovedLabelsAndMatchingTaskAreAccepted()throws Exception{
        var owner=new Client().login(teacher.email());long chart=uploadImage(owner);
        for(String label:WritingContentHandler.LABELS){
            String task=List.of("OVERVIEW_SELECTION","DESCRIPTIVE_LANGUAGE").contains(label)?"TASK_1":"TASK_2";
            var p=input(label,task,label);if(task.equals("TASK_1"))p.withObject("content").put("visualAssetId",chart);
            publish(owner,draft(owner,p));
        }
        assertThat(resourceLabelEntities.count()).isEqualTo(5);
        assertThat(owner.send("POST","/api/resources",input("Wrong task","TASK_2","OVERVIEW_SELECTION")).statusCode()).isEqualTo(400);
        assertThat(owner.send("POST","/api/resources",input("Unapproved","TASK_2","ARGUMENT_DEVELOPMENT")).statusCode()).isEqualTo(400);
    }
    @Test void grammarAndGuidedExercisesNeedReviewedContextAndTransfer()throws Exception{
        var owner=new Client().login(teacher.email());var p=input("Grammar","TASK_2","GRAMMAR_REPAIR");
        p.withObject("content").put("teacherConfirmed",false);var r=draft(owner,p);
        assertThat(owner.send("POST",publishPath(r),null).statusCode()).isEqualTo(400);
        p.withObject("content").put("teacherConfirmed",true);var fixed=owner.send("PUT","/api/resources/"+r.path("id").asLong()+"/draft",p);assertThat(fixed.statusCode()).isEqualTo(200);publish(owner,body(fixed));
        p=input("No transfer","TASK_2","IDEA_LINKING");p.withObject("content").withArray("activities").remove(1);
        assertThat(owner.send("POST",publishPath(draft(owner,p)),null).statusCode()).isEqualTo(400);
        p=input("Empty words","TASK_2","TOPIC_VOCABULARY");p.withObject("content").putArray("vocabulary");
        assertThat(owner.send("POST",publishPath(draft(owner,p)),null).statusCode()).isEqualTo(400);
        p=input("Broken answer reference","TASK_2","IDEA_LINKING");((ObjectNode)p.withObject("teaching").withArray("sampleAnswers").get(0)).put("activityId","absent");
        assertThat(owner.send("POST","/api/resources",p).statusCode()).isEqualTo(400);
    }
    @Test void rejectsMalformedContentUnsafeSourcesAndUnknownSkill()throws Exception{
        var owner=new Client().login(teacher.email());var p=input("Unknown","TASK_2","MAIN");p.withObject("content").put("arbitraryField",true);
        assertThat(owner.send("POST","/api/resources",p).statusCode()).isEqualTo(400);
        p=input("Unsupported","TASK_2","MAIN");p.put("skill","READING");assertThat(owner.send("POST","/api/resources",p).statusCode()).isEqualTo(400);
        for(String url:List.of("javascript:alert(1)","/relative")){
            p=input("Unsafe source","TASK_2","MAIN");p.withObject("source").put("url",url);assertThat(owner.send("POST","/api/resources",p).statusCode()).isEqualTo(400);
        }
        p=input("Duplicate IDs","TASK_2","IDEA_LINKING");((ObjectNode)p.withObject("content").withArray("activities").get(1)).put("id","guide-1");
        assertThat(owner.send("POST","/api/resources",p).statusCode()).isEqualTo(400);
        p=input("Null step","TASK_2","IDEA_LINKING");((ObjectNode)p.withObject("content").withArray("activities").get(0)).putNull("type");
        assertThat(owner.send("POST","/api/resources",p).statusCode()).isEqualTo(400);
    }
    @Test void studentGetsImmutableAssignedVersionWithoutPrivateExamples()throws Exception{
        var owner=new Client().login(teacher.email());var learner=new Client().login(student.email());var room=room(owner);join(learner,room);
        var r=draft(owner,input("Original","TASK_2","IDEA_LINKING"));publish(owner,r);
        var assigned=owner.send("POST","/api/assignments",assignment(room,r,"CLASS_SNAPSHOT",List.of()));assertThat(assigned.statusCode()).isEqualTo(201);long id=body(assigned).path("id").asLong();
        var next=owner.send("PUT","/api/resources/"+r.path("id").asLong()+"/draft",input("Revised","TASK_2","IDEA_LINKING"));
        assertThat(next.statusCode()).isEqualTo(200);assertThat(body(next).path("version").path("versionNo").asInt()).isEqualTo(2);publish(owner,body(next));
        var detail=learner.send("GET","/api/assignments/"+id,null);assertThat(detail.statusCode()).isEqualTo(200);
        assertThat(body(detail).path("title").asText()).isEqualTo("Original");assertThat(body(detail).path("assignmentId").asLong()).isEqualTo(id);
        assertThat(body(detail).path("resourceVersionId")).isEqualTo(r.path("version").path("id"));assertThat(body(detail).path("questions")).isEmpty();
        assertThat(detail.body()).doesNotContain("PRIVATE_WRITING_NOTE","PRIVATE_MODEL","PRIVATE_ACCEPTED_EXAMPLE","PRIVATE_FEEDBACK","sampleAnswers","teaching");
        assertThat(body(owner.send("GET","/api/resources/"+r.path("id").asLong()+"/versions",null))).hasSize(2);
    }
    @Test void snapshotSelectedRecipientsAndMembershipControlAccess()throws Exception{
        var owner=new Client().login(teacher.email());var learner=new Client().login(student.email());var room=room(owner);join(learner,room);var r=draft(owner,input("Snapshot","TASK_2","MAIN"));publish(owner,r);
        var a=body(owner.send("POST","/api/assignments",assignment(room,r,"CLASS_SNAPSHOT",List.of())));long id=a.path("id").asLong();
        var later=users.create("later@test.local",hash,"Later",Role.STUDENT);var second=new Client().login(later.email());join(second,room);
        assertThat(body(second.send("GET","/api/assignments",null))).isEmpty();assertThat(second.send("GET","/api/assignments/"+id,null).statusCode()).isEqualTo(403);
        var outsider=users.create("outside@test.local",hash,"Outside",Role.STUDENT);
        assertThat(owner.send("POST","/api/assignments",assignment(room,r,"SELECTED",List.of(outsider.id()))).statusCode()).isEqualTo(400);
        var selected=body(owner.send("POST","/api/assignments",assignment(room,r,"SELECTED",List.of(later.id()))));
        assertThat(learner.send("GET","/api/assignments/"+selected.path("id").asLong(),null).statusCode()).isEqualTo(403);
        assertThat(owner.send("DELETE","/api/classes/"+room.path("id").asLong()+"/members/"+student.id(),null).statusCode()).isEqualTo(204);
        assertThat(learner.send("GET","/api/assignments/"+id,null).statusCode()).isEqualTo(403);assertThat(recipientEntities.countByAssignmentId(id)).isEqualTo(1);
    }
    @Test void replayAndConcurrentRequestsProduceOneAssignment()throws Exception{
        var owner=new Client().login(teacher.email());var owner2=new Client().login(teacher.email());var learner=new Client().login(student.email());var room=room(owner);join(learner,room);var r=draft(owner,input("Concurrent","TASK_2","MAIN"));publish(owner,r);
        var p=assignment(room,r,"CLASS_SNAPSHOT",List.of());var barrier=new CyclicBarrier(2);var pool=Executors.newFixedThreadPool(2);
        try{
            var first=pool.submit(()->{barrier.await();return owner.send("POST","/api/assignments",p);});var second=pool.submit(()->{barrier.await();return owner2.send("POST","/api/assignments",p);});
            var a=first.get();var b=second.get();assertThat(List.of(a.statusCode(),b.statusCode())).containsExactlyInAnyOrder(201,200);assertThat(body(a).path("id")).isEqualTo(body(b).path("id"));
        }finally{pool.shutdownNow();}
        assertThat(owner.send("POST","/api/assignments",p).statusCode()).isEqualTo(200);p.put("maxAttempts",2);assertThat(owner.send("POST","/api/assignments",p).statusCode()).isEqualTo(409);assertThat(assignmentEntities.count()).isEqualTo(1);
    }
    @Test void taskOneImagesArePrivateScheduledAndSupportRange()throws Exception{
        var owner=new Client().login(teacher.email());var learner=new Client().login(student.email());long image=uploadImage(owner);
        assertThat(learner.send("GET","/api/assets/"+image,null).statusCode()).isEqualTo(403);
        var room=room(owner);join(learner,room);var p=input("Image","TASK_1","MAIN");p.withObject("content").put("visualAssetId",image);var r=draft(owner,p);publish(owner,r);
        var a=assignment(room,r,"CLASS_SNAPSHOT",List.of());a.put("opensAt","2099-01-02T00:00:00Z").put("dueAt","2099-01-01T00:00:00Z");
        assertThat(owner.send("POST","/api/assignments",a).statusCode()).isEqualTo(400);a.put("dueAt","2099-01-03T00:00:00Z");var future=body(owner.send("POST","/api/assignments",a));
        assertThat(learner.send("GET","/api/assignments/"+future.path("id").asLong(),null).statusCode()).isEqualTo(403);
        assertThat(learner.send("GET","/api/assets/"+image,null).statusCode()).isEqualTo(403);
        assertThat(owner.send("POST","/api/assignments",assignment(room,r,"CLASS_SNAPSHOT",List.of())).statusCode()).isEqualTo(201);
        var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/assets/"+image)).header("Range","bytes=0-11").GET().build();
        var response=learner.http.send(request,HttpResponse.BodyHandlers.ofByteArray());assertThat(response.statusCode()).isEqualTo(206);assertThat(response.body()).hasSize(12);assertThat(response.headers().firstValue("Content-Type")).contains("image/png");
        owner.send("DELETE","/api/classes/"+room.path("id").asLong()+"/members/"+student.id(),null);
        assertThat(learner.send("GET","/api/assets/"+image,null).statusCode()).isEqualTo(403);
    }
    @Test void imageUploadChecksFormatSizeAndOwnership()throws Exception{
        var owner=new Client().login(teacher.email());var other=new Client().login(otherTeacher.email());
        assertThat(upload(owner,"<svg></svg>".getBytes(),"/api/assets/images",true).statusCode()).isEqualTo(400);
        assertThat(upload(owner,new byte[5*1024*1024+1],"/api/assets/images",true).statusCode()).isEqualTo(413);
        long foreign=uploadImage(other);var p=input("Foreign","TASK_1","MAIN");p.withObject("content").put("visualAssetId",foreign);
        assertThat(owner.send("POST","/api/resources",p).statusCode()).isEqualTo(400);
        byte[] wav=new byte[44];System.arraycopy("RIFF".getBytes(),0,wav,0,4);System.arraycopy("WAVE".getBytes(),0,wav,8,4);
        var audio=upload(owner,wav,"/api/assets",true);assertThat(audio.statusCode()).isEqualTo(201);
        p.withObject("content").put("visualAssetId",body(audio).path("id").asLong());assertThat(owner.send("POST","/api/resources",p).statusCode()).isEqualTo(400);
    }
    @Test void demoBankContainsTwoTasksAndFiveExercisesWithValidContent()throws Exception{
        var owner=new Client().login(teacher.email());long image=uploadImage(owner);JsonNode bank;
        try(var stream=new ClassPathResource("demo/writing-resources.json").getInputStream()){bank=json.readTree(stream);}
        assertThat(bank).hasSize(7);
        for(var entry:bank){
            ObjectNode p=entry.path("resource").deepCopy();var c=p.withObject("content");
            if(c.path("taskType").asText().equals("TASK_1"))c.put("visualAssetId",image);
            if(c.path("activity").asText().equals("GRAMMAR_REPAIR"))c.put("teacherConfirmed",true);
            var r=draft(owner,p);publish(owner,r);
            if(c.path("activity").asText().equals("MAIN"))assertThat(p.path("teaching").path("modelResponse").asText().strip().split("\\s+").length).isGreaterThanOrEqualTo(c.path("minimumWords").asInt());
        }
        assertThat(resourceLabelEntities.count()).isEqualTo(5);
    }
}

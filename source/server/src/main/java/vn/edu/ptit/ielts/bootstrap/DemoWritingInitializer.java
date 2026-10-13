package vn.edu.ptit.ielts.bootstrap;

import vn.edu.ptit.ielts.security.AuthValidation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ptit.ielts.dto.ResourceRequests;
import vn.edu.ptit.ielts.repository.LearningRepository;
import vn.edu.ptit.ielts.repository.UserRepository;
import vn.edu.ptit.ielts.security.UserPrincipal;
import vn.edu.ptit.ielts.service.AssetService;
import vn.edu.ptit.ielts.service.LearningService;

@Component
@Order(20)
@ConditionalOnProperty(name="app.demo.enabled",havingValue="true")
public class DemoWritingInitializer implements ApplicationRunner {
    private static final String MARKER="DEMO_WRITING_V1:";
    private final UserRepository users;
    private final LearningRepository db;
    private final LearningService learning;
    private final AssetService assets;
    private final ObjectMapper json;
    private final String email;
    public DemoWritingInitializer(UserRepository users,LearningRepository db,LearningService learning,AssetService assets,ObjectMapper json,
        @Value("$"+"{app.demo.teacher-email}") String email){this.users=users;this.db=db;this.learning=learning;this.assets=assets;this.json=json;this.email=email;}
    @Override @Transactional(rollbackFor=Exception.class)
    public void run(ApplicationArguments args) throws Exception {
        var teacher=users.findByEmail(AuthValidation.normalizeEmail(email)).orElseThrow();
        db.lockUser(teacher.id());
        var actor=UserPrincipal.from(teacher);
        Long chart=learning.list(actor,"WRITING").stream()
            .filter(r->r.version().source()!=null && r.version().source().path("note").asText().startsWith(MARKER))
            .map(r->r.version().content().path("visualAssetId")).filter(n->n.isIntegralNumber() && db.ownedImage(n.asLong(),actor.id()))
            .map(n->n.asLong()).findFirst().orElse(null);
        com.fasterxml.jackson.databind.JsonNode bank;
        try(var stream=new ClassPathResource("demo/writing-resources.json").getInputStream()){bank=json.readTree(stream);}
        for(var entry:bank){
            String key=entry.path("key").asText();
            if(db.existingSeed(actor.id(),"WRITING",MARKER+key+" |"))continue;
            ObjectNode input=entry.path("resource").deepCopy();
            ObjectNode content=(ObjectNode)input.path("content");
            if(content.path("taskType").asText().equals("TASK_1")){
                if(chart==null){
                    byte[] bytes;
                    try(var stream=new ClassPathResource("demo/writing-transport.png").getInputStream()){bytes=stream.readAllBytes();}
                    chart=assets.storeImage(bytes,"writing-transport-demo.png",actor.id()).id();
                }
                content.put("visualAssetId",chart);
            }
            learning.create(json.treeToValue(input,ResourceRequests.ResourceInput.class),actor);
        }
    }
}

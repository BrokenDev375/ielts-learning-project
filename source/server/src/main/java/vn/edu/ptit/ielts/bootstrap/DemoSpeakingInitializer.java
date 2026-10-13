package vn.edu.ptit.ielts.bootstrap;

import vn.edu.ptit.ielts.security.AuthValidation;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import vn.edu.ptit.ielts.dto.ResourceRequests;
import vn.edu.ptit.ielts.repository.LearningRepository;
import vn.edu.ptit.ielts.repository.UserRepository;
import vn.edu.ptit.ielts.security.UserPrincipal;
import vn.edu.ptit.ielts.service.AssetService;
import vn.edu.ptit.ielts.service.LearningService;

@Component
@Order(20)
@ConditionalOnProperty(name="app.demo.enabled",havingValue="true")
public class DemoSpeakingInitializer implements ApplicationRunner {
    private final UserRepository users; private final LearningService learning; private final AssetService assets;
    private final LearningRepository db; private final ObjectMapper json; private final String email;
    public DemoSpeakingInitializer(UserRepository users,LearningService learning,AssetService assets,LearningRepository db,ObjectMapper json,
        @Value("${app.demo.teacher-email}") String email){this.users=users;this.learning=learning;this.assets=assets;this.db=db;this.json=json;this.email=email;}
    @Override @org.springframework.transaction.annotation.Transactional(rollbackFor=Exception.class)
    public void run(ApplicationArguments args) throws Exception {
        var teacher=users.findByEmail(AuthValidation.normalizeEmail(email)).orElseThrow();
        db.lockUser(teacher.id());
        // Each seed type is added once; existing versions and teacher edits are retained.
        var u=UserPrincipal.from(teacher);
        var criteria=List.of("Fluency and coherence","Lexical resource","Grammatical range and accuracy","Pronunciation");
        var content=json.createObjectNode().put("taskType","MAIN").put("part",2)
            .put("prompt","Describe a hobby you enjoy. Say what it is, when you do it, and why you enjoy it.")
            .put("instructions","Chuẩn bị ý, sau đó nói về trải nghiệm của chính bạn.").put("preparationSeconds",60).put("speakingSeconds",120);
        content.set("checklist",json.valueToTree(List.of("Giới thiệu sở thích","Khi nào và cùng ai","Giải thích lý do yêu thích")));
        content.set("phonemes",json.createArrayNode());content.set("items",json.createArrayNode());content.set("assessmentCriteria",json.valueToTree(criteria));
        seed(u,"Speaking: A hobby you enjoy","MAIN","SPEAKING_RECORDING",content,List.of(),"Đề luyện tự biên soạn; cần giảng viên duyệt.");
        var develop=content.deepCopy().put("taskType","DEVELOPMENT").put("part",0)
            .put("prompt","Talk about a hobby you enjoy. Explain why you like it and give a recent example.");
        develop.set("checklist",json.valueToTree(List.of("Nêu sở thích","Giải thích lý do","Đưa một ví dụ gần đây")));
        seed(u,"Bổ trợ: Phát triển câu trả lời về sở thích","REMEDIAL","OUTLINE",develop,
            List.of("Badminton → with classmates → relax after studying → last Friday: a close match","Cooking → weekends → try new recipes → Sunday: soup for family"),"Outline minh họa cần giảng viên kiểm tra; không có một đáp án duy nhất.");
        if(existing(u.id(),"PRONUNCIATION"))return;
        byte[] wav;
        try(var stream=new ClassPathResource("demo/speaking-theta.wav").getInputStream()){wav=stream.readAllBytes();}
        long audio=assets.store(wav,"demo-theta-synthetic.wav",u.id()).id();
        var phonetic=content.deepCopy().put("taskType","PRONUNCIATION").put("part",0).put("prompt","Luyện âm /θ/ trong từ và câu.")
            .put("instructions","Nghe audio mẫu tổng hợp, luyện từng từ rồi cả câu. Giảng viên cần kiểm tra mẫu trước khi xuất bản.");
        phonetic.set("phonemes",json.valueToTree(List.of("/θ/")));
        phonetic.set("checklist",json.valueToTree(List.of("Đọc từ rồi cụm/câu","Chú ý âm mục tiêu")));
        var items=json.createArrayNode();
        for(var word:List.of(new String[]{"think","/θɪŋk/"},new String[]{"thin","/θɪn/"},new String[]{"three","/θriː/"})) {
            byte[] sample;
            try(var stream=new ClassPathResource("demo/speaking-"+word[0]+".wav").getInputStream()){sample=stream.readAllBytes();}
            long sampleId=assets.store(sample,"demo-"+word[0]+"-synthetic.wav",u.id()).id();
            items.addObject().put("text",word[0]).put("ipa",word[1]).put("audioAssetId",sampleId);
        }
        items.addObject().put("text","I think three friends will arrive on Thursday.").put("ipa","/aɪ θɪŋk θriː frɛndz wɪl əˈraɪv ɑːn ˈθɝːzdeɪ/").put("audioAssetId",audio);
        phonetic.set("items",items);
        seed(u,"Bổ trợ: Phát âm /θ/","REMEDIAL","PRONUNCIATION",phonetic,List.of(),"Audio tổng hợp Microsoft Zira (US) dùng demo, không phải bản thu giám khảo; giảng viên cần kiểm tra phiên âm/audio trước xuất bản.");
    }
    private void seed(UserPrincipal u,String title,String kind,String activity,com.fasterxml.jackson.databind.JsonNode content,List<String> examples,String note) {
        if(existing(u.id(),activity))return;
        learning.create(new ResourceRequests.ResourceInput(title,"SPEAKING",kind,activity,"Hobbies","IELTS_PRACTICE",null,
            kind.equals("MAIN")?"INDEPENDENT":"GUIDED",content,
            json.valueToTree(Map.of("outlineExamples",examples,"teacherNotes",note)),
            json.valueToTree(Map.of("url","","note","DEMO_SPEAKING_V1 — "+note))),u);
    }
    private boolean existing(long owner,String activity) {
        return db.existingSeed(owner,activity);
    }
}

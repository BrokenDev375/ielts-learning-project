package vn.edu.ptit.ielts.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import vn.edu.ptit.ielts.dto.ResourceRequests;
import vn.edu.ptit.ielts.dto.WritingPayloads;
import vn.edu.ptit.ielts.exception.ApiException;
import vn.edu.ptit.ielts.service.ResourceContentHandler;

@Component
public class WritingContentHandler implements ResourceContentHandler {
    public static final String RUBRIC_URL="https://ielts.org/cdn/ielts-guides/ielts-writing-band-descriptors.pdf";
    public static final List<String> LABELS=List.of("OVERVIEW_SELECTION","DESCRIPTIVE_LANGUAGE","TOPIC_VOCABULARY","IDEA_LINKING","GRAMMAR_REPAIR");
    private static final List<String> VISUAL_TYPES=List.of("NONE","BAR_CHART","LINE_GRAPH","PIE_CHART","TABLE","MIXED","PROCESS","MAP");
    private static final List<String> STEP_TYPES=List.of("SELECT_FEATURES","COMPARE_OVERVIEWS","PARAPHRASE","VOCAB_CONTEXT","LINK_IDEAS","REPAIR_SENTENCE","TRANSFER");
    private static final Map<String,List<String>> TARGET_STEPS=Map.of(
        "OVERVIEW_SELECTION",List.of("SELECT_FEATURES","COMPARE_OVERVIEWS"),
        "DESCRIPTIVE_LANGUAGE",List.of("PARAPHRASE"),"TOPIC_VOCABULARY",List.of("VOCAB_CONTEXT"),
        "IDEA_LINKING",List.of("LINK_IDEAS"),"GRAMMAR_REPAIR",List.of("REPAIR_SENTENCE"));
    private final ObjectMapper json;
    public WritingContentHandler(ObjectMapper json){this.json=json;}
    @Override public String skill(){return "WRITING";}
    public static List<String> criteria(String task) {
        return List.of(task.equals("TASK_1")?"Task Achievement":"Task Response",
            "Coherence and Cohesion","Lexical Resource","Grammatical Range and Accuracy");
    }
    private WritingPayloads.Content parse(JsonNode input) {
        try{return json.treeToValue(input,WritingPayloads.Content.class);}
        catch(Exception ex){throw invalid("Nội dung Writing không đúng cấu trúc.");}
    }
    @Override public void validate(ResourceRequests.ResourceInput input,boolean publishing) {
        if(!input.content().isObject() || input.content().toString().length()>100000)throw invalid("Nội dung Writing quá dài hoặc sai định dạng.");
        var c=parse(input.content());
        if(c.taskType()==null || !List.of("TASK_1","TASK_2").contains(c.taskType()))throw invalid("Chọn Task 1 Academic hoặc Task 2.");
        if(c.activity()==null || (!c.activity().equals("MAIN") && !LABELS.contains(c.activity())))throw invalid("Nhóm bài Writing chưa được hỗ trợ.");
        boolean main=c.activity().equals("MAIN");
        if(!input.resourceKind().equals(main?"MAIN":"REMEDIAL") || !input.activityType().equals(main?"WRITING_ESSAY":"GUIDED"))throw invalid("Loại tài liệu không khớp hoạt động Writing.");
        if(List.of("OVERVIEW_SELECTION","DESCRIPTIVE_LANGUAGE").contains(c.activity()) && !c.taskType().equals("TASK_1"))throw invalid("Nhóm bổ trợ này dành cho Task 1.");
        if(c.activity().equals("TOPIC_VOCABULARY") && !c.taskType().equals("TASK_2"))throw invalid("Từ vựng theo chủ đề dành cho Task 2.");
        text(c.prompt(),15000,"Đề bài");text(c.instructions(),10000,"Hướng dẫn");
        if(c.minimumWords()==null || c.minimumWords()<0 || c.minimumWords()>2000 || c.writingSeconds()==null || c.writingSeconds()<1 || c.writingSeconds()>7200)throw invalid("Số từ/thời lượng không hợp lệ.");
        if(c.visualType()==null || !VISUAL_TYPES.contains(c.visualType()) || (c.visualAssetId()!=null && c.visualAssetId()<=0))throw invalid("Thông tin hình không hợp lệ.");
        text(c.visualUnit(),100,"Đơn vị");strings(c.visualGroups(),30,100,"Nhóm dữ liệu");strings(c.visualPeriods(),30,100,"Mốc thời gian");
        strings(c.checklist(),30,1000,"Checklist");strings(c.assessmentCriteria(),10,200,"Tiêu chí đánh giá");
        if(c.vocabulary()==null || c.vocabulary().size()>50)throw invalid("Bảng từ quá dài hoặc sai định dạng.");
        for(var word:c.vocabulary()){
            if(word==null)throw invalid("Từ vựng không hợp lệ.");
            text(word.term(),150,"Từ/cụm");text(word.meaning(),500,"Nghĩa");text(word.collocation(),300,"Kết hợp từ");
            text(word.example(),1000,"Ví dụ");text(word.level(),30,"Mức từ");
            if(publishing && (blank(word.term()) || blank(word.meaning()) || blank(word.example())))throw invalid("Mỗi từ cần từ/cụm, nghĩa trong ngữ cảnh và ví dụ.");
        }
        if(c.activities()==null || c.activities().size()>20)throw invalid("Danh sách hoạt động không hợp lệ.");
        var ids=new HashSet<String>();
        for(var step:c.activities()){
            if(step==null || step.id()==null || !step.id().matches("[A-Za-z0-9_-]{1,64}") || !ids.add(step.id()))throw invalid("Mỗi hoạt động cần mã ổn định và duy nhất.");
            if(step.type()==null || step.stage()==null || !STEP_TYPES.contains(step.type()) || !List.of("MODEL","GUIDED","INDEPENDENT","TRANSFER").contains(step.stage()))throw invalid("Loại bước/mức hỗ trợ không hợp lệ.");
            text(step.instruction(),2000,"Yêu cầu bước");text(step.context(),6000,"Ngữ cảnh");text(step.example(),3000,"Ví dụ hướng dẫn");
            strings(step.items(),20,2000,"Ghi chú/ý");
            if(publishing && blank(step.instruction()))throw invalid("Bổ sung yêu cầu cho từng hoạt động.");
        }
        text(c.originalSentence(),3000,"Câu lỗi");text(c.originalMeaning(),3000,"Ý nghĩa/ngữ cảnh gốc");
        if(c.grammarSourceKind()!=null && !List.of("DEMO_AUTHORING","TEACHER_SUPPLIED").contains(c.grammarSourceKind()))throw invalid("Nguồn câu lỗi không hợp lệ.");
        WritingPayloads.Teaching teaching=null;
        try {
            if(input.teaching()!=null && !input.teaching().isNull()){
                teaching=json.treeToValue(input.teaching(),WritingPayloads.Teaching.class);
                text(teaching.teacherNotes(),10000,"Ghi chú riêng");text(teaching.modelResponse(),15000,"Bài mẫu riêng");url(teaching.rubricSourceUrl());
                if(teaching.sampleAnswers()==null || teaching.sampleAnswers().size()>30)throw invalid("Gợi ý phản hồi không hợp lệ.");
                var answerIds=new HashSet<String>();
                for(var answer:teaching.sampleAnswers()){
                    if(answer==null || !ids.contains(answer.activityId()) || !answerIds.add(answer.activityId()))throw invalid("Gợi ý phản hồi phải gắn một hoạt động đang có.");
                    strings(answer.acceptedExamples(),5,5000,"Phương án tham khảo");text(answer.explanation(),5000,"Giải thích");
                }
            }
            if(input.source()!=null && !input.source().isNull()){
                var source=json.treeToValue(input.source(),WritingPayloads.Source.class);url(source.url());text(source.note(),2000,"Ghi chú nguồn");
            }
        } catch(ApiException ex){throw ex;} catch(Exception ex){throw invalid("Tài liệu tham khảo không đúng cấu trúc.");}
        if(!publishing)return;
        if(blank(c.prompt()))throw invalid("Bổ sung đề bài trước khi xuất bản.");
        if(c.assessmentCriteria().isEmpty())throw invalid("Bổ sung tiêu chí đánh giá.");
        if(main && (new HashSet<>(c.assessmentCriteria()).size()!=4 || !c.assessmentCriteria().containsAll(criteria(c.taskType()))))throw invalid("Bài chính cần đủ bốn tiêu chí đúng Task 1/Task 2.");
        if(main && c.minimumWords()!=(c.taskType().equals("TASK_1")?150:250))throw invalid("Bài chính Academic dùng tối thiểu 150 từ cho Task 1, 250 từ cho Task 2.");
        boolean needsVisual=c.taskType().equals("TASK_1") && (main || List.of("OVERVIEW_SELECTION","DESCRIPTIVE_LANGUAGE").contains(c.activity()));
        if(needsVisual && (c.visualAssetId()==null || c.visualType().equals("NONE")))throw invalid("Bài Task 1 này cần hình gốc và loại hình.");
        if(c.taskType().equals("TASK_2") && (c.visualAssetId()!=null || !c.visualType().equals("NONE")))throw invalid("Task 2 không dùng hình Task 1.");
        if(c.hasTimeDimension() && c.visualPeriods().size()<2)throw invalid("Bài có diễn biến theo thời gian cần ít nhất hai mốc.");
        if(!main){
            if(c.checklist().isEmpty() || c.activities().size()<2)throw invalid("Bài bổ trợ cần checklist, bước hướng dẫn và bước vận dụng.");
            if(c.activities().stream().noneMatch(s->TARGET_STEPS.get(c.activity()).contains(s.type())))throw invalid("Hoạt động không phù hợp nhãn bổ trợ.");
            if(c.activities().stream().noneMatch(s->s.type().equals("TRANSFER") || List.of("INDEPENDENT","TRANSFER").contains(s.stage())))throw invalid("Bổ sung bước tự vận dụng vào tình huống mới.");
            if(teaching==null || teaching.sampleAnswers().stream().noneMatch(a->!a.acceptedExamples().isEmpty() && !blank(a.explanation())))throw invalid("Bài bổ trợ cần phương án tham khảo và giải thích riêng cho giảng viên.");
            if(c.activity().equals("TOPIC_VOCABULARY") && c.vocabulary().isEmpty())throw invalid("Bổ sung bảng từ theo chủ đề.");
            if(c.activity().equals("GRAMMAR_REPAIR") && (blank(c.originalSentence()) || blank(c.originalMeaning()) || c.grammarSourceKind()==null || !c.teacherConfirmed()))throw invalid("Giảng viên cần xác nhận câu lỗi và ngữ cảnh gốc trước xuất bản.");
        }
    }
    @Override public List<Long> audioAssets(JsonNode content){return List.of();}
    @Override public List<Long> imageAssets(JsonNode content){var id=parse(content).visualAssetId();return id==null?List.of():List.of(id);}
    @Override public List<String> labels(JsonNode content){var activity=parse(content).activity();return activity.equals("MAIN")?List.of():List.of(activity);}
    private static boolean blank(String value){return value==null || value.isBlank();}
    private static void text(String value,int max,String name){if(value!=null && value.length()>max)throw invalid(name+" quá dài.");}
    private static void strings(List<String> values,int max,int length,String name){if(values==null || values.size()>max || values.stream().anyMatch(v->blank(v) || v.length()>length))throw invalid(name+" không hợp lệ.");}
    private static void url(String value){if(blank(value))return;text(value,2000,"Link nguồn");try {var uri=URI.create(value);if(!List.of("http","https").contains(uri.getScheme()) || uri.getHost()==null)throw invalid("Link nguồn cần http hoặc https.");}catch(IllegalArgumentException ex){throw invalid("Link nguồn không hợp lệ.");}}
    private static ApiException invalid(String message){return new ApiException(HttpStatus.BAD_REQUEST,"INVALID_WRITING",message);}
}

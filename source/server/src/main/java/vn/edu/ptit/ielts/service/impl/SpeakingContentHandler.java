package vn.edu.ptit.ielts.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import vn.edu.ptit.ielts.dto.ResourceRequests;
import vn.edu.ptit.ielts.exception.ApiException;
import vn.edu.ptit.ielts.service.ResourceContentHandler;

@Component
public class SpeakingContentHandler implements ResourceContentHandler {
    public record Item(String text, String ipa, Long audioAssetId) {}
    public record Content(String taskType, Integer part, String prompt, String instructions,
                          Integer preparationSeconds, Integer speakingSeconds,
                          List<String> checklist, List<String> phonemes, List<Item> items,
                          List<String> assessmentCriteria) {}
    public record Teaching(List<String> outlineExamples, String teacherNotes) {}
    public record Source(String url, String note) {}
    private final ObjectMapper json;
    public SpeakingContentHandler(ObjectMapper json) { this.json=json; }
    @Override public String skill() { return "SPEAKING"; }
    private Content parse(JsonNode content) {
        try { return json.treeToValue(content, Content.class); }
        catch(Exception ex) { throw invalid("Nội dung Speaking không đúng cấu trúc."); }
    }
    @Override public void validate(ResourceRequests.ResourceInput input, boolean publishing) {
        if (!input.content().isObject() || input.content().toString().length()>100000) throw invalid("Nội dung bài quá dài hoặc sai định dạng.");
        var c=parse(input.content());
        if (c.taskType()==null || !List.of("MAIN","DEVELOPMENT","PRONUNCIATION").contains(c.taskType())) throw invalid("Chọn dạng bài Speaking.");
        String kind=c.taskType().equals("MAIN")?"MAIN":"REMEDIAL";
        String activity=switch(c.taskType()){case "MAIN" -> "SPEAKING_RECORDING"; case "DEVELOPMENT" -> "OUTLINE"; default -> "PRONUNCIATION";};
        if (!kind.equals(input.resourceKind()) || !activity.equals(input.activityType())) throw invalid("Dạng bài không khớp loại tài liệu.");
        if (c.part()==null || c.part()<0 || c.part()>3) throw invalid("Phần Speaking phải từ 0 đến 3.");
        text(c.prompt(),15000,"Đề bài"); text(c.instructions(),10000,"Hướng dẫn");
        if (c.preparationSeconds()==null || c.preparationSeconds()<0 || c.preparationSeconds()>600 ||
            c.speakingSeconds()==null || c.speakingSeconds()<1 || c.speakingSeconds()>1800) throw invalid("Thời lượng chuẩn bị/nói không hợp lệ.");
        strings(c.checklist(),30,1000,"Checklist"); strings(c.phonemes(),20,30,"Âm mục tiêu");
        strings(c.assessmentCriteria(),10,1500,"Tiêu chí đánh giá");
        if (c.items()==null || c.items().size()>100) throw invalid("Danh sách từ/câu không hợp lệ.");
        for(var item:c.items()) {
            if (item==null) throw invalid("Từ/câu không hợp lệ.");
            text(item.text(),1000,"Từ/câu"); text(item.ipa(),300,"Phiên âm");
            if (item.audioAssetId()!=null && item.audioAssetId()<=0) throw invalid("Mã audio không hợp lệ.");
        }
        try {
            if (input.teaching()!=null && !input.teaching().isNull()) {
                var t=json.treeToValue(input.teaching(),Teaching.class);
                strings(t.outlineExamples(),10,5000,"Outline tham khảo"); text(t.teacherNotes(),10000,"Ghi chú giảng viên");
            }
            if (input.source()!=null && !input.source().isNull()) {
                var s=json.treeToValue(input.source(),Source.class); text(s.url(),2000,"Nguồn"); text(s.note(),2000,"Ghi chú nguồn");
                if (s.url()!=null && !s.url().isBlank()) {
                    var u=URI.create(s.url());
                    if (!List.of("http","https").contains(u.getScheme()) || u.getHost()==null) throw invalid("Link nguồn cần bắt đầu bằng http hoặc https.");
                }
            }
        } catch(ApiException ex) { throw ex; } catch(Exception ex) { throw invalid("Tài liệu tham khảo không đúng cấu trúc."); }
        if (publishing) {
            if (c.prompt()==null || c.prompt().isBlank()) throw invalid("Bổ sung đề bài trước khi xuất bản.");
            if (c.assessmentCriteria().isEmpty()) throw invalid("Bổ sung tiêu chí đánh giá.");
            if (c.taskType().equals("DEVELOPMENT") && c.checklist().isEmpty()) throw invalid("Bài phát triển câu trả lời cần checklist.");
            if (c.taskType().equals("PRONUNCIATION") && (c.phonemes().isEmpty() || c.items().isEmpty())) throw invalid("Bài phát âm cần âm mục tiêu và từ/câu.");
            if (c.taskType().equals("PRONUNCIATION") && c.items().stream().anyMatch(i->i.text()==null || i.text().isBlank() || i.ipa()==null || i.ipa().isBlank() || i.audioAssetId()==null))
                throw invalid("Mỗi từ/câu cần nội dung, phiên âm và audio mẫu trước khi xuất bản.");
        }
    }
    @Override public List<Long> audioAssets(JsonNode content) { return parse(content).items().stream().map(Item::audioAssetId).filter(java.util.Objects::nonNull).distinct().toList(); }
    @Override public List<String> labels(JsonNode content) {
        return switch(parse(content).taskType()) { case "DEVELOPMENT" -> List.of("SPEAK_DEVELOPMENT"); case "PRONUNCIATION" -> List.of("SPEAK_PRONUNCIATION"); default -> List.of(); };
    }
    private static void text(String value,int max,String name) { if(value!=null && value.length()>max) throw invalid(name+" quá dài."); }
    private static void strings(List<String> values,int max,int length,String name) {
        if(values==null || values.size()>max || values.stream().anyMatch(v->v==null || v.isBlank() || v.length()>length)) throw invalid(name+" không hợp lệ.");
    }
    private static ApiException invalid(String message) { return new ApiException(HttpStatus.BAD_REQUEST,"INVALID_RESOURCE",message); }
}

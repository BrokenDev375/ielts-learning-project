package vn.edu.ptit.ielts.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ptit.ielts.dto.LearningViews;
import vn.edu.ptit.ielts.dto.ResourceRequests;
import vn.edu.ptit.ielts.enums.Role;
import vn.edu.ptit.ielts.exception.ApiException;
import vn.edu.ptit.ielts.repository.LearningRepository;
import vn.edu.ptit.ielts.security.ResourceAccess;
import vn.edu.ptit.ielts.security.UserPrincipal;
import vn.edu.ptit.ielts.service.LearningService;
import vn.edu.ptit.ielts.service.ResourceContentHandler;

@Service
@Transactional(readOnly=true)
public class LearningServiceImpl implements LearningService {
    private final LearningRepository db;
    private final Map<String,ResourceContentHandler> handlers=new HashMap<>();
    private final ObjectMapper json;
    public LearningServiceImpl(LearningRepository db,List<ResourceContentHandler> handlers,ObjectMapper json) {
        this.db=db; this.json=json; for(var h:handlers)this.handlers.put(h.skill(),h);
    }    private ResourceContentHandler handler(String skill){var h=handlers.get(skill);if(h==null)throw new ApiException(HttpStatus.BAD_REQUEST,"SKILL_NOT_SUPPORTED","Kỹ năng này chưa được triển khai.");return h;}
    private void owned(long id,UserPrincipal u,boolean lock){ResourceAccess.requireTeacher(u);long owner=db.owner(id,lock);if(owner<0)throw ResourceAccess.missing();if(owner!=u.id())throw ResourceAccess.forbidden();}
    private void assets(ResourceRequests.ResourceInput i,long owner) {
        for(long id:handler(i.skill()).audioAssets(i.content())) {
            if(!db.ownedAudio(id,owner))
                throw new ApiException(HttpStatus.BAD_REQUEST,"INVALID_ASSET","Audio không tồn tại hoặc không thuộc giảng viên.");
        }
    }
    @Override
    public List<LearningViews.Resource> list(UserPrincipal u,String skill){ResourceAccess.requireTeacher(u);return db.list(u.id(),skill);}
    @Override
    public List<LearningViews.Version> versions(long id,UserPrincipal u){owned(id,u,false);return db.versions(id);}
    @Override
    public LearningViews.Resource get(long id,UserPrincipal u){var v=versions(id,u).get(0);return new LearningViews.Resource(id,v.skill(),v);}
    @Override
    @Transactional public LearningViews.Resource create(ResourceRequests.ResourceInput i,UserPrincipal u) {
        ResourceAccess.requireTeacher(u);var h=handler(i.skill());h.validate(i,false);assets(i,u.id());
        long id=db.createResource(u.id(),i.skill()),v=db.createVersion(id,1,i);
        db.links(v,i.skill(),h.audioAssets(i.content()),h.labels(i.content()));
        return new LearningViews.Resource(id,i.skill(),db.version(v));
    }
    @Override
    @Transactional public LearningViews.Resource save(long id,ResourceRequests.ResourceInput i,UserPrincipal u) {
        owned(id,u,true);var current=db.versions(id).get(0);
        if(!current.skill().equals(i.skill()))throw new ApiException(HttpStatus.BAD_REQUEST,"SKILL_IMMUTABLE","Không thể đổi kỹ năng của bài.");
        var h=handler(i.skill());h.validate(i,false);assets(i,u.id());
        long v=current.id();
        if(current.status().equals("DRAFT"))db.updateVersion(v,i);else v=db.createVersion(id,current.versionNo()+1,i);
        db.links(v,i.skill(),h.audioAssets(i.content()),h.labels(i.content()));
        return new LearningViews.Resource(id,i.skill(),db.version(v));
    }
    @Override
    @Transactional public LearningViews.Version publish(long id,long versionId,UserPrincipal u) {
        owned(id,u,true);var v=db.version(versionId);
        if(v==null || v.resourceId()!=id)throw ResourceAccess.missing();
        if(v.status().equals("PUBLISHED"))return v;
        if(!v.status().equals("DRAFT"))throw new ApiException(HttpStatus.CONFLICT,"VERSION_NOT_DRAFT","Phiên bản không còn là bản nháp.");
        handler(v.skill()).validate(v.input(),true);assets(v.input(),u.id());db.publish(v.id(),u.id());return db.version(v.id());
    }
    @Override
    public List<LearningViews.Assignment> assignments(UserPrincipal u){return db.assignments(u.id(),u.role());}
    @Override
    public LearningViews.Assigned assigned(long id,UserPrincipal u) {
        var a=db.assignment(id);if(a==null)throw ResourceAccess.missing();
        if(u.role()==Role.TEACHER) {
            if(!db.assignmentOwned(id,u.id()))throw ResourceAccess.forbidden();
        } else {
            if(!db.recipient(id,u.id()))throw ResourceAccess.forbidden();
            if(a.opensAt()!=null && Instant.now().isBefore(a.opensAt()))throw new ApiException(HttpStatus.FORBIDDEN,"ASSIGNMENT_NOT_OPEN","Bài chưa đến thời gian mở.");
        }
        var v=db.version(a.resourceVersionId());return LearningViews.Assigned.from(a,u.role()==Role.TEACHER?v:v.student());
    }
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    @Override
    public LearningViews.CreatedAssignment assign(ResourceRequests.Assign i,UserPrincipal u) {
        ResourceAccess.requireTeacher(u);
        // The classroom row serializes identical requests and membership changes during the snapshot.
        Long owner=db.lockClassOwner(i.classroomId());
        if(owner==null)throw ResourceAccess.missing();if(owner!=u.id())throw ResourceAccess.forbidden();
        var v=db.version(i.resourceVersionId());if(v==null)throw ResourceAccess.missing();owned(v.resourceId(),u,true);
        if(!v.status().equals("PUBLISHED"))throw new ApiException(HttpStatus.BAD_REQUEST,"RESOURCE_NOT_PUBLISHED","Xuất bản bài trước khi giao.");
        if(i.dueAt()!=null && i.opensAt()!=null && i.dueAt().isBefore(i.opensAt()))throw new ApiException(HttpStatus.BAD_REQUEST,"INVALID_DATES","Hạn nộp phải sau thời gian mở.");
        if(v.resourceKind().equals("REMEDIAL")!=i.purpose().equals("REMEDIAL"))throw new ApiException(HttpStatus.BAD_REQUEST,"INVALID_PURPOSE","Bài bổ trợ cần mục đích giao bài là Bổ trợ.");
        var students=i.studentIds().stream().distinct().sorted().toList();
        if(i.recipientMode().equals("CLASS_SNAPSHOT") && !students.isEmpty())throw new ApiException(HttpStatus.BAD_REQUEST,"INVALID_RECIPIENTS","Giao cả lớp không kèm danh sách riêng.");
        String fingerprint=fingerprint(i,students);
        Long replay=db.request(u.id(),i.requestId().toString());
        if(replay!=null) {
            if(!fingerprint.equals(db.fingerprint(replay)))throw new ApiException(HttpStatus.CONFLICT,"REQUEST_ID_REUSED","Yêu cầu đã được dùng với nội dung khác.");
            return new LearningViews.CreatedAssignment(db.assignment(replay),false);
        }
        var members=db.classStudentIds(i.classroomId());
        var recipients=i.recipientMode().equals("CLASS_SNAPSHOT")?members:students;
        if(recipients.isEmpty() || !members.containsAll(recipients))throw new ApiException(HttpStatus.BAD_REQUEST,"INVALID_RECIPIENTS","Chọn ít nhất một học viên đang thuộc lớp.");
        long id=db.createAssignment(i,u.id(),fingerprint,recipients);
        return new LearningViews.CreatedAssignment(db.assignment(id),true);
    }
    private String fingerprint(ResourceRequests.Assign i,List<Long> students) {
        try {
            var canonical=new ResourceRequests.Assign(i.requestId(),i.classroomId(),i.resourceVersionId(),i.purpose(),i.recipientMode(),students,i.opensAt(),i.dueAt(),i.allowLate(),i.maxAttempts(),i.feedbackPolicy());
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsBytes(canonical)));
        } catch(Exception ex){throw new IllegalStateException(ex);}
    }
}

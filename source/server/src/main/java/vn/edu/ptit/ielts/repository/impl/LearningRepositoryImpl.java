package vn.edu.ptit.ielts.repository.impl;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ptit.ielts.dto.LearningViews;
import vn.edu.ptit.ielts.dto.ResourceRequests;
import vn.edu.ptit.ielts.entity.AssetEntity;
import vn.edu.ptit.ielts.entity.AssignmentEntity;
import vn.edu.ptit.ielts.entity.AssignmentRecipientEntity;
import vn.edu.ptit.ielts.entity.ResourceAssetEntity;
import vn.edu.ptit.ielts.entity.ResourceEntity;
import vn.edu.ptit.ielts.entity.ResourceLabelEntity;
import vn.edu.ptit.ielts.entity.ResourceVersionEntity;
import vn.edu.ptit.ielts.enums.Role;
import vn.edu.ptit.ielts.repository.AssetJpaRepository;
import vn.edu.ptit.ielts.repository.AssignmentJpaRepository;
import vn.edu.ptit.ielts.repository.AssignmentRecipientJpaRepository;
import vn.edu.ptit.ielts.repository.ClassMemberJpaRepository;
import vn.edu.ptit.ielts.repository.ClassroomJpaRepository;
import vn.edu.ptit.ielts.repository.LearningLabelJpaRepository;
import vn.edu.ptit.ielts.repository.LearningRepository;
import vn.edu.ptit.ielts.repository.ResourceAssetJpaRepository;
import vn.edu.ptit.ielts.repository.ResourceJpaRepository;
import vn.edu.ptit.ielts.repository.ResourceLabelJpaRepository;
import vn.edu.ptit.ielts.repository.ResourceVersionJpaRepository;
import vn.edu.ptit.ielts.repository.UserJpaRepository;

/** Persistence boundary: entities stay in transactions; HTTP callers receive DTOs. */

@Repository
@Transactional(readOnly=true)
public class LearningRepositoryImpl implements LearningRepository {
    private final ResourceJpaRepository resources;
    private final ResourceVersionJpaRepository versions;
    private final ResourceAssetJpaRepository resourceAssets;
    private final ResourceLabelJpaRepository resourceLabels;
    private final LearningLabelJpaRepository labels;
    private final AssignmentJpaRepository assignments;
    private final AssignmentRecipientJpaRepository recipients;
    private final AssetJpaRepository assets;
    private final UserJpaRepository users;
    private final ClassroomJpaRepository classrooms;
    private final ClassMemberJpaRepository members;
    public LearningRepositoryImpl(ResourceJpaRepository resources,ResourceVersionJpaRepository versions,
        ResourceAssetJpaRepository resourceAssets,ResourceLabelJpaRepository resourceLabels,
        LearningLabelJpaRepository labels,AssignmentJpaRepository assignments,
        AssignmentRecipientJpaRepository recipients,AssetJpaRepository assets,UserJpaRepository users,
        ClassroomJpaRepository classrooms,ClassMemberJpaRepository members) {
        this.resources=resources;this.versions=versions;this.resourceAssets=resourceAssets;
        this.resourceLabels=resourceLabels;this.labels=labels;this.assignments=assignments;
        this.recipients=recipients;this.assets=assets;this.users=users;this.classrooms=classrooms;this.members=members;
    }
    @Override
    public List<LearningViews.Resource> list(long owner,String skill) {
        return versions.latest(owner,skill).stream().map(v->new LearningViews.Resource(v.getResource().getId(),v.getSkill(),v.view())).toList();
    }
    @Override
    public long owner(long id,boolean lock) {
        return (lock?resources.lockById(id):resources.findByIdAndArchivedAtIsNull(id)).map(r->r.getOwner().getId()).orElse(-1L);
    }
    @Override
    public List<LearningViews.Version> versions(long id){return versions.findByResourceIdOrderByVersionNoDesc(id).stream().map(ResourceVersionEntity::view).toList();}
    @Override
    public LearningViews.Version version(long id){return versions.findById(id).map(ResourceVersionEntity::view).orElse(null);}
    @Override
    @Transactional public long createResource(long owner,String skill){return resources.saveAndFlush(new ResourceEntity(users.getReferenceById(owner),skill)).getId();}
    @Override
    @Transactional public long createVersion(long id,int no,ResourceRequests.ResourceInput input){return versions.saveAndFlush(new ResourceVersionEntity(resources.getReferenceById(id),no,input)).getId();}
    @Override
    @Transactional public void updateVersion(long id,ResourceRequests.ResourceInput input) {
        var v=versions.findById(id).orElseThrow();
        if(!v.getStatus().equals("DRAFT"))throw new IllegalStateException("Only draft versions can be edited");
        v.update(input);versions.flush();
    }
    @Override
    @Transactional public void links(long id,String skill,List<Long> assetIds,List<String> labelCodes) {
        links(id,skill,assetIds,List.of(),labelCodes);
    }
    @Override
    @Transactional public void links(long id,String skill,List<Long> assetIds,List<Long> imageIds,List<String> labelCodes) {
        resourceAssets.deleteForVersion(id);resourceLabels.deleteForVersion(id);
        var version=versions.getReferenceById(id);
        resourceAssets.saveAllAndFlush(assetIds.stream().distinct().map(a->new ResourceAssetEntity(version,assets.getReferenceById(a))).toList());
        resourceAssets.saveAllAndFlush(imageIds.stream().distinct().map(a->new ResourceAssetEntity(version,assets.getReferenceById(a),"VISUAL")).toList());
        resourceLabels.saveAllAndFlush(labelCodes.stream().distinct().map(c->new ResourceLabelEntity(version,labels.getReferenceById(c),skill)).toList());
    }
    @Override
    @Transactional public void publish(long id,long teacher) {
        var v=versions.findById(id).orElseThrow();var now=Instant.now();
        v.setStatus("PUBLISHED");v.setPublishedAt(now);v.setReviewedAt(now);v.setReviewer(users.getReferenceById(teacher));versions.flush();
    }
    @Override
    public List<LearningViews.Assignment> assignments(long actor,Role role){return role==Role.TEACHER?assignments.teacherList(actor):assignments.studentList(actor);}
    @Override
    public LearningViews.Assignment assignment(long id){return assignments.viewById(id).orElse(null);}
    @Override
    public Long request(long teacher,String requestId){return assignments.findByCreatedByIdAndCreateRequestId(teacher,requestId).map(AssignmentEntity::getId).orElse(null);}
    @Override
    public String fingerprint(long id){return assignments.findById(id).orElseThrow().getRequestFingerprint();}
    @Override
    public boolean assignmentOwned(long id,long teacher){return assignments.existsByIdAndCreatedById(id,teacher);}
    @Override
    public boolean recipient(long id,long student){return assignments.visibleToStudent(id,student)>0;}
    @Override
    @Transactional public Long lockClassOwner(long id){return classrooms.lockById(id).map(c->c.getTeacher().getId()).orElse(null);}
    @Override
    public List<Long> classStudentIds(long id){return members.studentIds(id);}
    @Override
    @Transactional public long createAssignment(ResourceRequests.Assign input,long teacher,String fingerprint,List<Long> students) {
        var assignment=assignments.saveAndFlush(new AssignmentEntity(classrooms.getReferenceById(input.classroomId()),
            users.getReferenceById(teacher),versions.getReferenceById(input.resourceVersionId()),input,fingerprint));
        recipients.saveAllAndFlush(students.stream().map(s->new AssignmentRecipientEntity(assignment,users.getReferenceById(s))).toList());
        return assignment.getId();
    }
    @Override
    public boolean ownedAudio(long id,long owner){return assets.existsByIdAndOwnerIdAndStatusAndMimeTypeStartingWith(id,owner,"READY","audio/");}
    @Override
    public boolean ownedImage(long id,long owner){return assets.existsByIdAndOwnerIdAndStatusAndMimeTypeStartingWith(id,owner,"READY","image/");}
    @Override
    public boolean assetVisible(long id,long actor,Role role){return role==Role.TEACHER?assets.existsByIdAndOwnerIdAndStatus(id,actor,"READY"):assets.visibleToStudent(id,actor,Instant.now())>0;}
    @Override
    public AssetEntity asset(long id){return assets.findById(id).orElseThrow();}
    @Override
    @Transactional public LearningViews.Asset storeAsset(long owner,String key,String name,String mime,long size,String sha) {
        var asset=assets.saveAndFlush(new AssetEntity(users.getReferenceById(owner),key,name,mime,size,sha));
        return new LearningViews.Asset(asset.getId(),name,mime,size,"/api/assets/"+asset.getId());
    }
    @Override
    @Transactional public void lockUser(long owner){users.lockById(owner).orElseThrow();}
    @Override
    public boolean existingSeed(long owner,String activity){return versions.seedCandidates(owner,activity).stream().anyMatch(v->v.getSource()!=null && v.getSource().path("note").asText().startsWith("DEMO_SPEAKING_V1"));}
    @Override
    public boolean existingSeed(long owner,String skill,String marker){return versions.seedCandidatesForSkill(owner,skill).stream().anyMatch(v->v.getSource()!=null && v.getSource().path("note").asText().startsWith(marker));}
}

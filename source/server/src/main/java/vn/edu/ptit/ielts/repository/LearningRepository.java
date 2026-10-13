package vn.edu.ptit.ielts.repository;

import java.util.List;
import vn.edu.ptit.ielts.dto.LearningViews;
import vn.edu.ptit.ielts.dto.ResourceRequests;
import vn.edu.ptit.ielts.entity.AssetEntity;
import vn.edu.ptit.ielts.enums.Role;

/** Contract injected by callers; implementation is in repository.impl. */
public interface LearningRepository {
    List<LearningViews.Resource> list(long owner, String skill);
    long owner(long id, boolean lock);
    List<LearningViews.Version> versions(long id);
    LearningViews.Version version(long id);
    long createResource(long owner, String skill);
    long createVersion(long id, int no, ResourceRequests.ResourceInput input);
    void updateVersion(long id, ResourceRequests.ResourceInput input);
    void links(long id, String skill, List<Long> assetIds, List<String> labelCodes);
    void links(long id, String skill, List<Long> assetIds, List<Long> imageIds, List<String> labelCodes);
    void publish(long id, long teacher);
    List<LearningViews.Assignment> assignments(long actor, Role role);
    LearningViews.Assignment assignment(long id);
    Long request(long teacher, String requestId);
    String fingerprint(long id);
    boolean assignmentOwned(long id, long teacher);
    boolean recipient(long id, long student);
    Long lockClassOwner(long id);
    List<Long> classStudentIds(long id);
    long createAssignment(ResourceRequests.Assign input, long teacher, String fingerprint, List<Long> students);
    boolean ownedAudio(long id, long owner);
    boolean ownedImage(long id, long owner);
    boolean assetVisible(long id, long actor, Role role);
    AssetEntity asset(long id);
    LearningViews.Asset storeAsset(long owner, String key, String name, String mime, long size, String sha);
    void lockUser(long owner);
    boolean existingSeed(long owner, String activity);
    boolean existingSeed(long owner, String skill, String marker);
}

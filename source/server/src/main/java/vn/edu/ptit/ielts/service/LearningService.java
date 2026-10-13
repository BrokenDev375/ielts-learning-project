package vn.edu.ptit.ielts.service;

import java.util.List;
import vn.edu.ptit.ielts.dto.LearningViews;
import vn.edu.ptit.ielts.dto.ResourceRequests;
import vn.edu.ptit.ielts.security.UserPrincipal;

/** Contract injected by callers; implementation is in service.impl. */
public interface LearningService {
    List<LearningViews.Resource> list(UserPrincipal u, String skill);
    List<LearningViews.Version> versions(long id, UserPrincipal u);
    LearningViews.Resource get(long id, UserPrincipal u);
    LearningViews.Resource create(ResourceRequests.ResourceInput i, UserPrincipal u);
    LearningViews.Resource save(long id, ResourceRequests.ResourceInput i, UserPrincipal u);
    LearningViews.Version publish(long id, long versionId, UserPrincipal u);
    List<LearningViews.Assignment> assignments(UserPrincipal u);
    LearningViews.Assigned assigned(long id, UserPrincipal u);
    LearningViews.CreatedAssignment assign(ResourceRequests.Assign i, UserPrincipal u);
}

package vn.edu.ptit.ielts.service;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.ptit.ielts.dto.LearningViews;
import vn.edu.ptit.ielts.security.UserPrincipal;

/** Contract injected by callers; implementation is in service.impl. */
public interface AssetService {
    LearningViews.Asset upload(MultipartFile file, UserPrincipal u) throws Exception;
    LearningViews.Asset store(byte[] bytes, String name, long owner) throws Exception;
    ResponseEntity<Resource> get(long id, UserPrincipal u);
}

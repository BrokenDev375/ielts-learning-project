package vn.edu.ptit.ielts.service.impl;

import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.ptit.ielts.dto.LearningViews;
import vn.edu.ptit.ielts.exception.ApiException;
import vn.edu.ptit.ielts.repository.LearningRepository;
import vn.edu.ptit.ielts.security.ResourceAccess;
import vn.edu.ptit.ielts.security.UserPrincipal;
import vn.edu.ptit.ielts.service.AssetService;

@Service
public class AssetServiceImpl implements AssetService {
    private final LearningRepository db;
    private final Path root;
    public AssetServiceImpl(LearningRepository db,@Value("${app.assets.directory:./data/assets}") String directory) throws IOException {
        this.db=db;root=Path.of(directory).toAbsolutePath().normalize();Files.createDirectories(root);
    }
    @Override
    @Transactional(rollbackFor=Exception.class) public LearningViews.Asset upload(MultipartFile file,UserPrincipal u) throws Exception {
        ResourceAccess.requireTeacher(u);
        if(file.isEmpty() || file.getSize()>5*1024*1024)throw new ApiException(HttpStatus.BAD_REQUEST,"INVALID_AUDIO","Audio cần nhỏ hơn hoặc bằng 5 MB.");
        return store(file.getBytes(),file.getOriginalFilename(),u.id());
    }
    @Override
    @Transactional(rollbackFor=Exception.class) public LearningViews.Asset store(byte[] bytes,String name,long owner) throws Exception {
        String mime=audioType(bytes);
        String key=UUID.randomUUID().toString();
        Path target=root.resolve(key);
        try {
            Files.write(target,bytes,StandardOpenOption.CREATE_NEW);
            String sha=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            String original=name==null?"audio":name.replaceAll("[\\\\/\\r\\n]","_");
            if(original.length()>255)original=original.substring(0,255);
            var result=db.storeAsset(owner,key,original,mime,bytes.length,sha);
            if(org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive())
                org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override public void afterCompletion(int status) {
                            if(status!=STATUS_COMMITTED) try {Files.deleteIfExists(target);} catch(IOException ex) {
                                org.slf4j.LoggerFactory.getLogger(AssetServiceImpl.class).warn("Could not clean rolled back asset",ex);
                            }
                        }
                    });
            return result;
        } catch(Exception ex) {Files.deleteIfExists(target);throw ex;}
    }
    @Transactional(readOnly=true)
    @Override
    public ResponseEntity<org.springframework.core.io.Resource> get(long id,UserPrincipal u) {
        boolean allowed=db.assetVisible(id,u.id(),u.role());
        if(!allowed)throw ResourceAccess.forbidden();
        var asset=db.asset(id);
        Path target=root.resolve(asset.getStorageKey()).normalize();
        if(!target.startsWith(root) || !Files.isRegularFile(target))throw ResourceAccess.missing();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(asset.getMimeType()))
            .header("X-Content-Type-Options","nosniff").cacheControl(CacheControl.noStore())
            .body(new FileSystemResource(target));
    }
    private static String audioType(byte[] b) {
        if(b.length>=44 && ascii(b,0,"RIFF") && ascii(b,8,"WAVE"))return "audio/wav";
        if(b.length>=27 && ascii(b,0,"OggS"))return "audio/ogg";
        if(b.length>=10 && (ascii(b,0,"ID3") || ((b[0]&255)==255 && (b[1]&224)==224)))return "audio/mpeg";
        throw new ApiException(HttpStatus.BAD_REQUEST,"INVALID_AUDIO","Chọn tệp WAV, MP3 hoặc OGG có định dạng hợp lệ.");
    }
    private static boolean ascii(byte[] b,int offset,String s){for(int i=0;i<s.length();i++)if(b[offset+i]!=(byte)s.charAt(i))return false;return true;}
}

package vn.edu.ptit.ielts.controller;

import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.ptit.ielts.dto.LearningViews;
import vn.edu.ptit.ielts.security.UserPrincipal;
import vn.edu.ptit.ielts.service.AssetService;

@RestController
@RequestMapping("/api/assets")
public class AssetController {
    private final AssetService service;
    public AssetController(AssetService service){this.service=service;}
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED)
    public LearningViews.Asset upload(@RequestPart("file") MultipartFile file,@AuthenticationPrincipal UserPrincipal u) throws Exception {return service.upload(file,u);}
    @PostMapping(value="/images",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED)
    public LearningViews.Asset uploadImage(@RequestPart("file") MultipartFile file,@AuthenticationPrincipal UserPrincipal u) throws Exception {return service.uploadImage(file,u);}
    @GetMapping("/{id}") public ResponseEntity<Resource> get(@PathVariable long id,@AuthenticationPrincipal UserPrincipal u){return service.get(id,u);}
}

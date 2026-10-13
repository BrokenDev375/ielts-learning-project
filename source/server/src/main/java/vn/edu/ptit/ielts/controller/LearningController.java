package vn.edu.ptit.ielts.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.edu.ptit.ielts.dto.LearningViews;
import vn.edu.ptit.ielts.dto.ResourceRequests;
import vn.edu.ptit.ielts.security.UserPrincipal;
import vn.edu.ptit.ielts.service.LearningService;

@RestController
@RequestMapping("/api")
public class LearningController {
    private final LearningService service;
    public LearningController(LearningService service){this.service=service;}
    @GetMapping("/resources") public List<LearningViews.Resource> list(@RequestParam(required=false) String skill,@AuthenticationPrincipal UserPrincipal u){return service.list(u,skill);}
    @PostMapping("/resources") @ResponseStatus(HttpStatus.CREATED) public LearningViews.Resource create(@Valid @RequestBody ResourceRequests.ResourceInput i,@AuthenticationPrincipal UserPrincipal u){return service.create(i,u);}
    @GetMapping("/resources/{id}") public LearningViews.Resource get(@PathVariable long id,@AuthenticationPrincipal UserPrincipal u){return service.get(id,u);}
    @GetMapping("/resources/{id}/versions") public List<LearningViews.Version> versions(@PathVariable long id,@AuthenticationPrincipal UserPrincipal u){return service.versions(id,u);}
    @PutMapping("/resources/{id}/draft") public LearningViews.Resource save(@PathVariable long id,@Valid @RequestBody ResourceRequests.ResourceInput i,@AuthenticationPrincipal UserPrincipal u){return service.save(id,i,u);}
    @PostMapping("/resources/{id}/versions/{versionId}/publish") public LearningViews.Version publish(@PathVariable long id,@PathVariable long versionId,@AuthenticationPrincipal UserPrincipal u){return service.publish(id,versionId,u);}
    @GetMapping("/assignments") public List<LearningViews.Assignment> assignments(@AuthenticationPrincipal UserPrincipal u){return service.assignments(u);}
    @GetMapping("/assignments/{id}") public LearningViews.Assigned assigned(@PathVariable long id,@AuthenticationPrincipal UserPrincipal u){return service.assigned(id,u);}
    @PostMapping("/assignments") public ResponseEntity<LearningViews.Assignment> assign(@Valid @RequestBody ResourceRequests.Assign i,@AuthenticationPrincipal UserPrincipal u){
        var result=service.assign(i,u);return ResponseEntity.status(result.created()?201:200).body(result.assignment());
    }
}

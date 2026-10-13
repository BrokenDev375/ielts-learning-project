package vn.edu.ptit.ielts.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import vn.edu.ptit.ielts.dto.ResourceRequests;

public interface ResourceContentHandler {
    String skill();
    void validate(ResourceRequests.ResourceInput input, boolean publishing);
    List<Long> audioAssets(JsonNode content);
    List<String> labels(JsonNode content);
}

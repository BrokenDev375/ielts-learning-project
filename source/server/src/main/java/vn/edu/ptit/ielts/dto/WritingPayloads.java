package vn.edu.ptit.ielts.dto;

import java.util.List;

public final class WritingPayloads {
    private WritingPayloads() {}
    public record Vocabulary(String term,String meaning,String collocation,String example,String level) {}
    public record Activity(String id,String type,String stage,String instruction,String context,
                           List<String> items,String example) {}
    public record SampleAnswer(String activityId,List<String> acceptedExamples,String explanation) {}
    public record Content(String taskType,String activity,String prompt,String instructions,
        Integer minimumWords,Integer writingSeconds,Long visualAssetId,String visualType,
        boolean hasTimeDimension,String visualUnit,List<String> visualGroups,List<String> visualPeriods,
        List<String> checklist,List<String> assessmentCriteria,List<Vocabulary> vocabulary,List<Activity> activities,
        String originalSentence,String originalMeaning,String grammarSourceKind,boolean teacherConfirmed) {}
    public record Teaching(String teacherNotes,String modelResponse,List<SampleAnswer> sampleAnswers,String rubricSourceUrl) {}
    public record Source(String url,String note) {}
}

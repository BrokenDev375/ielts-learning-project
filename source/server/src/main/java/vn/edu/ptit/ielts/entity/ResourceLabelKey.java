package vn.edu.ptit.ielts.entity;
import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;
@Embeddable
public class ResourceLabelKey implements Serializable {
    @Column(name="resource_version_id") private Long resourceVersionId;
    @Column(name="label_code",length=64) private String labelCode;
    protected ResourceLabelKey(){}
    public ResourceLabelKey(Long resourceVersionId,String labelCode){this.resourceVersionId=resourceVersionId;this.labelCode=labelCode;}
    public Long getResourceVersionId(){return resourceVersionId;}
    public String getLabelCode(){return labelCode;}
    @Override public boolean equals(Object o){return o instanceof ResourceLabelKey k && Objects.equals(resourceVersionId,k.resourceVersionId) && Objects.equals(labelCode,k.labelCode);}
    @Override public int hashCode(){return Objects.hash(resourceVersionId,labelCode);}
}

package vn.edu.ptit.ielts.entity;
import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;
@Embeddable
public class ResourceAssetKey implements Serializable {
    @Column(name="resource_version_id") private Long resourceVersionId;
    @Column(name="asset_id") private Long assetId;
    @Column(name="asset_role",length=64) private String assetRole;
    protected ResourceAssetKey(){}
    public ResourceAssetKey(Long resourceVersionId,Long assetId,String assetRole){this.resourceVersionId=resourceVersionId;this.assetId=assetId;this.assetRole=assetRole;}
    public Long getResourceVersionId(){return resourceVersionId;}
    public Long getAssetId(){return assetId;}
    public String getAssetRole(){return assetRole;}
    @Override public boolean equals(Object o){return o instanceof ResourceAssetKey k && Objects.equals(resourceVersionId,k.resourceVersionId) && Objects.equals(assetId,k.assetId) && Objects.equals(assetRole,k.assetRole);}
    @Override public int hashCode(){return Objects.hash(resourceVersionId,assetId,assetRole);}
}

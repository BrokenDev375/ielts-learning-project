package vn.edu.ptit.ielts.entity;
import jakarta.persistence.*;
@Entity @Table(name="resource_assets")
public class ResourceAssetEntity {
    @EmbeddedId private ResourceAssetKey id;
    @MapsId("resourceVersionId") @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="resource_version_id") private ResourceVersionEntity version;
    @MapsId("assetId") @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="asset_id") private AssetEntity asset;
    @Column(name="sort_order",nullable=false) private int sortOrder;
    protected ResourceAssetEntity(){}
    public ResourceAssetEntity(ResourceVersionEntity version,AssetEntity asset){this.version=version;this.asset=asset;id=new ResourceAssetKey(version.getId(),asset.getId(),"MODEL_AUDIO");}
}

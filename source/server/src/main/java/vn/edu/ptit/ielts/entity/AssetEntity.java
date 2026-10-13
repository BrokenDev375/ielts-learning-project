package vn.edu.ptit.ielts.entity;

import jakarta.persistence.*;
import java.time.Instant;
import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity @Table(name="assets")
public class AssetEntity extends StoredEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="owner_id",nullable=false) private UserEntity owner;
    @Column(name="storage_key",length=255,nullable=false) private String storageKey;
    @Column(name="original_name",length=255,nullable=false) private String originalName;
    @Column(name="mime_type",length=100,nullable=false) private String mimeType;
    @Column(name="size_bytes",nullable=false) private long sizeBytes;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name="sha256",length=64,nullable=false,columnDefinition="char(64)") private String sha256;
    @Column(name="status",length=16,nullable=false) private String status="READY";
    protected AssetEntity(){}
    public UserEntity getOwner(){return owner;}
    public void setOwner(UserEntity value){this.owner=value;}
    public String getStorageKey(){return storageKey;}
    public void setStorageKey(String value){this.storageKey=value;}
    public String getOriginalName(){return originalName;}
    public void setOriginalName(String value){this.originalName=value;}
    public String getMimeType(){return mimeType;}
    public void setMimeType(String value){this.mimeType=value;}
    public long getSizeBytes(){return sizeBytes;}
    public void setSizeBytes(long value){this.sizeBytes=value;}
    public String getSha256(){return sha256;}
    public void setSha256(String value){this.sha256=value;}
    public String getStatus(){return status;}
    public void setStatus(String value){this.status=value;}
    public AssetEntity(UserEntity owner,String key,String name,String mime,long size,String sha) {
        this.owner=owner;this.storageKey=key;this.originalName=name;this.mimeType=mime;this.sizeBytes=size;this.sha256=sha;
    }
}

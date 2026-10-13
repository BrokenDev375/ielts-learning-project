package vn.edu.ptit.ielts.entity;

import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;

@MappedSuperclass
public abstract class StoredEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @CreationTimestamp(source=SourceType.DB)
    @Column(name="created_at",nullable=false,updatable=false,columnDefinition="datetime(6)") private Instant createdAt;
    public Long getId(){return id;}
    public Instant getCreatedAt(){return createdAt;}
}

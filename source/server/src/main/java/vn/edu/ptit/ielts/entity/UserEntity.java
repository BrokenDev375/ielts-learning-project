package vn.edu.ptit.ielts.entity;

import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SourceType;
import org.hibernate.type.SqlTypes;
import vn.edu.ptit.ielts.dto.UserRecord;
import vn.edu.ptit.ielts.enums.Role;

@Entity
@Table(name="users")
public class UserEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,length=254,unique=true) private String email;
    @Column(name="password_hash",nullable=false,length=100) private String passwordHash;
    @Column(name="full_name",nullable=false,length=100) private String fullName;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable=false,length=16) private Role role;
    @CreationTimestamp(source=SourceType.DB)
    @Column(name="created_at",nullable=false,updatable=false,columnDefinition="timestamp(6)") private Instant createdAt;
    protected UserEntity() {}
    public UserEntity(String email,String passwordHash,String fullName,Role role) {
        this.email=email;this.passwordHash=passwordHash;this.fullName=fullName;this.role=role;
    }
    public Long getId(){return id;}
    public String getEmail(){return email;}
    public String getPasswordHash(){return passwordHash;}
    public String getFullName(){return fullName;}
    public Role getRole(){return role;}
    public Instant getCreatedAt(){return createdAt;}
    public UserRecord record(){return new UserRecord(id,email,passwordHash,fullName,role,createdAt);}
}

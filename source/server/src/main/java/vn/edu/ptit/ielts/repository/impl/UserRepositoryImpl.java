package vn.edu.ptit.ielts.repository.impl;

import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ptit.ielts.dto.UserRecord;
import vn.edu.ptit.ielts.entity.UserEntity;
import vn.edu.ptit.ielts.enums.Role;
import vn.edu.ptit.ielts.repository.UserJpaRepository;
import vn.edu.ptit.ielts.repository.UserRepository;

@Repository
@Transactional(readOnly=true)
public class UserRepositoryImpl implements UserRepository {
    private final UserJpaRepository users;
    public UserRepositoryImpl(UserJpaRepository users){this.users=users;}
    @Override
    public Optional<UserRecord> findByEmail(String email){return users.findByEmail(email).map(UserEntity::record);}
    @Override
    public Optional<UserRecord> findById(long id){return users.findById(id).map(UserEntity::record);}
    @Override
    @Transactional public UserRecord create(String email,String passwordHash,String name,Role role) {
        return users.saveAndFlush(new UserEntity(email,passwordHash,name,role)).record();
    }
}

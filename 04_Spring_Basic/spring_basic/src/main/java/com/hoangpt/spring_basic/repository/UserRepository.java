package com.hoangpt.spring_basic.repository;

import com.hoangpt.spring_basic.entity.user.UserEntity;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.RepositoryDefinition;
import org.springframework.stereotype.Repository;

//Annotation
//@RepositoryDefinition(domainClass = UserEntity.class, idClass = Long.class)
//@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long>, JpaSpecificationExecutor<UserEntity> {
    // different ways to write queries
    // 1. method name
    UserEntity findByUserName(String username);

    // 2. native query
    @Query(value = "SELECT * FROM user_entity WHERE user_name = ?", nativeQuery = true)
    UserEntity findByUserNameNative(String username);

    // 3. jpql
    @Query(value = "SELECT u FROM UserEntity u WHERE u.userName = ?")
    UserEntity findByUserNameJpql(String username);

    // 4. named query
    @Query(name = "UserEntity.findByUserName")
    UserEntity findByUserNameNamed(String username);

    // 5. Criteria API (using Specification)
    // You must extend JpaSpecificationExecutor<UserEntity> to use this
    // For example, finding a user by username:
    static Specification<UserEntity> hasUserName(String username) {
        return (root, query, cb) -> cb.equal(root.get("userName"), username);
    }

    // 6. Querydsl
    // To use Querydsl, you need to add 'querydsl-jpa' and 'querydsl-apt' dependencies to pom.xml
    // and extend QuerydslPredicateExecutor<UserEntity>.
    // Example: repository.findAll(QUserEntity.userEntity.userName.eq(username));
}

package com.hoangpt.spring_basic.entity.user;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;


@Data
@Entity
@Table(name = "java_user_001")
@DynamicUpdate
@DynamicInsert
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "varchar{50}", nullable = false, unique = true)
    private String userName;
    private String userEmail;
}

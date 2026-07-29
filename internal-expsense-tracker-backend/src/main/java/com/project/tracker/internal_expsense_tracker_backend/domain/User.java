package com.project.tracker.internal_expsense_tracker_backend.domain;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;

    @Column (nullable = false, name = "username")
    private String username;

    @Column (nullable = false)
    private String email;

    @JsonIgnore
    @Column (nullable = false, unique = true)
    private String password_hash;

    @Enumerated (EnumType.STRING)
    @Column (nullable = false)
    private Role role;

    @ManyToOne
    @JoinColumn (name = "department_id")
    private Department department;

}

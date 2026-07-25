package com.project.tracker.internal_expsense_tracker_backend.Repo;

import com.project.tracker.internal_expsense_tracker_backend.domain.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeptRepo extends JpaRepository<Department, Long> {
    Optional<Department> findByName(String name);
    Optional<Department> findByCode(String code);

}


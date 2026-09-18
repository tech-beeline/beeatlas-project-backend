/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.beeline.projectbackend.domain.ProjectUser;

public interface ProjectUserRepository extends JpaRepository<ProjectUser, Integer> {

    boolean existsByProjectIdAndUserId(Integer projectId, Integer userId);
}

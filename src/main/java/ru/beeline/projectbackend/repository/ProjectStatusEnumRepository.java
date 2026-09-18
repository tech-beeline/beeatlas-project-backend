/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.beeline.projectbackend.domain.ProjectStatusEnum;

import java.util.Optional;

public interface ProjectStatusEnumRepository extends JpaRepository<ProjectStatusEnum, Integer> {

    Optional<ProjectStatusEnum> findByName(String name);
}

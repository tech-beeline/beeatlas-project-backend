/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.beeline.projectbackend.domain.AssessmentStatusEnum;

import java.util.Optional;

public interface AssessmentStatusEnumRepository extends JpaRepository<AssessmentStatusEnum, Integer> {

    Optional<AssessmentStatusEnum> findByName(String name);
}

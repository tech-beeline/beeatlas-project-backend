/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.beeline.projectbackend.domain.ArtifactBranch;

import java.util.Optional;

public interface ArtifactBranchRepository extends JpaRepository<ArtifactBranch, Integer> {

    Optional<ArtifactBranch> findByArtifactTypeAndArtifactIdAndNameIgnoreCase(String artifactType,
                                                                              Integer artifactId,
                                                                              String name);
}

package com.onetuks.iflow_sentinel.connector.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.onetuks.iflow_sentinel.connector.domain.project.Project;

public interface ProjectJpaRepository extends JpaRepository<Project, Long> {

  Optional<Project> findByName(String name);
}

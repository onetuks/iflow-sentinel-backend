package com.onetuks.iflow_sentinel.domain.binding;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BindingRepository extends JpaRepository<Binding, Long> {

    List<Binding> findByProjectId(Long projectId);
}

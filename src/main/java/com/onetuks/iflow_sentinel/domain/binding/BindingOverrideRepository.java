package com.onetuks.iflow_sentinel.domain.binding;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BindingOverrideRepository extends JpaRepository<BindingOverride, Long> {

    List<BindingOverride> findByBindingId(Long bindingId);
}

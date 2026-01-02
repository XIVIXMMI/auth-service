package com.hdbank.auth_service.repository;

import com.hdbank.auth_service.entity.AppRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppRoleRepository extends JpaRepository<AppRole, Long> {

    Optional<AppRole> findByNameAndIsDeletedFalse(String name);

}

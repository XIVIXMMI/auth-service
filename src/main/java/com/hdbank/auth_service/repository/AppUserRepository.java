package com.hdbank.auth_service.repository;

import com.hdbank.auth_service.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByIdAndIsDeletedFalse(Long id);

    Optional<AppUser> findByUsernameAndIsDeletedFalse(String username);

    boolean existsByUsernameAndIsDeletedFalse(String username);
}

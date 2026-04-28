package com.project.pawn.authentication.repository;

import com.project.pawn.authentication.model.Credential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CredentialRepository extends JpaRepository<Credential, Long> {

    Optional<Credential> findByCredentialId(String credentialId);

    List<Credential> findAllByUser_Username(String username);
}

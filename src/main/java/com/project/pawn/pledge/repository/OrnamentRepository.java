package com.project.pawn.pledge.repository;

import com.project.pawn.pledge.model.Ornament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrnamentRepository extends JpaRepository<Ornament, Long> {

    Optional<Ornament> findByType(String type);

    List<Ornament> findByIsActiveTrue();

    boolean existsByType(String type);
}

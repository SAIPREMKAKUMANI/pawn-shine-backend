package com.project.pawn.customeronboarding.repository;
import com.project.pawn.customeronboarding.model.IdProofInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IdProofRepository extends JpaRepository<IdProofInfo, Long> {
    boolean existsByIdNumber(String idNumber);
}


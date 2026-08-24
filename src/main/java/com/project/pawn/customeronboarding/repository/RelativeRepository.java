package com.project.pawn.customeronboarding.repository;
import com.project.pawn.customeronboarding.model.RelativeInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RelativeRepository extends JpaRepository<RelativeInfo, Long> {
}


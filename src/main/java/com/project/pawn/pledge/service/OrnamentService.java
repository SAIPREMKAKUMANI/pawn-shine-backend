package com.project.pawn.pledge.service;

import com.project.pawn.pledge.dto.OrnamentDto;
import com.project.pawn.pledge.mapper.PledgeMapper;
import com.project.pawn.pledge.model.Ornament;
import com.project.pawn.pledge.repository.OrnamentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrnamentService {

    private final OrnamentRepository ornamentRepository;
    private final PledgeMapper pledgeMapper;

    @Transactional
    public OrnamentDto createOrnament(OrnamentDto request) {
        log.info("Creating ornament type: {}", request.getType());

        if (ornamentRepository.existsByType(request.getType())) {
            throw new IllegalArgumentException("Ornament type already exists: " + request.getType());
        }

        Ornament ornament = pledgeMapper.toOrnamentEntity(request);
        ornament.setIsActive(true);
        Ornament saved = ornamentRepository.save(ornament);

        log.info("Ornament created with ID: {}", saved.getId());
        return pledgeMapper.toOrnamentDto(saved);
    }

    public OrnamentDto getOrnamentById(Long id) {
        return pledgeMapper.toOrnamentDto(findOrnamentOrThrow(id));
    }

    public List<OrnamentDto> getAllActiveOrnaments() {
        return pledgeMapper.toOrnamentDtoList(ornamentRepository.findByIsActiveTrue());
    }

    public List<OrnamentDto> getAllOrnaments() {
        return pledgeMapper.toOrnamentDtoList(ornamentRepository.findAll());
    }

    @Transactional
    public OrnamentDto updateOrnament(Long id, OrnamentDto request) {
        Ornament ornament = findOrnamentOrThrow(id);

        if (request.getDescription() != null) {
            ornament.setDescription(request.getDescription());
        }
        if (request.getDefaultInterestRate() != null) {
            ornament.setDefaultInterestRate(request.getDefaultInterestRate());
        }
        if (request.getDefaultAmountRate() != null) {
            ornament.setDefaultAmountRate(request.getDefaultAmountRate());
        }
        if (request.getImageUrl() != null) {
            ornament.setImageUrl(request.getImageUrl());
        }
        if (request.getIsActive() != null) {
            ornament.setIsActive(request.getIsActive());
        }

        Ornament saved = ornamentRepository.save(ornament);
        log.info("Ornament updated: {}", id);
        return pledgeMapper.toOrnamentDto(saved);
    }

    /**
     * Internal method for other modules to look up ornament details.
     */
    public Ornament findOrnamentOrThrow(Long id) {
        return ornamentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ornament not found with ID: " + id));
    }
}

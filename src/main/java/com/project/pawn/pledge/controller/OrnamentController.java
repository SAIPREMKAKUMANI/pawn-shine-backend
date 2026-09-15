package com.project.pawn.pledge.controller;

import com.project.pawn.common.service.RequestSanitizationService;
import com.project.pawn.pledge.dto.OrnamentDto;
import com.project.pawn.pledge.service.OrnamentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ornaments")
@RequiredArgsConstructor
public class OrnamentController {

    private final OrnamentService ornamentService;
    private final RequestSanitizationService sanitizationService;

    @PostMapping
    public ResponseEntity<OrnamentDto> createOrnament(@RequestBody OrnamentDto request) {
        sanitizationService.sanitize(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ornamentService.createOrnament(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrnamentDto> getOrnament(@PathVariable Long id) {
        return ResponseEntity.ok(ornamentService.getOrnamentById(id));
    }

    @GetMapping
    public ResponseEntity<List<OrnamentDto>> getAllOrnaments(
            @RequestParam(value = "active_only", defaultValue = "true") boolean activeOnly) {
        List<OrnamentDto> ornaments = activeOnly
                ? ornamentService.getAllActiveOrnaments()
                : ornamentService.getAllOrnaments();
        return ResponseEntity.ok(ornaments);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrnamentDto> updateOrnament(@PathVariable Long id, @RequestBody OrnamentDto request) {
        sanitizationService.sanitize(request);
        return ResponseEntity.ok(ornamentService.updateOrnament(id, request));
    }
}

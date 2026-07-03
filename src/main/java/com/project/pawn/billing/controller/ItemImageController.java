package com.project.pawn.billing.controller;

import com.project.pawn.billing.dto.response.ItemImageResponseDto;
import com.project.pawn.billing.service.ImageHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemImageController {
    private final ImageHandler imageHandler;

    @GetMapping("/{id}/images")
    public ResponseEntity<List<ItemImageResponseDto>> getItemImages(@PathVariable(name = "id") Long itemId) {
        log.info("Fetching item images for itemId={}", itemId);
        return ResponseEntity.ok(imageHandler.fetchItemImages(itemId));
    }
}

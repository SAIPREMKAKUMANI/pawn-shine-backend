package com.project.pawn.pledge.controller;

import com.project.pawn.pledge.dto.ItemDto;
import com.project.pawn.pledge.enums.ItemStatus;
import com.project.pawn.pledge.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping("/{id}")
    public ResponseEntity<ItemDto> getItem(@PathVariable Long id) {
        return ResponseEntity.ok(itemService.getItemById(id));
    }

    @GetMapping("/customer/{custId}")
    public ResponseEntity<List<ItemDto>> getItemsByCustomer(@PathVariable Long custId) {
        return ResponseEntity.ok(itemService.getItemsByCustomer(custId));
    }

    @GetMapping("/customer/{custId}/active")
    public ResponseEntity<List<ItemDto>> getActiveItemsByCustomer(@PathVariable Long custId) {
        return ResponseEntity.ok(itemService.getActiveItemsByCustomer(custId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Page<ItemDto>> getItemsByStatus(
            @PathVariable ItemStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(itemService.getItemsByStatus(status, page, size));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        return ResponseEntity.ok(itemService.getDashboardStats());
    }
}

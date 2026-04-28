package com.project.pawn.customeronboarding.controller;

import com.project.pawn.customeronboarding.dto.CustomerResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/image")
public class ImageController {

    public ResponseEntity<CustomerResponse> uploadImage() {

        // Placeholder for image upload logic
        return ResponseEntity.ok(CustomerResponse.builder()
                .customerId(1L)
                .status("Success")
                .message("Image uploaded successfully")
                .build());
    }
}

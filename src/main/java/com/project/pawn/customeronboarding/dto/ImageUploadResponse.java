package com.project.pawn.customeronboarding.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class ImageUploadResponse {
    private String status;
    private String fileName;
    private String fileDownloadUri;
    private String fileType;
    private Long size;
}

package com.project.pawn.billing.service;

import com.project.pawn.billing.dto.response.ItemImageResponseDto;
import com.project.pawn.billing.model.Bill;
import com.project.pawn.billing.model.ItemImage;
import com.project.pawn.billing.repository.ItemImageRepository;
import com.project.pawn.common.enums.MediaType;
import com.project.pawn.common.service.ImageHandlingService;
import com.project.pawn.pledge.model.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ImageHandler {

    private final ImageHandlingService imageHandlingService;
    private final ItemImageRepository itemImageRepository;

    public void uploadItemImages(Item item, Bill bill, List<MultipartFile> itemImages, MediaType type) {
        for(MultipartFile image : itemImages) {
            String imageUrl = imageHandlingService.uploadImageSafe(image, item.getId(), item.getId(), type);
            ItemImage itemImage = ItemImage.builder()
                .item(item)
                .bill(bill)
                .imageUrl(imageUrl)
                .build();
            itemImageRepository.save(itemImage);
        }
    }

    public List<ItemImageResponseDto> fetchItemImages(Long itemId) {
        List<ItemImage> itemImages = itemImageRepository.findByItemId(itemId);
        return itemImages.stream()
                .map(img -> ItemImageResponseDto.builder()
                        .id(img.getId())
                        .itemId(img.getItem().getId())
                        .imageUrl(img.getImageUrl())
                        .build())
                .toList();
    }
}

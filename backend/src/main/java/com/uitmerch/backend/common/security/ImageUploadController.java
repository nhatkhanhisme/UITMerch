package com.uitmerch.backend.common.security;

import com.uitmerch.backend.common.model.ApiResponse;
import com.uitmerch.backend.common.service.*;
import com.uitmerch.backend.common.exception.*;
import com.uitmerch.backend.common.util.FileUploadResponse;
import com.uitmerch.backend.organization.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.Duration;
import java.util.*;

@RestController @RequestMapping("/api/v1/uploads") @RequiredArgsConstructor
public class ImageUploadController {
    private final StorageService storage;
    private final OrganizationRepository organizations;
    private final RateLimiterService rates;
    @Value("${app.storage.avatar-bucket:avatars}") private String avatarBucket;
    @Value("${app.storage.organization-bucket:org-assets}") private String orgBucket;
    @PostMapping("/avatar") @PreAuthorize("isAuthenticated()")
    public ApiResponse<FileUploadResponse> avatar(@RequestAttribute("userId") String actor,@RequestParam MultipartFile file) {
        limit(actor);
        return ApiResponse.success("Image uploaded.",storage.uploadFile(new SafeImageFile(ImageUploadPolicy.validate(file)),avatarBucket,UUID.fromString(actor).toString()));
    }
    @PostMapping("/organizations/{id}/{kind}") @PreAuthorize("hasRole('ORGANIZER')")
    public ApiResponse<FileUploadResponse> organization(@RequestAttribute("userId") String actor,@PathVariable UUID id,@PathVariable String kind,@RequestParam MultipartFile file) {
        if(!Set.of("logo","cover","merch","events").contains(kind)) throw new ValidationException("Invalid image purpose.");
        organizations.findByIdAndOwnerId(id,UUID.fromString(actor)).orElseThrow(()->new ResourceNotFoundException("Organization not found for this account."));
        limit(actor);
        return ApiResponse.success("Image uploaded.",storage.uploadFile(new SafeImageFile(ImageUploadPolicy.validate(file)),orgBucket,"organizations/"+id+"/"+kind));
    }
    private void limit(String actor) {
        if(!rates.isAllowed("upload:"+actor,20,Duration.ofHours(1))) throw new RateLimitException("Too many uploads. Try again later.",3600);
    }
}

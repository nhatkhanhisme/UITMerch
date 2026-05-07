package com.uitmerch.backend.common.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for successful file upload to Supabase Storage.
 * NFR03: Supabase Storage used for images; Base64/BLOB persistence in DB is strictly prohibited.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "File upload response containing public URL")
public class FileUploadResponse {
    
    @JsonProperty("fileName")
    @Schema(description = "Original file name", example = "hoodie.jpg")
    private String fileName;
    
    @JsonProperty("fileUrl")
    @Schema(description = "Public URL to access the uploaded file", example = "https://storage.supabase.co/...")
    private String fileUrl;
    
    @JsonProperty("fileSize")
    @Schema(description = "File size in bytes", example = "1048576")
    private Long fileSize;
    
    @JsonProperty("mimeType")
    @Schema(description = "MIME type of the uploaded file", example = "image/jpeg")
    private String mimeType;
}

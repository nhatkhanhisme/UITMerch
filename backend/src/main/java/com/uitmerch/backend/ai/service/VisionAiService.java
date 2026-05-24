package com.uitmerch.backend.ai.service;

public interface VisionAiService {
    String describeImage(byte[] imageBytes, String mimeType);
}

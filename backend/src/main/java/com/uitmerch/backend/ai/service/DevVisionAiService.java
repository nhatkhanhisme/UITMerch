package com.uitmerch.backend.ai.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("dev | docker")
public class DevVisionAiService implements VisionAiService {

    @Override
    public String describeImage(byte[] imageBytes, String mimeType) {
        return "áo khoa, trắng, UIT";
    }
}

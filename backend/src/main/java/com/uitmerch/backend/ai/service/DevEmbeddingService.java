package com.uitmerch.backend.ai.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@Profile("dev | docker")
public class DevEmbeddingService implements EmbeddingService {

    @Override
    public float[] embed(String text) {
        Random rng = new Random(text.hashCode());
        float[] vec = new float[768];
        for (int i = 0; i < vec.length; i++) {
            vec[i] = rng.nextFloat() * 2 - 1;
        }
        return vec;
    }
}

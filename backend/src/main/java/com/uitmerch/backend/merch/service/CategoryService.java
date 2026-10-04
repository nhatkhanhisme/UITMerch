package com.uitmerch.backend.merch.service;

import com.uitmerch.backend.merch.dto.CategoryResponse;
import com.uitmerch.backend.merch.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Cacheable(value = "categories", key = "'ordered-responses'")
    @Transactional(readOnly = true)
    public List<CategoryResponse> listAll() {
        return categoryRepository.findAllByOrderByDisplayOrderAsc()
            .stream()
            .map(CategoryResponse::from)
            .toList();
    }
}

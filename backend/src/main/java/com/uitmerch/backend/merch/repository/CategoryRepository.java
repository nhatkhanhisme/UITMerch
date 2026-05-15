package com.uitmerch.backend.merch.repository;

import com.uitmerch.backend.merch.entity.Category;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    Optional<Category> findBySlug(String slug);

    List<Category> findAllByOrderByDisplayOrderAsc();

    @Override
    @Cacheable("categories")
    List<Category> findAll();

    @Override
    @CacheEvict(value = "categories", allEntries = true)
    <S extends Category> S save(S entity);

    @Override
    @CacheEvict(value = "categories", allEntries = true)
    <S extends Category> List<S> saveAll(Iterable<S> entities);

    @Override
    @CacheEvict(value = "categories", allEntries = true)
    void delete(Category entity);

    @Override
    @CacheEvict(value = "categories", allEntries = true)
    void deleteById(UUID id);

    @Override
    @CacheEvict(value = "categories", allEntries = true)
    void deleteAll();
}

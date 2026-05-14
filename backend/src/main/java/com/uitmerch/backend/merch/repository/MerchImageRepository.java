package com.uitmerch.backend.merch.repository;

import com.uitmerch.backend.merch.entity.MerchImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface MerchImageRepository extends JpaRepository<MerchImage, UUID> {

    List<MerchImage> findByMerchIdOrderByPosition(UUID merchId);

    List<MerchImage> findByMerchIdInOrderByPosition(Collection<UUID> merchIds);

    void deleteByMerchId(UUID merchId);
}

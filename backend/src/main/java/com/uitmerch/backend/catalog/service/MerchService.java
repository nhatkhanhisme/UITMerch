package com.uitmerch.backend.catalog.service;

import com.uitmerch.backend.catalog.dto.CreateMerchRequest;
import com.uitmerch.backend.catalog.dto.MerchItemResponse;
import com.uitmerch.backend.catalog.dto.SearchMerchRequest;
import org.springframework.data.domain.Page;

/**
 * Merch catalog service contract.
 */
public interface MerchService {

    Page<MerchItemResponse> searchPublicMerch(SearchMerchRequest request);

    MerchItemResponse createMerch(CreateMerchRequest request);
}
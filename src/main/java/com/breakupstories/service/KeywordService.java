package com.breakupstories.service;

import com.breakupstories.dto.KeywordRequest;
import com.breakupstories.dto.KeywordResponse;
import com.breakupstories.dto.PagedResponse;
import com.breakupstories.model.Keyword;
import com.breakupstories.repository.KeywordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KeywordService {
    private final KeywordRepository keywordRepository;

    public KeywordResponse createKeyword(KeywordRequest request) {
        if (keywordRepository.existsByKeyword(request.getKeyword())) {
            throw new RuntimeException("Keyword already exists: " + request.getKeyword());
        }
        Keyword keyword = Keyword.builder()
                .keyword(request.getKeyword())
                .build();
        return KeywordResponse.fromKeyword(keywordRepository.save(keyword));
    }

    public KeywordResponse updateKeyword(String id, KeywordRequest request) {
        Keyword keyword = keywordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Keyword not found: " + id));
        keyword.setKeyword(request.getKeyword());
        return KeywordResponse.fromKeyword(keywordRepository.save(keyword));
    }

    public void deleteKeyword(String id) {
        if (!keywordRepository.existsById(id)) {
            throw new RuntimeException("Keyword not found: " + id);
        }
        keywordRepository.deleteById(id);
    }

    public KeywordResponse getKeywordById(String id) {
        return KeywordResponse.fromKeyword(
                keywordRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Keyword not found: " + id))
        );
    }

    public PagedResponse<KeywordResponse> getAllKeywords(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Keyword> keywordPage = keywordRepository.findAll(pageable);
        List<KeywordResponse> keywords = keywordPage.getContent().stream()
                .map(KeywordResponse::fromKeyword)
                .collect(Collectors.toList());
        return PagedResponse.of(keywords, page, size, keywordPage.getTotalElements());
    }
} 
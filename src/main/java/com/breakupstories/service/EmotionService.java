package com.breakupstories.service;

import com.breakupstories.dto.EmotionRequest;
import com.breakupstories.dto.EmotionResponse;
import com.breakupstories.dto.PagedResponse;
import com.breakupstories.model.Emotion;
import com.breakupstories.repository.EmotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmotionService {
    private final EmotionRepository emotionRepository;

    public EmotionResponse createEmotion(EmotionRequest request) {
        Emotion emotion = Emotion.builder()
                .type(request.getType())
                .score(request.getScore())
                .build();
        return EmotionResponse.fromEmotion(emotionRepository.save(emotion));
    }

    public EmotionResponse updateEmotion(String id, EmotionRequest request) {
        Emotion emotion = emotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Emotion not found: " + id));
        emotion.setType(request.getType());
        emotion.setScore(request.getScore());
        return EmotionResponse.fromEmotion(emotionRepository.save(emotion));
    }

    public void deleteEmotion(String id) {
        if (!emotionRepository.existsById(id)) {
            throw new RuntimeException("Emotion not found: " + id);
        }
        emotionRepository.deleteById(id);
    }

    public EmotionResponse getEmotionById(String id) {
        return EmotionResponse.fromEmotion(
                emotionRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Emotion not found: " + id))
        );
    }

    public PagedResponse<EmotionResponse> getAllEmotions(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Emotion> emotionPage = emotionRepository.findAll(pageable);
        List<EmotionResponse> emotions = emotionPage.getContent().stream()
                .map(EmotionResponse::fromEmotion)
                .collect(Collectors.toList());
        return PagedResponse.of(emotions, page, size, emotionPage.getTotalElements());
    }
} 
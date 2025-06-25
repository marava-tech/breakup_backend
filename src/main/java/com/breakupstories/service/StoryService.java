package com.breakupstories.service;

import com.breakupstories.dto.CreateStoryRequest;
import com.breakupstories.dto.PagedResponse;
import com.breakupstories.dto.StoryResponse;
import com.breakupstories.model.Story;
import com.breakupstories.repository.StoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoryService {
    
    private final StoryRepository storyRepository;
    
    public StoryResponse createStory(String userId, CreateStoryRequest request) {
        Story story = Story.builder()
                .userId(userId)
                .title(request.getTitle())
                .audioUrl(request.getAudioUrl())
                .shareLink(request.getShareLink())
                .viewCount(0)
                .status(Story.StoryStatus.PROCESSING)
                .contents(request.getContents())
                .tags(request.getTags())
                .emotions(request.getEmotions())
                .keywords(request.getKeywords())
                .build();
        
        Story savedStory = storyRepository.save(story);
        return StoryResponse.fromStory(savedStory);
    }
    
    public PagedResponse<StoryResponse> getStories(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Story> storyPage = storyRepository.findByStatus(Story.StoryStatus.ACTIVE, pageable);
        
        List<StoryResponse> stories = storyPage.getContent().stream()
                .map(StoryResponse::fromStory)
                .collect(Collectors.toList());
        
        return PagedResponse.of(stories, page, size, storyPage.getTotalElements());
    }
    
    public StoryResponse getStoryById(String storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found with ID: " + storyId));
        
        return StoryResponse.fromStory(story);
    }
    
    public void incrementViewCount(String storyId) {
        storyRepository.findById(storyId).ifPresent(story -> {
            story.setViewCount(story.getViewCount() != null ? story.getViewCount() + 1 : 1);
            storyRepository.save(story);
        });
    }
    
    public void updateStoryStatus(String storyId, Story.StoryStatus status) {
        storyRepository.findById(storyId).ifPresent(story -> {
            story.setStatus(status);
            storyRepository.save(story);
        });
    }
} 
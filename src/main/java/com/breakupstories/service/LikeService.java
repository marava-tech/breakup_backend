package com.breakupstories.service;

import com.breakupstories.dto.LikeRequest;
import com.breakupstories.dto.LikeResponse;
import com.breakupstories.dto.PagedResponse;
import com.breakupstories.model.Like;
import com.breakupstories.repository.LikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LikeService {
    private final LikeRepository likeRepository;

    public LikeResponse createLike(String userId, LikeRequest request) {
        if (likeRepository.existsByUserIdAndStoryId(userId, request.getStoryId())) {
            throw new RuntimeException("Like already exists for this story");
        }
        Like like = Like.builder()
                .userId(userId)
                .storyId(request.getStoryId())
                .build();
        Like savedLike = likeRepository.save(like);
        return LikeResponse.fromLike(savedLike);
    }

    public PagedResponse<LikeResponse> getLikes(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Like> likePage = likeRepository.findAll(pageable);
        List<LikeResponse> likes = likePage.getContent().stream()
                .map(LikeResponse::fromLike)
                .collect(Collectors.toList());
        return PagedResponse.of(likes, page, size, likePage.getTotalElements());
    }

    public PagedResponse<LikeResponse> getLikesByUser(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Like> likePage = likeRepository.findByUserId(userId, pageable);
        List<LikeResponse> likes = likePage.getContent().stream()
                .map(LikeResponse::fromLike)
                .collect(Collectors.toList());
        return PagedResponse.of(likes, page, size, likePage.getTotalElements());
    }

    public PagedResponse<LikeResponse> getLikesByStory(String storyId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Like> likePage = likeRepository.findByStoryId(storyId, pageable);
        List<LikeResponse> likes = likePage.getContent().stream()
                .map(LikeResponse::fromLike)
                .collect(Collectors.toList());
        return PagedResponse.of(likes, page, size, likePage.getTotalElements());
    }

    public LikeResponse getLikeById(String likeId) {
        Like like = likeRepository.findById(likeId)
                .orElseThrow(() -> new RuntimeException("Like not found with ID: " + likeId));
        return LikeResponse.fromLike(like);
    }

    public boolean isLiked(String userId, String storyId) {
        return likeRepository.existsByUserIdAndStoryId(userId, storyId);
    }

    public void deleteLike(String likeId, String userId) {
        Like like = likeRepository.findById(likeId)
                .orElseThrow(() -> new RuntimeException("Like not found with ID: " + likeId));
        if (!like.getUserId().equals(userId)) {
            throw new RuntimeException("You can only delete your own likes");
        }
        likeRepository.deleteById(likeId);
    }

    public void deleteLikeByUserAndStory(String userId, String storyId) {
        likeRepository.deleteByUserIdAndStoryId(userId, storyId);
    }
} 
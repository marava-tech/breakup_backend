package com.breakupstories.service;

import com.breakupstories.dto.PagedResponse;
import com.breakupstories.dto.ShortVideoRequest;
import com.breakupstories.dto.ShortVideoResponse;
import com.breakupstories.model.ShortVideo;
import com.breakupstories.repository.ShortVideoLikeRepository;
import com.breakupstories.repository.ShortVideoRepository;
import com.breakupstories.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShortVideoService {

    private final ShortVideoRepository shortVideoRepository;
    private final ShortVideoLikeRepository shortVideoLikeRepository;

    public ShortVideoResponse createShortVideo(ShortVideoRequest request) {
        ShortVideo video = ShortVideo.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .videoUrl(request.getVideoUrl())
                .thumbnailUrl(request.getThumbnailUrl())
                .language(request.getLanguage() != null ? request.getLanguage().name() : null)
                .tags(request.getTags())
                .status(request.getStatus() != null ? request.getStatus() : ShortVideo.VideoStatus.ACTIVE)
                .build();

        ShortVideo saved = shortVideoRepository.save(video);
        return mapToResponse(saved, null);
    }

    public ShortVideoResponse updateShortVideo(String id, ShortVideoRequest request) {
        ShortVideo video = shortVideoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ShortVideo", "id", id));

        if (request.getTitle() != null)
            video.setTitle(request.getTitle());
        if (request.getDescription() != null)
            video.setDescription(request.getDescription());
        if (request.getVideoUrl() != null)
            video.setVideoUrl(request.getVideoUrl());
        if (request.getThumbnailUrl() != null)
            video.setThumbnailUrl(request.getThumbnailUrl());
        if (request.getLanguage() != null)
            video.setLanguage(request.getLanguage().name());
        if (request.getTags() != null)
            video.setTags(request.getTags());
        if (request.getStatus() != null)
            video.setStatus(request.getStatus());

        ShortVideo saved = shortVideoRepository.save(video);
        return mapToResponse(saved, null);
    }

    public void deleteShortVideo(String id) {
        shortVideoRepository.deleteById(id);
    }

    public ShortVideoResponse getShortVideoById(String id, String userId) {
        ShortVideo video = shortVideoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ShortVideo", "id", id));
        return mapToResponse(video, userId);
    }

    public PagedResponse<ShortVideoResponse> getAllShortVideosForAdmin(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ShortVideo> videoPage = shortVideoRepository.findAll(pageable);

        List<ShortVideoResponse> responses = videoPage.getContent().stream()
                .map(v -> mapToResponse(v, null))
                .collect(Collectors.toList());

        return PagedResponse.of(responses, page, size, videoPage.getTotalElements());
    }

    public ShortVideoResponse mapToResponse(ShortVideo video, String userId) {
        boolean isLiked = false;
        if (userId != null) {
            isLiked = shortVideoLikeRepository.existsByUserIdAndVideoId(userId, video.getId());
        }

        return ShortVideoResponse.builder()
                .id(video.getId())
                .title(video.getTitle())
                .description(video.getDescription())
                .videoUrl(video.getVideoUrl())
                .thumbnailUrl(video.getThumbnailUrl())
                .language(video.getLanguage())
                .tags(video.getTags())
                .viewCount(video.getViewCount())
                .likeCount(video.getLikeCount())
                .commentCount(video.getCommentCount())
                .shareCount(video.getShareCount())
                .status(video.getStatus())
                .createdAt(video.getCreatedAt())
                .isLiked(isLiked)
                .build();
    }
}

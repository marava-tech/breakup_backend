package com.breakupstories.service;


import com.breakupstories.dto.BookmarkResponse;
import com.breakupstories.dto.PagedResponse;
import com.breakupstories.dto.StoryResponse;
import com.breakupstories.model.Bookmark;
import com.breakupstories.model.Story;
import com.breakupstories.model.User;
import com.breakupstories.repository.BookmarkRepository;
import com.breakupstories.repository.StoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookmarkService {
    
    private final BookmarkRepository bookmarkRepository;
    private final StoryRepository storyRepository;
    private final LikeService likeService;
    @Lazy
    private final CommentService commentService;
    @Lazy
    private final UserService userService;
    
    public BookmarkResponse createBookmark(String userId, String storyId) {
        // Check if bookmark already exists
        if (bookmarkRepository.existsByUserIdAndStoryId(userId, storyId)) {
            throw new RuntimeException("Bookmark already exists for this story");
        }
        
        // Verify story exists
        if (!storyRepository.existsById(storyId)) {
            throw new RuntimeException("Story not found with ID: " + storyId);
        }
        
        Bookmark bookmark = Bookmark.builder()
                .userId(userId)
                .storyId(storyId)
                .build();
        
        Bookmark savedBookmark = bookmarkRepository.save(bookmark);
        return BookmarkResponse.fromBookmark(savedBookmark);
    }
    
    public PagedResponse<BookmarkResponse> getBookmarks(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Bookmark> bookmarkPage = bookmarkRepository.findAll(pageable);
        
        List<BookmarkResponse> bookmarks = bookmarkPage.getContent().stream()
                .map(BookmarkResponse::fromBookmark)
                .collect(Collectors.toList());
        
        return PagedResponse.of(bookmarks, page, size, bookmarkPage.getTotalElements());
    }
    
    public PagedResponse<BookmarkResponse> getBookmarksByUser(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Bookmark> bookmarkPage = bookmarkRepository.findByUserId(userId, pageable);
        
        List<BookmarkResponse> bookmarks = bookmarkPage.getContent().stream()
                .map(BookmarkResponse::fromBookmark)
                .collect(Collectors.toList());
        
        return PagedResponse.of(bookmarks, page, size, bookmarkPage.getTotalElements());
    }
    
    public PagedResponse<StoryResponse> getBookmarkedStoriesWithDetails(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Bookmark> bookmarkPage = bookmarkRepository.findByUserId(userId, pageable);
        
        List<StoryResponse> stories = bookmarkPage.getContent().stream()
                .map(bookmark -> {
                    try {
                        Story story = storyRepository.findById(bookmark.getStoryId()).orElse(null);
                        if (story == null) {
                            return null;
                        }
                        
                        User user = userService.getUserEntityById(story.getUserId());
                        
                        // Check if user liked this story
                        boolean likedByMe = likeService.isLiked(userId, story.getId());
                        
                        // Check if user bookmarked this story (should be true)
                        boolean bookmarkedByMe = true;
                        
                        long likeCount = likeService.getLikeCount(story.getId());
                        long commentCount = commentService.getCommentCount(story.getId());
                        
                        return StoryResponse.fromStory(story, user, likedByMe, bookmarkedByMe, likeCount, commentCount);
                    } catch (Exception e) {
                        // If story is not found or accessible, return null
                        return null;
                    }
                })
                .filter(story -> story != null) // Filter out null stories
                .collect(Collectors.toList());
        
        return PagedResponse.of(stories, page, size, bookmarkPage.getTotalElements());
    }
    
    public PagedResponse<BookmarkResponse> getBookmarksByStory(String storyId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Bookmark> bookmarkPage = bookmarkRepository.findByStoryId(storyId, pageable);
        
        List<BookmarkResponse> bookmarks = bookmarkPage.getContent().stream()
                .map(BookmarkResponse::fromBookmark)
                .collect(Collectors.toList());
        
        return PagedResponse.of(bookmarks, page, size, bookmarkPage.getTotalElements());
    }
    
    public BookmarkResponse getBookmarkById(String bookmarkId) {
        Bookmark bookmark = bookmarkRepository.findById(bookmarkId)
                .orElseThrow(() -> new RuntimeException("Bookmark not found with ID: " + bookmarkId));
        
        return BookmarkResponse.fromBookmark(bookmark);
    }
    
    public boolean isBookmarked(String userId, String storyId) {
        return bookmarkRepository.existsByUserIdAndStoryId(userId, storyId);
    }
    
    public long getBookmarkCountByUser(String userId) {
        return bookmarkRepository.countByUserId(userId);
    }
    
    public long getBookmarkCountByStory(String storyId) {
        return bookmarkRepository.countByStoryId(storyId);
    }
    
    public void deleteBookmark(String bookmarkId, String userId) {
        Bookmark bookmark = bookmarkRepository.findById(bookmarkId)
                .orElseThrow(() -> new RuntimeException("Bookmark not found with ID: " + bookmarkId));
        
        // Check if user owns this bookmark
        if (!bookmark.getUserId().equals(userId)) {
            throw new RuntimeException("You can only delete your own bookmarks");
        }
        
        bookmarkRepository.deleteById(bookmarkId);
    }
    
    public void deleteBookmarkByUserAndStory(String userId, String storyId) {
        bookmarkRepository.deleteByUserIdAndStoryId(userId, storyId);
    }
} 
package com.breakupstories.service;

import com.breakupstories.dto.BookmarkRequest;
import com.breakupstories.dto.BookmarkResponse;
import com.breakupstories.dto.PagedResponse;
import com.breakupstories.model.Bookmark;
import com.breakupstories.repository.BookmarkRepository;
import lombok.RequiredArgsConstructor;
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
    
    public BookmarkResponse createBookmark(String userId, BookmarkRequest request) {
        // Check if bookmark already exists
        if (bookmarkRepository.existsByUserIdAndStoryId(userId, request.getStoryId())) {
            throw new RuntimeException("Bookmark already exists for this story");
        }
        
        Bookmark bookmark = Bookmark.builder()
                .userId(userId)
                .storyId(request.getStoryId())
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
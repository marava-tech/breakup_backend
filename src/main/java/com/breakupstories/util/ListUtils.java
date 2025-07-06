package com.breakupstories.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for list operations
 */
public class ListUtils {
    
    /**
     * Safely adds elements to a list. If the list is null or empty, creates a new list.
     * If the list already has elements, adds the new elements to the existing list.
     * 
     * @param existingList The existing list (can be null or empty)
     * @param newElements The new elements to add
     * @return The updated list with new elements
     */
    public static <T> List<T> addOrCreate(List<T> existingList, List<T> newElements) {
        if (newElements == null || newElements.isEmpty()) {
            return existingList;
        }
        
        if (existingList == null || existingList.isEmpty()) {
            return new ArrayList<>(newElements);
        }
        
        existingList.addAll(newElements);
        return existingList;
    }
    
    /**
     * Safely adds a single element to a list. If the list is null or empty, creates a new list.
     * If the list already has elements, adds the new element to the existing list.
     * 
     * @param existingList The existing list (can be null or empty)
     * @param newElement The new element to add
     * @return The updated list with the new element
     */
    public static <T> List<T> addOrCreate(List<T> existingList, T newElement) {
        if (newElement == null) {
            return existingList;
        }
        
        if (existingList == null || existingList.isEmpty()) {
            List<T> newList = new ArrayList<>();
            newList.add(newElement);
            return newList;
        }
        
        existingList.add(newElement);
        return existingList;
    }
    
    /**
     * Safely adds string elements to a list. If the list is null or empty, creates a new list.
     * If the list already has elements, adds the new elements to the existing list.
     * Filters out null and empty strings.
     * 
     * @param existingList The existing list (can be null or empty)
     * @param newElements The new string elements to add
     * @return The updated list with new elements
     */
    public static List<String> addOrCreateStrings(List<String> existingList, List<String> newElements) {
        if (newElements == null || newElements.isEmpty()) {
            return existingList;
        }
        
        // Filter out null and empty strings
        List<String> validElements = newElements.stream()
                .filter(element -> element != null && !element.trim().isEmpty())
                .toList();
        
        if (validElements.isEmpty()) {
            return existingList;
        }
        
        if (existingList == null || existingList.isEmpty()) {
            return new ArrayList<>(validElements);
        }
        
        existingList.addAll(validElements);
        return existingList;
    }
    
    /**
     * Safely adds a single string element to a list. If the list is null or empty, creates a new list.
     * If the list already has elements, adds the new element to the existing list.
     * Filters out null and empty strings.
     * 
     * @param existingList The existing list (can be null or empty)
     * @param newElement The new string element to add
     * @return The updated list with the new element
     */
    public static List<String> addOrCreateString(List<String> existingList, String newElement) {
        if (newElement == null || newElement.trim().isEmpty()) {
            return existingList;
        }
        
        if (existingList == null || existingList.isEmpty()) {
            List<String> newList = new ArrayList<>();
            newList.add(newElement.trim());
            return newList;
        }
        
        existingList.add(newElement.trim());
        return existingList;
    }
} 
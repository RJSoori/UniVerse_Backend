package com.example.backend_service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/focus")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class FocusController {

    @Autowired
    private FocusRepository repository;

    @PostMapping("/save")
    public Focus saveFocus(@RequestBody Focus request) {
        // Look for an existing session for this user on this specific date
        return repository.findByUserIdAndFocusDate(request.getUserId(), request.getFocusDate())
            .map(existingSession -> {
                // If found, update the existing record's minutes
                existingSession.setTotalMinutes(existingSession.getTotalMinutes() + request.getTotalMinutes());
                return repository.save(existingSession);
            })
            .orElseGet(() -> {
                // If not found, save the new session as a new entry
                return repository.save(request);
            });
    }

    @GetMapping("/analytics")
    // Endpoint to fetch aggregated focus minutes by date for a specific user
    // formatted for frontend charts
    public List<Map<String, Object>> getAnalytics(@RequestParam String userId) {
        List<Object[]> results = repository.findTotalMinutesByDate(userId);
        List<Map<String, Object>> formattedData = new ArrayList<>();
        
        for (Object[] result : results) {
            Map<String, Object> map = new HashMap<>();// Map each result to a format suitable for the frontend chart
            map.put("date", result[0] != null ? result[0].toString() : "");
            map.put("minutes", result[1] != null ? result[1] : 0);
            formattedData.add(map);
        }
        return formattedData;
    }
}
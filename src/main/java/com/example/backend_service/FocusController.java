package com.example.backend_service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/focus")
public class FocusController {

    @Autowired
    private FocusRepository repository;

    @PostMapping("/save")
    @Transactional
    public Focus saveFocus(@AuthenticationPrincipal Long authStudentId, @RequestBody Focus request) {
        String userId = authStudentId.toString();
        return repository.findByUserIdAndFocusDate(userId, request.getFocusDate())
            .map(existingSession -> {
                existingSession.setTotalMinutes(existingSession.getTotalMinutes() + request.getTotalMinutes());
                return repository.save(existingSession);
            })
            .orElseGet(() -> {
                request.setUserId(userId);
                return repository.save(request);
            });
    }

    @GetMapping("/analytics")
    public List<Map<String, Object>> getAnalytics(@AuthenticationPrincipal Long authStudentId) {
        String userId = authStudentId.toString();
        List<Object[]> results = repository.findTotalMinutesByDate(userId);
        List<Map<String, Object>> formattedData = new ArrayList<>();
        for (Object[] result : results) {
            Map<String, Object> map = new HashMap<>();
            map.put("date", result[0] != null ? result[0].toString() : "");
            map.put("minutes", result[1] != null ? result[1] : 0);
            formattedData.add(map);
        }
        return formattedData;
    }
}

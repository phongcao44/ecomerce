package com.ra.base_spring_boot.dto.req;

import com.ra.base_spring_boot.model.ChatTurn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class GeminiRequest {
    private String prompt;
    private List<ChatTurn> history;
    private Long userId;
    public static class HistoryTurn {
        private String role;
        private String message;
    }
}

package com.talkifyx.message_service.dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReactionGroupDto {
    private String emoji;
    private int count;
    private List<Long> userIds;
}

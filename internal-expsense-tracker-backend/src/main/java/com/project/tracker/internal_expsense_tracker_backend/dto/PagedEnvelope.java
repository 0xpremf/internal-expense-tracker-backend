package com.project.tracker.internal_expsense_tracker_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class PagedEnvelope<T>{
    private long total;
    private int page;
    private int limit;
    private List<T> data;
}

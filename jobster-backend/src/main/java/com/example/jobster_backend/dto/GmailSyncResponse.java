package com.example.jobster_backend.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GmailSyncResponse {
    private int processed;
    private int categorized;
    private int skipped;
    private SyncStats stats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SyncStats {
        private long applied;
        private long interviews;
        private long offers;
        private long rejected;
    }
}

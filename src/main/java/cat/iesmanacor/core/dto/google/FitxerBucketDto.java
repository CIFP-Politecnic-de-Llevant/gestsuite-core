package cat.iesmanacor.core.dto.google;

import lombok.Data;

import java.time.LocalDateTime;

public @Data class FitxerBucketDto {
    private Long idfitxer;
    private String nom;
    private String path;
    private String bucket;
    private LocalDateTime dataCreacio;
    private String url;
}

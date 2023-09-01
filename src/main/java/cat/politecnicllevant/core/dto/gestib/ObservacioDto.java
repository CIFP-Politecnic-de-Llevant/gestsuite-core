package cat.politecnicllevant.core.dto.gestib;

import lombok.Data;

import java.time.LocalDateTime;

public @Data class ObservacioDto {
    private Long idobservacio;
    private LocalDateTime dataCreacio;
    private String descripcio;
    private ObservacioTipusDto tipus;
    private UsuariDto usuari;
}

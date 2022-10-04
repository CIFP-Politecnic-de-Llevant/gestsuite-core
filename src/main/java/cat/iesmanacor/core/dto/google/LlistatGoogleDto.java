package cat.iesmanacor.core.dto.google;

import cat.iesmanacor.core.dto.gestib.UsuariDto;
import lombok.Data;

import java.time.LocalDateTime;

public @Data class LlistatGoogleDto {
    private Long idllistat;
    private String nom;
    private String identificador;
    private String url;
    private LocalDateTime dataCreacio;
    private LlistatGoogleTipusDto llistatGoogleTipus;
    private UsuariDto propietari;
}

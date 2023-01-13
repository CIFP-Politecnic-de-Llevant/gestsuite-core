package cat.iesmanacor.core.dto.gestib;

import cat.iesmanacor.core.dto.MapInterface;
import cat.iesmanacor.core.model.gestib.Observacio;
import cat.iesmanacor.core.model.gestib.ObservacioTipus;
import cat.iesmanacor.core.model.gestib.Usuari;
import lombok.Data;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.time.LocalDateTime;

public @Data class ObservacioDto extends MapInterface<Observacio, ObservacioDto> {
    private Long idobservacio;
    private LocalDateTime dataCreacio;
    private String descripcio;
    private ObservacioTipusDto tipus;
    private UsuariDto usuari;
}

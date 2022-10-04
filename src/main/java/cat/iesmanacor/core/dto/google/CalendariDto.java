package cat.iesmanacor.core.dto.google;

import cat.iesmanacor.core.dto.gestib.UsuariDto;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;


public @Data class CalendariDto {
    private Long idcalendari;
    private CalendariTipusDto calendariTipus;
    private String gestibGrup;
    private String gsuiteEmail;
    private String gsuiteNom;
    private String gsuiteDescripcio;
    private Set<UsuariDto> usuaris = new HashSet<>();
}

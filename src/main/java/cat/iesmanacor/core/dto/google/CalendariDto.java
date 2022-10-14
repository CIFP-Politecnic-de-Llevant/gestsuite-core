package cat.iesmanacor.core.dto.google;

import cat.iesmanacor.core.dto.gestib.UsuariDto;
import cat.iesmanacor.core.model.google.GrupCorreu;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;


public @Data class CalendariDto {
    private Long idcalendari;
    private CalendariTipusDto calendariTipus;
    private String gsuiteEmail;
    private String gsuiteNom;
    private String gsuiteDescripcio;
    private Set<UsuariDto> usuarisLectura = new HashSet<>();
    private Set<UsuariDto> usuarisEdicio = new HashSet<>();
    private Set<GrupCorreuDto> grupCorreuLectura = new HashSet<>();
    private Set<GrupCorreuDto> grupCorreuEdicio = new HashSet<>();
}

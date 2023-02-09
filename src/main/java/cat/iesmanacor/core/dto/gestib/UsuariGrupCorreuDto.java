package cat.iesmanacor.core.dto.gestib;

import cat.iesmanacor.core.dto.google.GrupCorreuDto;
import lombok.Data;

public @Data class UsuariGrupCorreuDto {
    private UsuariDto usuari;
    private GrupCorreuDto grupCorreu;
    private boolean bloquejat;
}
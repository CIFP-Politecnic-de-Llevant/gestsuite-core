package cat.politecnicllevant.core.dto.gestib;

import cat.politecnicllevant.core.dto.google.GrupCorreuDto;
import lombok.Data;

public @Data class UsuariGrupCorreuDto {
    private UsuariDto usuari;
    private GrupCorreuDto grupCorreu;
    private boolean bloquejat;
}
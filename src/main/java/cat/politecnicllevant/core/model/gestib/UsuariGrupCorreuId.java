package cat.politecnicllevant.core.model.gestib;

import lombok.Data;

import java.io.Serializable;

public @Data class UsuariGrupCorreuId implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long usuari;
    private Long grupCorreu;
}

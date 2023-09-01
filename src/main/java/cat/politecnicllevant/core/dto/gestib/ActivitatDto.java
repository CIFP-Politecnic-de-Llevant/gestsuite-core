package cat.politecnicllevant.core.dto.gestib;

import lombok.Data;

public @Data class ActivitatDto {
    private Long idactivitat;
    private String gestibIdentificador;
    private String gestibNom;
    private String gestibNomCurt;
}

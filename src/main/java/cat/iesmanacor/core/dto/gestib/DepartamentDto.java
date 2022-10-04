package cat.iesmanacor.core.dto.gestib;

import lombok.Data;

public @Data class DepartamentDto {
    private Long iddepartament;
    private String gestibIdentificador;
    private String gestibNom;
    private UsuariDto capDepartament;
}

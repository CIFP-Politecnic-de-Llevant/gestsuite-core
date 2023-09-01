package cat.politecnicllevant.core.dto.gestib;

import lombok.Data;

public @Data class SessioDto {
    private Long idsessio;
    private String gestibProfessor;
    private String gestibAlumne;
    private String gestibCurs;
    private String gestibGrup;
    private String gestibDia;
    private String gestibHora;
    private String gestibDurada;
    private String gestibAula;
    private String gestibSubmateria;
    private String gestibActivitat;
    private String gestibPlaca;
}

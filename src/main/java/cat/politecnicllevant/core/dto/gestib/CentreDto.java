package cat.politecnicllevant.core.dto.gestib;

import lombok.Data;

import java.time.LocalDateTime;

public @Data class CentreDto {
    private Long idcentre;
    private String identificador;
    private String anyAcademic;
    private String nom;
    private Boolean sincronitzar;
    private Boolean sincronitzaProfessors;
    private Boolean sincronitzaAlumnes;
    private LocalDateTime dataSincronitzacio;
}

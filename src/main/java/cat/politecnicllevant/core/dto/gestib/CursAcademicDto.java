package cat.politecnicllevant.core.dto.gestib;

import lombok.Data;

import javax.persistence.*;

public @Data class CursAcademicDto {
    private Long idcursAcademic;
    private String nom;
    private Boolean actual;
}

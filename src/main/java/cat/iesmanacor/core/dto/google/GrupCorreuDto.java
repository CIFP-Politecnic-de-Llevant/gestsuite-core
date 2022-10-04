package cat.iesmanacor.core.dto.google;

import cat.iesmanacor.core.dto.gestib.DepartamentDto;
import cat.iesmanacor.core.dto.gestib.GrupDto;
import cat.iesmanacor.core.dto.gestib.UsuariDto;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashSet;
import java.util.Set;

@JsonIdentityInfo(generator= ObjectIdGenerators.PropertyGenerator.class, property="idgrup")
@EqualsAndHashCode(exclude={"usuaris","grupCorreus","grups","departaments"})
public @Data class GrupCorreuDto {
    private Long idgrup;
    private GrupCorreuTipusDto grupCorreuTipus;
    private String gsuiteNom;
    private String gsuiteEmail;
    private String gsuiteDescripcio;
    private Set<UsuariDto> usuaris = new HashSet<>();
    private Set<GrupCorreuDto> grupCorreus = new HashSet<>();
    private Set<GrupDto> grups = new HashSet<>();
    private Set<DepartamentDto> departaments = new HashSet<>();
}

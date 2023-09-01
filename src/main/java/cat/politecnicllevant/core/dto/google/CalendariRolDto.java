package cat.politecnicllevant.core.dto.google;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum CalendariRolDto {
    SENSE_ACCES ("none"),LECTOR_DISPONIBLE_OCUPAT("freeBusyReader" ),LECTOR("reader"),LECTOR_ESCRIPTOR("writer"),PROPIETARI("owner");

    private final String rol;
}

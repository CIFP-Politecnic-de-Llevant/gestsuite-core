package cat.iesmanacor.core.dto.google;

import cat.iesmanacor.core.dto.gestib.UsuariDto;
import lombok.Data;

//No ho desem a la BBDD de moment
public @Data class DispositiuDto {
    private String idDispositiu;
    private String serialNumber;
    private String model;
    private String estat;
    private String macAddress;
    private String orgUnitPath;
    private UsuariDto usuari;
}

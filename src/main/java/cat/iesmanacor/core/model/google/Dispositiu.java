package cat.iesmanacor.core.model.google;

import cat.iesmanacor.core.model.gestib.Usuari;
import lombok.Data;

//No ho desem a la BBDD de moment
public @Data class Dispositiu {

    private String idDispositiu;

    private String serialNumber;

    private String model;

    private String estat;

    private String macAddress;

    private String orgUnitPath;

    private Usuari usuari;
}

package cat.iesmanacor.core.model.google;

import cat.iesmanacor.core.model.gestib.Usuari;
import lombok.Data;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;


@Entity
@Table(name = "im_calendari")
public @Data class Calendari {
    @Id
    @Column(name = "idcalendari")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idcalendari;

    @Column(name = "tipus", nullable = false)
    @Enumerated(EnumType.STRING)
    private CalendariTipus calendariTipus;

    /* GSUITE */
    @Column(name = "gsuite_email", nullable = true, length = 1024)
    private String gsuiteEmail;

    @Column(name = "gsuite_nom", nullable = true, length = 1024)
    private String gsuiteNom;

    @Column(name = "gsuite_descripcio", nullable = true, length = 1024)
    private String gsuiteDescripcio;

    @ManyToMany
    private Set<Usuari> usuarisLectura = new HashSet<>();

    @ManyToMany
    private Set<Usuari> usuarisEdicio = new HashSet<>();

    @ManyToMany
    private Set<GrupCorreu> grupCorreuLectura = new HashSet<>();

    @ManyToMany
    private Set<GrupCorreu> grupCorreuEdicio = new HashSet<>();
}

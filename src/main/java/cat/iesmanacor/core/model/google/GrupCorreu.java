package cat.iesmanacor.core.model.google;

import cat.iesmanacor.core.model.gestib.Departament;
import cat.iesmanacor.core.model.gestib.Grup;
import cat.iesmanacor.core.model.gestib.Usuari;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;


@Entity
@Table(name = "im_grup_correu")
@EqualsAndHashCode(exclude={"usuaris","grupCorreus","grups"})
public @Data class GrupCorreu {
    @Id
    @Column(name = "idgrup")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idgrup;

    @Column(name = "tipus", nullable = false)
    @Enumerated(EnumType.STRING)
    private GrupCorreuTipus grupCorreuTipus;

    /* GESTIB */
    @Column(name = "gestib_grup", nullable = true, length = 255)
    private String gestibGrup;

    /* GSUITE */
    @Column(name = "gsuite_nom", nullable = true, length = 255)
    private String gsuiteNom;

    @Column(name = "gsuite_email", nullable = true, length = 255)
    private String gsuiteEmail;

    @Column(name = "gsuite_descripcio", nullable = true)
    @Type(type = "text")
    private String gsuiteDescripcio;


    /*@OneToMany(mappedBy="grupCorreu")
    @JsonManagedReference
    private Set<Usuari> usuaris = new HashSet<>();*/

    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Usuari> usuaris = new HashSet<>();

    //Un grup de correu pot tenir com a membres altres grups de correu
    @ManyToMany(fetch = FetchType.EAGER)
    private Set<GrupCorreu> grupCorreus = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Grup> grups = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Departament> departaments = new HashSet<>();
}

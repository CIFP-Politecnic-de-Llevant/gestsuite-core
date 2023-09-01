package cat.politecnicllevant.core.model.google;

import cat.politecnicllevant.core.model.gestib.Usuari;
import lombok.Data;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pll_llistat")
public @Data class LlistatGoogle {
    @Id
    @Column(name = "idllistat")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idllistat;

    @Column(name = "nom", nullable = false, length = 255)
    private String nom;

    @Column(name = "identificador", nullable = false, length = 1024)
    private String identificador;

    @Column(name = "url", nullable = false)
    @Type(type = "text")
    private String url;

    @Column(name = "data_creacio", nullable = false)
    private LocalDateTime dataCreacio;

    @Column(name = "tipus", nullable = false)
    @Enumerated(EnumType.STRING)
    private LlistatGoogleTipus llistatGoogleTipus;


    @ManyToOne
    @JoinColumn(
            name = "user_iduser",
            nullable = false)
    //@JsonBackReference
    private Usuari propietari;

}

package cat.politecnicllevant.core.model.gestib;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pll_centre")
//@EqualsAndHashCode(exclude="programacio")
public @Data class Centre {
    @Id
    @Column(name = "idcentre")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idcentre;

    @Column(name = "identificador", unique = true, nullable = false)
    private String identificador;

    @Column(name = "any_academic", nullable = false, length = 32)
    private String anyAcademic;

    @Column(name = "nom", nullable = false, length = 2048)
    private String nom;

    @Column(name = "sincronitzar", nullable = false)
    private Boolean sincronitzar;

    @Column(name = "sincronitzant", nullable = false)
    private Boolean sincronitzant;

    @Column(name = "sincronitza_professors", nullable = false)
    private Boolean sincronitzaProfessors;

    @Column(name = "sincronitza_alumnes", nullable = false)
    private Boolean sincronitzaAlumnes;

    @Column(name = "data_sincronitzacio", nullable = true)
    private LocalDateTime dataSincronitzacio;


    /* TODO: falta posar un altre flag per saber quan s'esta sincronitzant i no deixar entrar a la gent */

    //@OneToMany(mappedBy="centre")
    //@JsonManagedReference
    //private Set<Modul> moduls = new HashSet<>();

}

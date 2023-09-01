package cat.politecnicllevant.core.model.gestib;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "pll_grup")
public @Data class Grup {
    @Id
    @Column(name = "idgrup")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idgrup;

    /* GESTIB */
    @Column(name = "gestib_identificador", unique = true, nullable = false, length = 2048)
    private String gestibIdentificador;

    @Column(name = "gestib_nom", nullable = false, length = 2048)
    private String gestibNom;

    @Column(name = "gestib_curs", nullable = false, length = 2048)
    private String gestibCurs;

    @Column(name = "gestib_tutor1", nullable = false, length = 2048)
    private String gestibTutor1;

    @Column(name = "gestib_tutor2", nullable = false, length = 2048)
    private String gestibTutor2;

    @Column(name = "gestib_tutor3", nullable = false, length = 2048)
    private String gestibTutor3;

    @Column(name="actiu", nullable = false)
    private Boolean actiu;

}

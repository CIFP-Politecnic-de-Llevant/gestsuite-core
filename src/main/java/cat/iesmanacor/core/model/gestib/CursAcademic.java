package cat.iesmanacor.core.model.gestib;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "pll_curs_academic")
public @Data class CursAcademic {
    @Id
    @Column(name = "idcurs_academic")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idcursAcademic;

    @Column(name = "nom", unique = true, nullable = false, length = 20)
    private String nom;

    @Column(name = "actual", nullable = false)
    private Boolean actual;
}

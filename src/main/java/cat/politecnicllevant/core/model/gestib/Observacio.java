package cat.politecnicllevant.core.model.gestib;

import lombok.Data;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pll_observacio")
//@EqualsAndHashCode(exclude="programacio")
public @Data class Observacio {
    @Id
    @Column(name = "idobservacio")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idobservacio;

    @Column(name = "data_creacio", nullable = false)
    private LocalDateTime dataCreacio;

    @Column(name = "descripcio", nullable = false)
    @Type(type = "text")
    private String descripcio;

    @Column(name = "tipus", nullable = false)
    @Enumerated(EnumType.STRING)
    private ObservacioTipus tipus;

    @ManyToOne
    @JoinColumn(
            name = "user_iduser",
            nullable = false)
    private Usuari usuari;

}

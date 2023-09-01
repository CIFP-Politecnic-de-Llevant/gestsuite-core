package cat.politecnicllevant.core.model.google;

import lombok.Data;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pll_fitxer_bucket")
public @Data class FitxerBucket {
    @Id
    @Column(name = "idfitxer")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idfitxer;

    @Column(name = "nom", nullable = false, length = 2048)
    private String nom;

    @Column(name = "path", nullable = false)
    @Type(type = "text")
    private String path;

    @Column(name = "bucket", nullable = false, length = 2048)
    private String bucket;

    @Column(name = "data_creacio", nullable = false)
    private LocalDateTime dataCreacio;
}

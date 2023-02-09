package cat.iesmanacor.core.model.gestib;

import cat.iesmanacor.core.model.google.GrupCorreu;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name="im_grup_correu_usuaris")
@IdClass(UsuariGrupCorreuId.class)
public @Data class UsuariGrupCorreu {
    @Id
    @ManyToOne
    @JoinColumn(name = "usuaris_idusuari", insertable = false, updatable = false)
    @JsonManagedReference
    private Usuari usuari;

    @Id
    @ManyToOne
    @JoinColumn(name = "grup_correu_idgrup", insertable = false, updatable = false)
    @JsonBackReference
    private GrupCorreu grupCorreu;


    @Column(name = "bloquejat", nullable = false)
    private Boolean bloquejat;
}

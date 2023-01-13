package cat.iesmanacor.core.repository.gestib;

import cat.iesmanacor.core.model.gestib.Centre;
import cat.iesmanacor.core.model.gestib.Observacio;
import cat.iesmanacor.core.model.gestib.Usuari;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ObservacioRepository extends JpaRepository<Observacio, Long> {
    List<Observacio> findAllByUsuari(Usuari usuari);
}

package cat.iesmanacor.core.repository.google;

import cat.iesmanacor.core.model.gestib.Usuari;
import cat.iesmanacor.core.model.gestib.UsuariGrupCorreu;
import cat.iesmanacor.core.model.gestib.UsuariGrupCorreuId;
import cat.iesmanacor.core.model.google.GrupCorreu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UsuariGrupCorreuRepository extends JpaRepository<UsuariGrupCorreu, UsuariGrupCorreuId> {
    List<UsuariGrupCorreu> findAllByUsuari(Usuari usuari);
    List<UsuariGrupCorreu> findAllByGrupCorreu(GrupCorreu grupCorreu);
    UsuariGrupCorreu findByUsuari_IdusuariAndGrupCorreu_Idgrup(Long usuari, Long grupCorreu);
}

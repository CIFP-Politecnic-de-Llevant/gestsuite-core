package cat.iesmanacor.core.repository.google;

import cat.iesmanacor.core.model.gestib.Usuari;
import cat.iesmanacor.core.model.google.GrupCorreu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GrupCorreuRepository extends JpaRepository<GrupCorreu, Long> {
    GrupCorreu findGrupCorreuByGsuiteEmail(String email);

    //List<GrupCorreu> findAllByUsuarisContains(Usuari usuari);
}

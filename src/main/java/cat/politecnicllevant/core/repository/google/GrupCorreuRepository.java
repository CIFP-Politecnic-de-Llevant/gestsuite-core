package cat.politecnicllevant.core.repository.google;

import cat.politecnicllevant.core.model.google.GrupCorreu;
import cat.politecnicllevant.core.model.google.GrupCorreuTipus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GrupCorreuRepository extends JpaRepository<GrupCorreu, Long> {
    GrupCorreu findGrupCorreuByGsuiteEmail(String email);
    List<GrupCorreu> findAllByGrupCorreuTipus(GrupCorreuTipus grupCorreuTipus);

    //List<GrupCorreu> findAllByUsuarisContains(Usuari usuari);
}

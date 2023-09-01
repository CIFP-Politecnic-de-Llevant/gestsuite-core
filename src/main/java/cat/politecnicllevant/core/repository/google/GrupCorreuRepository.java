package cat.politecnicllevant.core.repository.google;

import cat.politecnicllevant.core.model.google.GrupCorreu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GrupCorreuRepository extends JpaRepository<GrupCorreu, Long> {
    GrupCorreu findGrupCorreuByGsuiteEmail(String email);

    //List<GrupCorreu> findAllByUsuarisContains(Usuari usuari);
}

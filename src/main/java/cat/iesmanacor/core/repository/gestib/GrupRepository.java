package cat.iesmanacor.core.repository.gestib;

import cat.iesmanacor.core.model.gestib.Grup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GrupRepository extends JpaRepository<Grup, Long> {
    Grup findGrupByGestibIdentificador(String identificador);

    List<Grup> findAllByGestibNomAndGestibCurs(String nom,String curs);

    List<Grup> findAllByGestibCurs(String curs);
    List<Grup> findAllByGestibTutor1OrGestibTutor2OrGestibTutor3(String tutor1, String tutor2, String tutor3);

}

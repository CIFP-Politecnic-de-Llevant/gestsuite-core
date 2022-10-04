package cat.iesmanacor.core.repository.gestib;

import cat.iesmanacor.core.model.gestib.Curs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CursRepository extends JpaRepository<Curs, Long> {
    Curs findCursByGestibIdentificador(String identificador);

    List<Curs> findAllByGestibNom(String nom);
}

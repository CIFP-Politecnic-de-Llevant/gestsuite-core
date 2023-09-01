package cat.politecnicllevant.core.repository.gestib;

import cat.politecnicllevant.core.model.gestib.Sessio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessioRepository extends JpaRepository<Sessio, Long> {
    List<Sessio> findAllByGestibProfessor(String professor);

    List<Sessio> findAllByGestibAlumne(String alumne);

    List<Sessio> findAllByGestibGrup(String grup);
}

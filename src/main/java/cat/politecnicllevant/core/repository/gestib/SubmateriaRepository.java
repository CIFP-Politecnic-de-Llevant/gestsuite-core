package cat.politecnicllevant.core.repository.gestib;

import cat.politecnicllevant.core.model.gestib.Submateria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubmateriaRepository extends JpaRepository<Submateria, Long> {
    Submateria findSubmateriaByGestibIdentificador(String identificador);
}

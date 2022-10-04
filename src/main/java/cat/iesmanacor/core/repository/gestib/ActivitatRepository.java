package cat.iesmanacor.core.repository.gestib;

import cat.iesmanacor.core.model.gestib.Activitat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivitatRepository extends JpaRepository<Activitat, Long> {
    Activitat findActivitatByGestibIdentificador(String identificador);
}

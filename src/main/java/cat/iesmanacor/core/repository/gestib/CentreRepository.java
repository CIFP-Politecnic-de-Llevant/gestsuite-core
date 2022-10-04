package cat.iesmanacor.core.repository.gestib;

import cat.iesmanacor.core.model.gestib.Centre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CentreRepository extends JpaRepository<Centre, Long> {
    Centre findCentreByIdentificador(String identificador);
}

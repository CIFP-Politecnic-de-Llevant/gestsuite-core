package cat.politecnicllevant.core.repository.gestib;

import cat.politecnicllevant.core.model.gestib.Aula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AulaRepository extends JpaRepository<Aula, Long> {

}

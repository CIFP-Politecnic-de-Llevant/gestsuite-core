package cat.politecnicllevant.core.repository.gestib;

import cat.politecnicllevant.core.model.gestib.Departament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartamentRepository extends JpaRepository<Departament, Long> {
    Departament findDepartamentByGestibIdentificador(String identificador);
}

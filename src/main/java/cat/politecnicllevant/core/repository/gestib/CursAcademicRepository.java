package cat.politecnicllevant.core.repository.gestib;

import cat.politecnicllevant.core.model.gestib.CursAcademic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CursAcademicRepository extends JpaRepository<CursAcademic, Long> {
    Optional<CursAcademic> findByNom(String nom);
}

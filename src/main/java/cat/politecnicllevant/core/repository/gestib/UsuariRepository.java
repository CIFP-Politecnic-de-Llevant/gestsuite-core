package cat.politecnicllevant.core.repository.gestib;

import cat.politecnicllevant.core.model.gestib.Usuari;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UsuariRepository extends JpaRepository<Usuari, Long> {
    Usuari findUsuariByGestibCodi(String codi);

    Usuari findUsuariByGsuiteEmail(String email);

    Usuari findUsuariByGestibExpedientAndActiuIsTrueAndGestibAlumneIsTrue(String expedient);

    Usuari findUsuariByGestibCodiOrGsuiteEmail(String codi, String email);

    Usuari findUsuariByGsuitePersonalID(String codi);

    List<Usuari> findAllByGestibProfessorTrue();

    List<Usuari> findAllByGestibAlumneTrue();

    List<Usuari> findAllByGsuiteEmailIsNull();

    List<Usuari> findAllByGsuiteSuspesTrue();

    List<Usuari> findAllByGsuiteEliminatTrue();

    List<Usuari> findAllByActiu(Boolean actiu);

    List<Usuari> findAllByGestibGrupOrGestibGrup2OrGestibGrup3(String grupGestib, String grupGestib2, String grupGestib3);

    List<Usuari> findAllByGestibDepartament(String gestibDepartament);
}

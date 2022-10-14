package cat.iesmanacor.core.repository.google;

import cat.iesmanacor.core.model.google.Calendari;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CalendariRepository extends JpaRepository<Calendari, Long> {
    Calendari findCalendariByGsuiteEmail(String email);
}

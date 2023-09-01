package cat.politecnicllevant.core.repository.google;

import cat.politecnicllevant.core.model.google.FitxerBucket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FitxerBucketRepository extends JpaRepository<FitxerBucket, Long> {
}

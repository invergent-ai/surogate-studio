package net.statemesh.repository;

import net.statemesh.domain.AppTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppTemplateRepository extends JpaRepository<AppTemplate, String> {

    @Query("SELECT DISTINCT a.category FROM AppTemplate a WHERE a.category IS NOT NULL AND a.category != ''")
    List<String> findAllDistinctCategories();

    @Query("SELECT a FROM AppTemplate a WHERE " +
        "(:search IS NULL OR :search = '' OR " +
        "LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
        "LOWER(a.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
        "LOWER(a.longDescription) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
        "LOWER(a.hashtags) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
        "LOWER(a.category) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
        "(:category IS NULL OR :category = '' OR a.category = :category) AND " +
        "(:providerId IS NULL OR :providerId = '' OR a.provider.id = :providerId)")
    List<AppTemplate> findWithFilters(@Param("search") String search,
                                      @Param("category") String category,
                                      @Param("providerId") String providerId);
}


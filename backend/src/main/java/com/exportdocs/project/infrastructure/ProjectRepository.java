package com.exportdocs.project.infrastructure;

import com.exportdocs.project.domain.Project;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Project p where p.id = :id")
    java.util.Optional<Project> findForUpdate(@Param("id") UUID id);

    @Query(value = """
            SELECT p.* FROM project p
            WHERE (:admin OR EXISTS (SELECT 1 FROM project_access a WHERE a.project_id=p.id AND a.user_id=:userId))
              AND (CAST(:status AS varchar) IS NULL OR p.status=CAST(:status AS varchar))
              AND (CAST(:prefix AS varchar) IS NULL OR p.reference LIKE CAST(:prefix AS varchar) ESCAPE '!')
            ORDER BY p.created_at DESC, p.id DESC
            """, countQuery = """
            SELECT count(*) FROM project p
            WHERE (:admin OR EXISTS (SELECT 1 FROM project_access a WHERE a.project_id=p.id AND a.user_id=:userId))
              AND (CAST(:status AS varchar) IS NULL OR p.status=CAST(:status AS varchar))
              AND (CAST(:prefix AS varchar) IS NULL OR p.reference LIKE CAST(:prefix AS varchar) ESCAPE '!')
            """, nativeQuery = true)
    Page<Project> findVisible(@Param("userId") UUID userId, @Param("admin") boolean admin,
            @Param("status") String status, @Param("prefix") String prefix, Pageable pageable);
}

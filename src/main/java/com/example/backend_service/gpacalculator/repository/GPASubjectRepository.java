package com.example.backend_service.gpacalculator.repository;

import com.example.backend_service.gpacalculator.model.GPASubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Repository
public interface GPASubjectRepository extends JpaRepository<GPASubject, String> {
    List<GPASubject> findBySemesterId(String semesterId);

    List<GPASubject> findByStudentId(Long studentId);

    List<GPASubject> findByStudentIdAndIsGpaTrue(Long studentId);

    /**
     * Deletes a subject via a direct bulk JPQL DELETE rather than the inherited
     * {@code deleteById} (which loads-then-removes the entity through the persistence context).
     *
     * <p>{@code GPASemester.subjects} is {@code fetch = EAGER}, so simply looking up a
     * {@code GPASubject} (e.g. the ownership check in {@code GPACalculatorController
     * #deleteSubject}) also eagerly loads its parent {@code GPASemester} and that semester's
     * <em>entire</em> subjects collection into the same persistence context - with {@code
     * spring.jpa.open-in-view=true}, that context spans the whole request. The parent's
     * {@code cascade = CascadeType.ALL} then still sees this subject sitting in that
     * already-loaded collection at flush time, and reconciling the (untouched) collection wins
     * over the entity-level {@code remove()} - so {@code deleteById} returns normally and the
     * row silently survives, only for the same id to be found, "deleted" again, or even updated
     * on a later request. A bulk delete bypasses that entity-graph reconciliation entirely: it
     * issues {@code DELETE FROM gpa_subjects WHERE id = ?} straight to the database, and {@code
     * clearAutomatically = true} evicts the persistence context afterward so the now-stale
     * eagerly-loaded parent collection can't cause the same problem later in the request.
     *
     * <p>{@code @Transactional} is required here explicitly - unlike inherited methods such as
     * {@code deleteById} (declared transactional on {@code SimpleJpaRepository} itself), a custom
     * {@code @Modifying} query method on the repository interface does not get a writable
     * transaction for free, and fails with "No EntityManager with actual transaction available"
     * on the {@code flushAutomatically} flush without one.
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM GPASubject s WHERE s.id = :id")
    void deleteByIdImmediate(@Param("id") String id);
}

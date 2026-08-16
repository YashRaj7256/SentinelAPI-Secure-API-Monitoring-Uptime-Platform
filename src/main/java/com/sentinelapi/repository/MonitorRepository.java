package com.sentinelapi.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sentinelapi.entity.Monitor;

/**
 * Spring Data JPA repository for {@link Monitor} entities.
 *
 * <h3>How Spring Data Repositories Work</h3>
 * <p>This is an <strong>interface</strong> — there is no implementation class.
 * Spring Data automatically generates the implementation at runtime using
 * a dynamic proxy. You get full CRUD operations for free:
 * <ul>
 *   <li>{@code save(Monitor)} — INSERT or UPDATE</li>
 *   <li>{@code findById(Long)} — SELECT by primary key</li>
 *   <li>{@code findAll()} — SELECT all rows</li>
 *   <li>{@code deleteById(Long)} — DELETE by primary key</li>
 *   <li>{@code count()} — COUNT all rows</li>
 *   <li>...and many more from {@link JpaRepository}</li>
 * </ul>
 *
 * <h3>Custom Query Methods</h3>
 * <p>Spring Data parses method names to generate queries:
 * <ul>
 *   <li>{@code findByActiveTrue()} → {@code WHERE active = true}</li>
 *   <li>{@code findByStatus(MonitorStatus)} → {@code WHERE status = ?}</li>
 * </ul>
 *
 * <p>More complex queries that can't be derived from method names
 * can use {@code @Query("SELECT m FROM Monitor m WHERE ...")} annotation.
 * We'll add those when the scheduler needs them (Day 12).
 *
 * <h3>Inheritance Chain</h3>
 * <p>{@code JpaRepository} extends {@code PagingAndSortingRepository}
 * (adds pagination and sorting) which extends {@code CrudRepository}
 * (basic CRUD). By extending {@code JpaRepository}, we get all three levels.
 *
 * <h3>Generic Parameters</h3>
 * <ul>
 *   <li>First type: {@code Monitor} — the entity class this repository manages</li>
 *   <li>Second type: {@code Long} — the type of the entity's {@code @Id} field</li>
 * </ul>
 *
 * @see org.springframework.data.jpa.repository.JpaRepository
 */
@Repository
public interface MonitorRepository extends JpaRepository<Monitor, Long> {

    /**
     * Find all monitors that are currently active.
     *
     * <p>Spring Data derives this query from the method name:
     * <pre>SELECT m FROM Monitor m WHERE m.active = true</pre>
     *
     * <p>Used by the scheduler to determine which monitors need checking.
     * More sophisticated scheduling queries will be added in Day 12.
     *
     * @return list of active monitors
     */
    List<Monitor> findByActiveTrue();
}

package com.skndan.veda.repo;

import com.skndan.veda.entity.Paged;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;

import java.util.List;
import java.util.Map;

/**
 * Base repository providing common pagination and querying methods for database entities.
 * Extends Panache's repository functionality with generic type support.
 *
 * @param <T> the type of the entity
 * @param <ID> the type of the entity's primary key
 */
public abstract class BaseRepo<T, ID> implements PanacheRepositoryBase<T, ID> {

    /**
     * Retrieves a paginated list of all entities.
     *
     * @param page the page number to retrieve
     * @param size the number of items per page
     * @return a Paged object containing the results, total count, and pagination details
     */
    public Paged<T> findAllPaged(int page, int size) {
        PanacheQuery<T> query = findAll();
        long total = query.count();
        List<T> results = query.page(Page.of(page, size)).list();
        return new Paged<>(results, total, page, size);
    }

    // Positional parameters
    /**
     * Retrieves a paginated list of entities using positional parameters.
     *
     * @param query the JPQL query string
     * @param page the page number to retrieve
     * @param size the number of items per page
     * @param params positional query parameters
     * @return a Paged object containing the results, total count, and pagination details
     */
    public Paged<T> findPaged(String query, int page, int size, Object... params) {
        PanacheQuery<T> q = find(query, params);
        long total = q.count();
        List<T> results = q.page(Page.of(page, size)).list();
        return new Paged<>(results, total, page, size);
    }

    // Named parameters
    /**
     * Retrieves a paginated list of entities using named parameters.
     *
     * @param query the JPQL query string
     * @param params a map of named query parameters
     * @param page the page number to retrieve
     * @param size the number of items per page
     * @return a Paged object containing the results, total count, and pagination details
     */
    public Paged<T> findPaged(String query, Map<String, Object> params, int page, int size) {
        PanacheQuery<T> q = find(query, params);
        long total = q.count();
        List<T> results = q.page(Page.of(page, size)).list();
        return new Paged<>(results, total, page, size);
    }
}
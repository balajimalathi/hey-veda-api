package com.skndan.veda.repo;

import com.skndan.veda.entity.Paged;
import com.skndan.veda.entity.Groups;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class GroupRepo extends BaseRepo<Groups, Long> implements PanacheRepository<Groups> {

  /**
   * Find workspaces owned by a specific user
   * 
   * @param ownerId UUID of the workspace owner
   * @param page    Page number for pagination
   * @param size    Number of items per page
   * @return Paged list of workspaces
   */
  public Paged<Groups> findByOwnerId(UUID ownerId, int page, int size) {
    PanacheQuery<Groups> query = find("ownerId", ownerId);
    long total = query.count();
    List<Groups> results = query.page(Page.of(page, size)).list();
    return new Paged<>(results, total, page, size);
  }
}
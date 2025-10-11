package com.skndan.veda.repo;

import com.skndan.veda.entity.FileInfo;
import com.skndan.veda.entity.Paged;
import com.skndan.veda.entity.Workspace;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class FileInfoRepo extends BaseRepo<FileInfo, Long> implements PanacheRepository<FileInfo> {

    /**
     * Find files by workspace
     * 
     * @param workspace Workspace entity
     * @param page      Page number for pagination
     * @param size      Number of items per page
     * @return Paged list of file info
     */
    public Paged<FileInfo> findByWorkspace(Workspace workspace, int page, int size) {
        PanacheQuery<FileInfo> query = find("workspace", workspace);
        long total = query.count();
        List<FileInfo> results = query.page(Page.of(page, size)).list();
        return new Paged<>(results, total, page, size);
    }

    /**
     * Find files by uploader
     * 
     * @param uploaderId UUID of the uploader
     * @param page       Page number for pagination
     * @param size       Number of items per page
     * @return Paged list of file info
     */
    public Paged<FileInfo> findByUploaderId(UUID uploaderId, int page, int size) {
        PanacheQuery<FileInfo> query = find("uploaderId", uploaderId);
        long total = query.count();
        List<FileInfo> results = query.page(Page.of(page, size)).list();
        return new Paged<>(results, total, page, size);
    }
}
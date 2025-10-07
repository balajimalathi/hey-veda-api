package com.skndan.veda.repo;

import com.skndan.veda.entity.Paged;
import io.quarkus.panache.common.Page;
import com.skndan.veda.entity.Profile;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ProfileRepo extends BaseRepo<Profile, Long> implements PanacheRepository<Profile> {

    public Paged<Profile> findAllPaged(int page, int size) {

        PanacheQuery<Profile> tickets = Profile.find("active", true);
        long total = tickets.count();

        List<Profile> ticketPaged = tickets.page(Page.of(page, size)).list();

        return new Paged<>(ticketPaged, total, page, size);
    }

    public Profile findByUid(String uid) {
        return find("uid", uid).firstResult();
    }
}
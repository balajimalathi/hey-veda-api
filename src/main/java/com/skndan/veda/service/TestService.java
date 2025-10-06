package com.skndan.veda.service;

import com.skndan.veda.repo.TestRepo;
import com.skndan.veda.entity.Paged;
import com.skndan.veda.entity.Ticket;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Service for managing test tickets and related operations.
 * Provides methods for creating, retrieving, and deleting test tickets.
 */
@ApplicationScoped
public class TestService {

    @Inject
    TestRepo repo;

    /**
     * Creates a new ticket in the system.
     * Persists the ticket and returns the created ticket.
     *
     * @param ticket the ticket to be created
     * @return the newly created ticket
     */
    @Transactional
    public Ticket createTicket(Ticket ticket) {
        repo.persist(ticket);
        return ticket;
    }

    /**
     * Deletes a ticket from the system by its unique identifier.
     *
     * @param id the unique identifier of the ticket to be deleted
     */
    public void deleteTicket(Long id) {
        Ticket ticket = repo.findById(id);
        ticket.delete();
    }

    /**
     * Retrieves a specific ticket by its unique identifier.
     *
     * @param id the unique identifier of the ticket
     * @return the ticket with the specified ID
     */
    public Ticket getTicket(Long id) {
        return repo.findById(id);
    }

    /**
     * Retrieves a paginated list of tickets.
     *
     * @param page the page number for pagination
     * @param size number of items per page
     * @return a paginated list of tickets
     */
    public Paged<Ticket> getTickets(int page, int size) {
        return repo.findAllPaged(page, size);
    }

    /**
     * Retrieves a paginated list of tickets filtered by an identifier.
     *
     * @param page the page number for pagination
     * @param size number of items per page
     * @param id the identifier to filter tickets
     * @return a paginated list of tickets matching the identifier
     */
    public Paged<Ticket> getTickets2(int page, int size, Long id) {
        return repo.findAllPagedById(page, size, id);
    }


}
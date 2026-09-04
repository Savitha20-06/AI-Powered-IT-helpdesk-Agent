package com.helpdesk.itagent.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.helpdesk.itagent.domain.AppUser;
import com.helpdesk.itagent.domain.Conversation;
import com.helpdesk.itagent.domain.IssueCategory;
import com.helpdesk.itagent.domain.Role;
import com.helpdesk.itagent.domain.Ticket;
import com.helpdesk.itagent.domain.TicketComment;
import com.helpdesk.itagent.domain.TicketPriority;
import com.helpdesk.itagent.domain.TicketSource;
import com.helpdesk.itagent.domain.TicketStatus;
import com.helpdesk.itagent.dto.TicketDtos;
import com.helpdesk.itagent.repo.AppUserRepository;
import com.helpdesk.itagent.repo.TicketCommentRepository;
import com.helpdesk.itagent.repo.TicketRepository;

@Service
@SuppressWarnings("null")
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketCommentRepository commentRepository;
    private final AppUserRepository userRepository;
    private final AtomicInteger sequence = new AtomicInteger((int) (System.currentTimeMillis() % 1000));

    public TicketService(TicketRepository ticketRepository,
                         TicketCommentRepository commentRepository,
                         AppUserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Ticket createManual(AppUser employee, TicketDtos.CreateTicketRequest request) {
        Ticket ticket = new Ticket();
        ticket.setTicketNumber(nextNumber());
        ticket.setEmployee(employee);
        ticket.setTitle(request.title());
        ticket.setSummary(request.summary());
        ticket.setSymptoms(request.symptoms());
        ticket.setTroubleshootingAttempted("Created manually by employee.");
        ticket.setCategory(request.category() == null ? IssueCategory.OTHER : request.category());
        ticket.setPriority(request.priority() == null ? TicketPriority.MEDIUM : request.priority());
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setSource(TicketSource.MANUAL);
        return ticketRepository.save(ticket);
    }

    @Transactional
    public Ticket createFromAi(AppUser employee,
                               Conversation conversation,
                               String title,
                               String summary,
                               String symptoms,
                               String troubleshooting,
                               String kbContext,
                               IssueCategory category,
                               TicketPriority priority) {
        Ticket ticket = new Ticket();
        ticket.setTicketNumber(nextNumber());
        ticket.setEmployee(employee);
        ticket.setConversation(conversation);
        ticket.setTitle(title);
        ticket.setSummary(summary);
        ticket.setSymptoms(symptoms);
        ticket.setTroubleshootingAttempted(troubleshooting);
        ticket.setKbContext(kbContext);
        ticket.setCategory(category);
        ticket.setPriority(priority);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setSource(TicketSource.AI_ESCALATION);
        return ticketRepository.save(ticket);
    }

    @Transactional(readOnly = true)
    public List<Ticket> listFor(AppUser user) {
        if (user.getRole() == Role.EMPLOYEE) {
            return ticketRepository.findByEmployeeOrderByCreatedAtDesc(user);
        }
        return ticketRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<TicketDtos.TicketSummary> listSummaries(AppUser user) {
        return listFor(user).stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public TicketDtos.TicketDetail getDetail(Long id, AppUser user) {
        return toDetail(getVisible(id, user));
    }

    public Ticket getVisible(Long id, AppUser user) {
        Ticket ticket = ticketRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        if (user.getRole() == Role.EMPLOYEE && !ticket.getEmployee().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only view your own tickets");
        }
        return ticket;
    }

    @Transactional
    public Ticket assign(Long ticketId, Long assigneeId, AppUser actor) {
        requireStaff(actor);
        Ticket ticket = getVisible(ticketId, actor);
        AppUser assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignee not found"));
        if (assignee.getRole() == Role.EMPLOYEE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tickets can only be assigned to IT Staff or Admin");
        }
        ticket.setAssignedTo(assignee);
        if (ticket.getStatus() == TicketStatus.OPEN) {
            ticket.setStatus(TicketStatus.ASSIGNED);
        }
        return ticketRepository.save(ticket);
    }

    @Transactional
    public Ticket updateStatus(Long ticketId, TicketStatus status, AppUser actor) {
        requireStaff(actor);
        Ticket ticket = getVisible(ticketId, actor);
        ticket.setStatus(status);
        if (status == TicketStatus.RESOLVED) {
            ticket.setResolvedAt(Instant.now());
        }
        if (status == TicketStatus.CLOSED) {
            ticket.setClosedAt(Instant.now());
            if (ticket.getResolvedAt() == null) {
                ticket.setResolvedAt(Instant.now());
            }
        }
        return ticketRepository.save(ticket);
    }

    @Transactional
    public Ticket update(Long ticketId, TicketDtos.UpdateTicketRequest request, AppUser actor) {
        requireStaff(actor);
        Ticket ticket = getVisible(ticketId, actor);
        if (request.priority() != null) ticket.setPriority(request.priority());
        if (request.category() != null) ticket.setCategory(request.category());
        if (request.title() != null && !request.title().isBlank()) ticket.setTitle(request.title());
        return ticketRepository.save(ticket);
    }

    @Transactional
    public TicketComment addComment(Long ticketId, String body, AppUser actor) {
        Ticket ticket = getVisible(ticketId, actor);
        if (body == null || body.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment cannot be empty");
        }
        TicketComment comment = new TicketComment();
        comment.setTicket(ticket);
        comment.setAuthor(actor);
        comment.setBody(body.trim());
        return commentRepository.save(comment);
    }

    @Transactional
    public void delete(Long ticketId, AppUser actor) {
        Ticket ticket = getVisible(ticketId, actor);
        if (actor.getRole() != Role.EMPLOYEE) {
            requireStaff(actor);
        }
        commentRepository.deleteByTicket(ticket);
        ticketRepository.delete(ticket);
    }

    public List<TicketComment> comments(Ticket ticket) {
        return commentRepository.findByTicketOrderByCreatedAtAsc(ticket);
    }

    public TicketDtos.TicketSummary toSummary(Ticket ticket) {
        return new TicketDtos.TicketSummary(
                ticket.getId(),
                ticket.getTicketNumber(),
                ticket.getTitle(),
                ticket.getCategory(),
                ticket.getPriority(),
                ticket.getStatus(),
                ticket.getSource(),
                toRef(ticket.getEmployee()),
                toRef(ticket.getAssignedTo()),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }

    public TicketDtos.TicketDetail toDetail(Ticket ticket) {
        return new TicketDtos.TicketDetail(
                ticket.getId(),
                ticket.getTicketNumber(),
                ticket.getTitle(),
                ticket.getSummary(),
                ticket.getSymptoms(),
                ticket.getTroubleshootingAttempted(),
                ticket.getKbContext(),
                ticket.getCategory(),
                ticket.getPriority(),
                ticket.getStatus(),
                ticket.getSource(),
                toRef(ticket.getEmployee()),
                toRef(ticket.getAssignedTo()),
                ticket.getConversation() == null ? null : ticket.getConversation().getId(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                ticket.getResolvedAt(),
                ticket.getClosedAt(),
                comments(ticket).stream().map(c -> new TicketDtos.CommentView(
                        c.getId(), toRef(c.getAuthor()), c.getBody(), c.getCreatedAt()
                )).toList()
        );
    }

    private TicketDtos.UserRef toRef(AppUser user) {
        if (user == null) return null;
        return new TicketDtos.UserRef(user.getId(), user.getFullName(), user.getEmail());
    }

    private void requireStaff(AppUser actor) {
        if (actor.getRole() == Role.EMPLOYEE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only IT Staff or Admin can perform this action");
        }
    }

    private String nextNumber() {
        String day = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        return "HD-" + day + "-" + String.format("%04d", sequence.incrementAndGet() % 10000);
    }
}

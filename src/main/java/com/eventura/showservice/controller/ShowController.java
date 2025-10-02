package com.eventura.showservice.controller;
import com.eventura.showservice.domain.Show;
import com.eventura.showservice.dto.*;
import com.eventura.showservice.repository.ShowRepository;
import com.eventura.showservice.service.ShowService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/shows")
public class ShowController {

    private final ShowService showService;
    private final ShowRepository showRepository; // ✅ final

    private static final Logger log = LoggerFactory.getLogger(ShowService.class);

    // ✅ Inject both ShowService and ShowRepository
    public ShowController(ShowService showService, ShowRepository showRepository) {
        this.showService = showService;
        this.showRepository = showRepository;
    }

    // Only admin can create new shows
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ShowResponse> createShow(@Valid @RequestBody CreateShowRequest req) {
        return ResponseEntity.ok(showService.createShow(req));
    }

    // Only admin can create multiple shows
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/batch")
    public ResponseEntity<List<ShowResponse>> createShowsBatch(@Valid @RequestBody List<CreateShowRequest> reqs) {
        return ResponseEntity.ok(showService.createShowsBatch(reqs));
    }

    // ✅ All users can list all shows
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping
    public ResponseEntity<List<ShowResponse>> listShows() {
        return ResponseEntity.ok(showService.listShows());
    }

    // ✅ All users can fetch a specific show
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<ShowResponse> getShow(@PathVariable UUID id) {
        return showService.getShow(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ✅ All users can fetch seats by hallId + showId
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/{hallId}/{showId}/seats")
    public ResponseEntity<Object> getSeats(@PathVariable UUID hallId, @PathVariable UUID showId) {
        return ResponseEntity.ok(showService.fetchSeatmap(hallId, showId));
    }

    // ✅ NEW: Get all shows for a specific movie with hall + theatre details
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/movie/{movieId}")
    public ResponseEntity<List<ShowResponseDTO>> getShowsByMovie(@PathVariable UUID movieId) {
        return ResponseEntity.ok(showService.getShowsByMovie(movieId));
    }

    // ✅ NEW: Get seatmap for specific movie, hall and showTime
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/seatmap")
    public ResponseEntity<SeatMapResponseDTO> getSeatMap(
            @RequestParam UUID movieId,
            @RequestParam UUID hallId,
            @RequestParam String showTime
    ) {
        try {
            return ResponseEntity.ok(showService.getSeatMap(movieId, hallId, showTime));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

//    @PostMapping("/confirm-seats")
//    @Transactional
//    public ResponseEntity<Void> confirmSeats(@RequestBody SeatUpdateRequest req) {
//        Show show = showRepository.findById(req.getShowId())
//                .orElseThrow(() -> new IllegalArgumentException("Show not found"));
//
//        List<String> available = new ArrayList<>(show.getAvailableSeats());
//        List<String> locked = new ArrayList<>(show.getLockedSeats());
//        List<String> booked = new ArrayList<>(show.getBookedSeats());
//
//        List<String> requested = req.getSeats();
//
//        if (available.containsAll(requested)) {
//            // Case 1: Confirm directly from available
//            available.removeAll(requested);
//            booked.addAll(requested);
//
//        } else if (locked.containsAll(requested)) {
//            // Case 2: Confirm from locked seats
//            locked.removeAll(requested);
//            booked.addAll(requested);
//
//        } else if (booked.containsAll(requested)) {
//            // Case 3: Already confirmed → no action
//            log.info("Seats {} already confirmed for show {}", requested, req.getShowId());
//
//        } else {
//            // Case 4: Invalid request
//            log.warn("⚠️ Seat conflict | requested={} | available={} | locked={} | booked={}",
//                    requested, available, locked, booked);
//            return ResponseEntity.status(HttpStatus.CONFLICT).build();
//        }
//
//        show.setAvailableSeats(available);
//        show.setLockedSeats(locked);
//        show.setBookedSeats(booked);
//
//        showRepository.save(show);
//        return ResponseEntity.ok().build();
//    }

    @PostMapping("/confirm-seats")
    @Transactional
    public ResponseEntity<Void> confirmSeats(@RequestBody SeatUpdateRequest req) {
        showService.confirmSeats(req.getShowId(), req.getSeats());
        return ResponseEntity.ok().build();
    }

//    @PostMapping("/release-seats")
//    @Transactional
//    public ResponseEntity<Void> releaseSeats(@RequestBody SeatUpdateRequest req) {
//        Show show = showRepository.findById(req.getShowId())
//                .orElseThrow(() -> new IllegalArgumentException("Show not found"));
//
//        List<String> booked = new ArrayList<>(show.getBookedSeats());
//        booked.removeAll(req.getSeats());
//        show.setBookedSeats(booked);
//
//        List<String> available = new ArrayList<>(show.getAvailableSeats());
//        available.addAll(req.getSeats());
//        show.setAvailableSeats(available);
//
//        showRepository.save(show);
//        return ResponseEntity.ok().build();
//    }

    @PostMapping("/release-seats")
    @Transactional
    public ResponseEntity<Void> releaseSeats(@RequestBody SeatUpdateRequest req) {
        showService.releaseSeats(req.getShowId(), req.getSeats());
        return ResponseEntity.ok().build();
    }


    // Get full seat layout for a show
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/{id}/seatmap")
    public ResponseEntity<Map<String, Object>> getSeatmap(@PathVariable UUID id) {
        Show show = showRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Show not found " + id));

        UUID hallId = show.getHallId();
        return ResponseEntity.ok(showService.fetchSeatmap(hallId, id));
    }

    // (optional) endpoints if frontend wants direct access
    @GetMapping("/{id}/booked-seats")
    public ResponseEntity<List<String>> getBookedSeats(@PathVariable UUID id) {
        Show show = showRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Show not found " + id));
        return ResponseEntity.ok(show.getBookedSeats());
    }

    @GetMapping("/{id}/locked-seats")
    public ResponseEntity<List<String>> getLockedSeats(@PathVariable UUID id) {
        Show show = showRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Show not found " + id));
        return ResponseEntity.ok(show.getLockedSeats() != null ? show.getLockedSeats() : List.of());
    }

    @PostMapping("/lock-seats")
    public ResponseEntity<Void> lockSeats(@RequestBody SeatUpdateRequest req) {
        return showService.lockSeats(req);
    }

}

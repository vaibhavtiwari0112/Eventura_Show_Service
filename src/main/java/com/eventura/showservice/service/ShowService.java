package com.eventura.showservice.service;

import com.eventura.showservice.domain.Show;
import com.eventura.showservice.dto.*;
import com.eventura.showservice.repository.ShowRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ShowService {
    private static final Logger log = LoggerFactory.getLogger(ShowService.class);

    private final ShowRepository showRepository;
    private final RestTemplate restTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final String catalogUrl;
    private final String bookingUrl;

    public ShowService(ShowRepository showRepository,
                       RestTemplate restTemplate,
                       RedisTemplate<String, Object> redisTemplate,
                       @Value("${catalog.url}") String catalogUrl,
                       @Value("${booking.url}") String bookingUrl) {
        this.showRepository = showRepository;
        this.restTemplate = restTemplate;
        this.redisTemplate = redisTemplate;
        this.catalogUrl = catalogUrl;
        this.bookingUrl = bookingUrl;
    }

    private String redisKey(UUID showId) {
        return "show:" + showId + ":seats";
    }

    // ---------------- CREATE SHOW ----------------
    @Transactional
    public ShowResponse createShow(CreateShowRequest req) {
        OffsetDateTime nowUtc = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime startUtc = req.getStartTime().withOffsetSameInstant(ZoneOffset.UTC);
        OffsetDateTime endUtc = req.getEndTime().withOffsetSameInstant(ZoneOffset.UTC);

        if (!startUtc.isAfter(nowUtc)) {
            throw new IllegalArgumentException("Show start time must be in the future");
        }

        boolean hallOverlap = showRepository.existsByHallIdAndStartTimeLessThanAndEndTimeGreaterThan(
                req.getHallId(), endUtc, startUtc
        );
        if (hallOverlap) {
            throw new IllegalArgumentException("Another show in the same hall overlaps with this time range");
        }

        Show show = new Show();
        show.setId(UUID.randomUUID());
        show.setMovieId(req.getMovieId());
        show.setHallId(req.getHallId());
        show.setStartTime(startUtc);
        show.setEndTime(endUtc);
        show.setBasePrice(req.getBasePrice());
        show.setCreatedAt(nowUtc);

        // Generate seat map
        List<String> allSeats = generateSeatMap();
        List<String> bookedSeats = req.getBookedSeats() != null ? new ArrayList<>(req.getBookedSeats()) : new ArrayList<>();
        List<String> lockedSeats = req.getLockedSeats() != null ? new ArrayList<>(req.getLockedSeats()) : new ArrayList<>();
        List<String> availableSeats = allSeats.stream()
                .filter(s -> !bookedSeats.contains(s) && !lockedSeats.contains(s))
                .toList();

        show.setAvailableSeats(availableSeats);
        show.setBookedSeats(bookedSeats);
        show.setLockedSeats(lockedSeats);

        Show saved = showRepository.save(show);

        // ✅ Initialize Redis
        String key = redisKey(saved.getId());
        Map<String, Object> redisMap = new HashMap<>();
        redisMap.put("available", new HashSet<>(availableSeats));
        redisMap.put("locked", new HashSet<>(lockedSeats));
        redisMap.put("booked", new HashSet<>(bookedSeats));
        redisTemplate.opsForHash().putAll(key, redisMap);

        log.info("✅ Show {} created and Redis seatmap initialized", saved.getId());
        return toResponse(saved);
    }

    // ---------------- CREATE SHOWS BATCH ----------------
    @Transactional
    public List<ShowResponse> createShowsBatch(List<CreateShowRequest> requests) {
        log.info("➡️ Entering createShowsBatch with {} requests",
                requests != null ? requests.size() : 0);

        List<Show> showsToSave = new ArrayList<>();
        OffsetDateTime nowUtc = OffsetDateTime.now(ZoneOffset.UTC);
        Map<UUID, List<Interval>> batchHallIntervals = new HashMap<>();

        int index = 0;
        for (CreateShowRequest req : requests) {
            log.info("🔹 Processing request {}: hallId={}, movieId={}, start={}, end={}",
                    index++, req.getHallId(), req.getMovieId(), req.getStartTime(), req.getEndTime());

            OffsetDateTime startUtc = req.getStartTime().withOffsetSameInstant(ZoneOffset.UTC);
            OffsetDateTime endUtc = req.getEndTime().withOffsetSameInstant(ZoneOffset.UTC);

            if (startUtc.isBefore(nowUtc)) {
                throw new IllegalArgumentException("Show start time must be greater than current time: " + startUtc);
            }

            boolean existsInDb = showRepository.existsByHallIdAndStartTimeLessThanAndEndTimeGreaterThan(
                    req.getHallId(), endUtc, startUtc);
            if (existsInDb) {
                throw new IllegalArgumentException("A show already exists in hall " + req.getHallId()
                        + " that overlaps " + startUtc + " - " + endUtc);
            }

            Interval newInterval = new Interval(startUtc, endUtc);
            List<Interval> intervalsForHall = batchHallIntervals.computeIfAbsent(req.getHallId(), k -> new ArrayList<>());
            for (Interval i : intervalsForHall) {
                if (i.overlaps(newInterval)) {
                    throw new IllegalArgumentException("Batch contains overlapping shows for hall " + req.getHallId()
                            + ": " + i + " vs " + newInterval);
                }
            }
            intervalsForHall.add(newInterval);

            Show show = new Show();
            show.setId(UUID.randomUUID());
            show.setMovieId(req.getMovieId());
            show.setHallId(req.getHallId());
            show.setStartTime(startUtc);
            show.setEndTime(endUtc);
            show.setBasePrice(req.getBasePrice());
            show.setCreatedAt(nowUtc);

            // Generate seat map
            List<String> allSeats = generateSeatMap();
            List<String> bookedSeats = req.getBookedSeats() != null ? new ArrayList<>(req.getBookedSeats()) : new ArrayList<>();
            List<String> lockedSeats = req.getLockedSeats() != null ? new ArrayList<>(req.getLockedSeats()) : new ArrayList<>();
            List<String> availableSeats = allSeats.stream()
                    .filter(s -> !bookedSeats.contains(s) && !lockedSeats.contains(s))
                    .toList();

            show.setAvailableSeats(availableSeats);
            show.setBookedSeats(bookedSeats);
            show.setLockedSeats(lockedSeats);
            showsToSave.add(show);
        }

        List<Show> savedShows = showRepository.saveAll(showsToSave);
        log.info("✅ Saved {} shows successfully", savedShows.size());

        // ✅ Initialize Redis for each
        for (Show saved : savedShows) {
            String key = redisKey(saved.getId());
            Map<String, Object> redisMap = new HashMap<>();
            redisMap.put("available", new HashSet<>(saved.getAvailableSeats()));
            redisMap.put("locked", new HashSet<>(saved.getLockedSeats()));
            redisMap.put("booked", new HashSet<>(saved.getBookedSeats()));
            redisTemplate.opsForHash().putAll(key, redisMap);
            log.info("✅ Redis seatmap initialized for show {}", saved.getId());
        }

        return savedShows.stream().map(this::toResponse).toList();
    }

    // ---------------- FETCH SEATMAP ----------------
    public Map<String, Object> fetchSeatmap(UUID hallId, UUID showId) {
        String key = redisKey(showId);
        Map<Object, Object> seatMap = redisTemplate.opsForHash().entries(key);

        if (seatMap == null || seatMap.isEmpty()) {
            log.warn("⚠️ No Redis seatmap for show {}, falling back to DB", showId);
            Show show = showRepository.findById(showId)
                    .orElseThrow(() -> new IllegalArgumentException("Show not found " + showId));
            Map<String, Object> result = new HashMap<>();
            result.put("availableSeats", show.getAvailableSeats());
            result.put("lockedSeats", show.getLockedSeats());
            result.put("bookedSeats", show.getBookedSeats());
            return result;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("availableSeats", new ArrayList<>((Set<String>) seatMap.get("available")));
        result.put("lockedSeats", new ArrayList<>((Set<String>) seatMap.get("locked")));
        result.put("bookedSeats", new ArrayList<>((Set<String>) seatMap.get("booked")));
        return result;
    }

    // ---------------- LOCK SEATS ----------------
    @Transactional
    public ResponseEntity<Void> lockSeats(SeatUpdateRequest req) {
        String key = redisKey(req.getShowId());
        HashOperations<String, String, Set<String>> ops = redisTemplate.opsForHash();

        Set<String> available = (Set<String>) ops.get(key, "available");
        Set<String> locked = (Set<String>) ops.get(key, "locked");
        Set<String> booked = (Set<String>) ops.get(key, "booked");

        if (available == null) available = new HashSet<>();
        if (locked == null) locked = new HashSet<>();
        if (booked == null) booked = new HashSet<>();

        // Validate availability in Redis (source-of-truth for transient locks)
        for (String seat : req.getSeats()) {
            if (!available.contains(seat) || booked.contains(seat) || locked.contains(seat)) {
                log.warn("❌ Seat conflict: show={} seat={}", req.getShowId(), seat);
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
        }

        // Update Redis
        available.removeAll(req.getSeats());
        locked.addAll(req.getSeats());

        ops.put(key, "available", available);
        ops.put(key, "locked", locked);

        // Persist locked seats in DB as a mirror for audit / durability
        Show show = showRepository.findById(req.getShowId())
                .orElseThrow(() -> new IllegalArgumentException("Show not found " + req.getShowId()));

        // Ensure lists are non-null
        if (show.getLockedSeats() == null) show.setLockedSeats(new ArrayList<>());
        if (show.getAvailableSeats() == null) show.setAvailableSeats(new ArrayList<>());
        // Keep DB list in sync (remove from available, add to locked)
        show.getAvailableSeats().removeAll(req.getSeats());
        show.getLockedSeats().addAll(req.getSeats());
        showRepository.save(show);

        log.info("🔒 Locked seats in Redis + DB: show={} seats={}", req.getShowId(), req.getSeats());
        return ResponseEntity.ok().build();
    }


    // ---------------- CONFIRM SEATS ----------------
    @Transactional
    public void confirmSeats(UUID showId, List<String> seats) {
        // ----- Redis update first (transient state) -----
        String key = redisKey(showId);
        HashOperations<String, String, Set<String>> ops = redisTemplate.opsForHash();

        Set<String> available = (Set<String>) ops.get(key, "available");
        Set<String> locked = (Set<String>) ops.get(key, "locked");
        Set<String> booked = (Set<String>) ops.get(key, "booked");

        if (available == null) available = new HashSet<>();
        if (locked == null) locked = new HashSet<>();
        if (booked == null) booked = new HashSet<>();

        // Validate seats were actually locked
        final Set<String> lockedSeats = locked;
        List<String> notLocked = seats.stream()
                .filter(seat -> !lockedSeats.contains(seat))
                .toList();
        if (!notLocked.isEmpty()) {
            log.warn("⚠️ Trying to confirm seats not locked in Redis for show {}: {}", showId, notLocked);
            // You can throw an exception here if you want to reject the request
        }

        // Move seats from locked/available → booked
        locked.removeAll(seats);
        available.removeAll(seats);
        booked.addAll(seats);

        ops.put(key, "available", available);
        ops.put(key, "locked", locked);
        ops.put(key, "booked", booked);

        // ----- DB final persist (booked) -----
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new RuntimeException("Show not found"));

        if (show.getLockedSeats() == null) show.setLockedSeats(new ArrayList<>());
        if (show.getAvailableSeats() == null) show.setAvailableSeats(new ArrayList<>());
        if (show.getBookedSeats() == null) show.setBookedSeats(new ArrayList<>());

        // Keep DB state in sync
        show.getLockedSeats().removeAll(seats);
        show.getAvailableSeats().removeAll(seats);
        show.getBookedSeats().addAll(seats);

        showRepository.save(show);
        log.info("✅ Confirmed seats in DB + Redis: show={} seats={}", showId, seats);
    }

    // ---------------- RELEASE SEATS ----------------
    @Transactional
    public void releaseSeats(UUID showId, List<String> seats) {
        String key = redisKey(showId);
        HashOperations<String, String, Set<String>> ops = redisTemplate.opsForHash();

        Set<String> available = (Set<String>) ops.get(key, "available");
        Set<String> locked = (Set<String>) ops.get(key, "locked");
        Set<String> booked = (Set<String>) ops.get(key, "booked");

        if (available == null) available = new HashSet<>();
        if (locked == null) locked = new HashSet<>();
        if (booked == null) booked = new HashSet<>();

        // Remove from locked + booked, return to available
        locked.removeAll(seats);
        booked.removeAll(seats);
        available.addAll(seats);

        ops.put(key, "locked", locked);
        ops.put(key, "available", available);
        ops.put(key, "booked", booked);

        // ----- DB persist -----
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new RuntimeException("Show not found"));

        if (show.getLockedSeats() == null) show.setLockedSeats(new ArrayList<>());
        if (show.getAvailableSeats() == null) show.setAvailableSeats(new ArrayList<>());
        if (show.getBookedSeats() == null) show.setBookedSeats(new ArrayList<>());

        show.getLockedSeats().removeAll(seats);
        show.getBookedSeats().removeAll(seats);
        show.getAvailableSeats().addAll(seats);

        showRepository.save(show);

        log.info("♻️ Released seats in DB + Redis: show={} seats={}", showId, seats);
    }

    // ---------------- LIST / GET SHOWS ----------------
    public List<ShowResponse> listShows() {
        return showRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public Optional<ShowResponse> getShow(UUID id) {
        return showRepository.findById(id).map(this::toResponse);
    }

        // ---------------- FIXED: Fetch shows by movie with hall + theatre ----------------
    public List<ShowResponseDTO> getShowsByMovie(UUID movieId) {
        List<Show> shows = showRepository.findByMovieId(movieId);

        String movieTitle = null;
        try {
            String movieUrl = catalogUrl + "/catalog/movies/" + movieId;
            ResponseEntity<Map> response = restTemplate.getForEntity(movieUrl, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> movieMap = response.getBody();
                log.info("Fetched movie from catalog: {}", movieMap);

                Object titleObj = movieMap.get("title");
                if (titleObj == null) titleObj = movieMap.get("name");

                if (titleObj != null) {
                    movieTitle = titleObj.toString();
                } else {
                    log.warn("Movie {} found but title key missing", movieId);
                }
            } else {
                log.warn("Catalog responded {} for movie {}", response.getStatusCode(), movieId);
            }
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Movie {} not found in catalog", movieId);
        } catch (RestClientException e) {
            log.error("Failed to fetch movie {} from catalog: {}", movieId, e.getMessage());
        }

        String finalMovieTitle = movieTitle; // effectively final for lambda

        return shows.stream().map(show -> {
            HallDTO hall = null;
            TheatreDTO theatre = null;

            // 🔹 Fetch hall
            try {
                String hallUrl = catalogUrl + "/catalog/halls/" + show.getHallId();
                @SuppressWarnings("unchecked")
                Map<String, Object> hallMap = restTemplate.getForObject(hallUrl, Map.class);

                if (hallMap != null) {
                    hall = new HallDTO();

                    Object idObj = hallMap.getOrDefault("id", hallMap.get("hallId"));
                    if (idObj != null) hall.setHallId(UUID.fromString(idObj.toString()));

                    Object nameObj = hallMap.getOrDefault("name", hallMap.get("hallName"));
                    if (nameObj != null) hall.setHallName(nameObj.toString());

                    Object capObj = hallMap.getOrDefault("capacity", hallMap.get("seatingCapacity"));
                    if (capObj != null) {
                        try {
                            hall.setCapacity(Integer.parseInt(capObj.toString()));
                        } catch (NumberFormatException ignored) {}
                    }

                    Object theatreIdObj = hallMap.get("theaterId");
                    if (theatreIdObj == null) theatreIdObj = hallMap.get("theatreId");
                    if (theatreIdObj != null) {
                        try {
                            hall.setTheatreId(UUID.fromString(theatreIdObj.toString()));
                        } catch (IllegalArgumentException ignored) {}
                    }
                } else {
                    log.warn("Catalog returned null for hall {}", show.getHallId());
                }
            } catch (RestClientException e) {
                log.error("Failed to fetch hall {} from catalog: {}", show.getHallId(), e.getMessage());
            }

            // 🔹 Fetch theatre
            try {
                UUID theatreId = (hall != null) ? hall.getTheatreId() : null;
                if (theatreId != null) {
                    String theatreUrl = catalogUrl + "/catalog/theatres/" + theatreId;
                    @SuppressWarnings("unchecked")
                    Map<String, Object> theatreMap = restTemplate.getForObject(theatreUrl, Map.class);

                    if (theatreMap != null) {
                        theatre = new TheatreDTO();

                        Object idObj = theatreMap.getOrDefault("id", theatreMap.get("theatreId"));
                        if (idObj != null) theatre.setTheatreId(UUID.fromString(idObj.toString()));

                        Object nameObj = theatreMap.getOrDefault("name", theatreMap.get("theatreName"));
                        if (nameObj != null) theatre.setTheatreName(nameObj.toString());

                        Object addrObj = theatreMap.getOrDefault("address", theatreMap.get("addr"));
                        if (addrObj != null) theatre.setAddress(addrObj.toString());

                        Object cityObj = theatreMap.getOrDefault("city", theatreMap.get("location"));
                        if (cityObj != null) theatre.setCity(cityObj.toString());
                    }
                } else {
                    // optional: theatre-by-hall fallback
                    String byHallUrl = catalogUrl + "/catalog/theatres/by-hall/" + show.getHallId();
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> theatreMap = restTemplate.getForObject(byHallUrl, Map.class);
                        if (theatreMap != null) {
                            theatre = new TheatreDTO();
                            Object idObj = theatreMap.getOrDefault("id", theatreMap.get("theatreId"));
                            if (idObj != null) theatre.setTheatreId(UUID.fromString(idObj.toString()));
                            Object nameObj = theatreMap.getOrDefault("name", theatreMap.get("theatreName"));
                            if (nameObj != null) theatre.setTheatreName(nameObj.toString());
                            Object addrObj = theatreMap.getOrDefault("address", theatreMap.get("addr"));
                            if (addrObj != null) theatre.setAddress(addrObj.toString());
                            Object cityObj = theatreMap.getOrDefault("city", theatreMap.get("location"));
                            if (cityObj != null) theatre.setCity(cityObj.toString());
                        }
                    } catch (RestClientException ex) {
                        log.debug("Theatre-by-hall lookup failed for hall {}: {}", show.getHallId(), ex.getMessage());
                    }
                }
            } catch (RestClientException e) {
                log.error("Failed to fetch theatre for show/hall {}: {}", show.getHallId(), e.getMessage());
            }

            // 🔹 Build final DTO
            ShowResponseDTO dto = new ShowResponseDTO();
            dto.setShowId(show.getId());
            dto.setStartTime(show.getStartTime());
            dto.setEndTime(show.getEndTime());
            dto.setBasePrice(show.getBasePrice());
            dto.setHall(hall);
            dto.setTheatre(theatre);
            dto.setMovieTitle(finalMovieTitle); // ✅ only movie title

            return dto;
        }).collect(Collectors.toList());
    }
    // ---------------- Helpers ----------------
    private List<String> generateSeatMap() {
        List<String> seats = new ArrayList<>();
        for (char row = 'A'; row <= 'I'; row++) {
            for (int num = 1; num <= 9; num++) {
                seats.add(row + String.valueOf(num));
            }
        }
        return seats;
    }

    private ShowResponse toResponse(Show s) {
        ShowResponse r = new ShowResponse();
        r.setId(s.getId());
        r.setMovieId(s.getMovieId());
        r.setHallId(s.getHallId());
        r.setStartTime(s.getStartTime());
        r.setEndTime(s.getEndTime());
        r.setBasePrice(s.getBasePrice());
        r.setAvailableSeats(s.getAvailableSeats());
        r.setBookedSeats(s.getBookedSeats());
        return r;
    }

    private static class Interval {
        private final OffsetDateTime start;
        private final OffsetDateTime end;
        Interval(OffsetDateTime s, OffsetDateTime e) { this.start = s; this.end = e; }
        boolean overlaps(Interval other) {
            return this.start.isBefore(other.end) && this.end.isAfter(other.start);
        }
        public String toString() { return start + "->" + end; }
    }

        public SeatMapResponseDTO getSeatMap(UUID movieId, UUID hallId, String showTime) {
        OffsetDateTime time = OffsetDateTime.parse(showTime);

        Optional<Show> showOpt = showRepository.findByMovieIdAndHallIdAndStartTime(movieId, hallId, time);
        if (showOpt.isEmpty()) {
            throw new NoSuchElementException("No show found for movieId=" + movieId + " hallId=" + hallId + " time=" + time);
        }

        Show show = showOpt.get();

        Map<String, Object> seatMap = fetchSeatmap(hallId, show.getId());

        SeatMapResponseDTO dto = new SeatMapResponseDTO();
        dto.setShowId(show.getId());
        dto.setMovieId(movieId);
        dto.setHallId(hallId);
        dto.setAvailableSeats((List<String>) seatMap.get("availableSeats"));
        dto.setBookedSeats((List<String>) seatMap.get("bookedSeats"));

        return dto;
    }
}

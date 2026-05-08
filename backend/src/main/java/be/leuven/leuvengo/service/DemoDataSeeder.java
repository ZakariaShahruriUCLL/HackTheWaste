package be.leuven.leuvengo.service;

import be.leuven.leuvengo.domain.*;
import be.leuven.leuvengo.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;

/**
 * Seeds Leuven-specific demo data: faculties (clans) with territories,
 * iconic street segments (Oude Markt, student housing clusters), reward
 * marketplace items, and a backlog of recent reports so dashboards have
 * something interesting to show out of the box.
 */
@Component
public class DemoDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final FacultyRepository faculties;
    private final StreetSegmentRepository segments;
    private final RewardItemRepository rewards;
    private final ReportRepository reports;
    private final ReportService reportService;
    private final ScoringService scoring;

    public DemoDataSeeder(FacultyRepository faculties,
                          StreetSegmentRepository segments,
                          RewardItemRepository rewards,
                          ReportRepository reports,
                          ReportService reportService,
                          ScoringService scoring) {
        this.faculties = faculties;
        this.segments = segments;
        this.rewards = rewards;
        this.reports = reports;
        this.reportService = reportService;
        this.scoring = scoring;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        if (faculties.count() > 0) return;
        log.info("Seeding Leuven Go demo data...");

        List<Faculty> facs = seedFaculties();
        seedSegments(facs);
        seedRewards();
        seedReports();
        scoring.recomputeAll();

        log.info("Seed complete: {} faculties, {} segments, {} rewards, {} reports",
                faculties.count(), segments.count(), rewards.count(), reports.count());
    }

    private List<Faculty> seedFaculties() {
        Faculty engineering = faculty("Engineering Science", "ENG", "#2563eb", "🛠",
                3200, 0,
                polygon(50.8645, 4.6770, 50.8645, 4.6900, 50.8740, 4.6900, 50.8740, 4.6770));
        Faculty law = faculty("Law", "LAW", "#dc2626", "⚖️", 2100, 0,
                polygon(50.8770, 4.6900, 50.8770, 4.7050, 50.8830, 4.7050, 50.8830, 4.6900));
        Faculty medicine = faculty("Medicine", "MED", "#16a34a", "🩺", 2900, 0,
                polygon(50.8520, 4.6680, 50.8520, 4.6800, 50.8620, 4.6800, 50.8620, 4.6680));
        Faculty arts = faculty("Arts & Humanities", "ART", "#9333ea", "🎭", 2400, 0,
                polygon(50.8780, 4.6970, 50.8780, 4.7090, 50.8860, 4.7090, 50.8860, 4.6970));
        Faculty economics = faculty("Economics & Business", "ECO", "#f59e0b", "📈", 2700, 0,
                polygon(50.8800, 4.7000, 50.8800, 4.7150, 50.8870, 4.7150, 50.8870, 4.7000));
        Faculty science = faculty("Science", "SCI", "#0891b2", "🧪", 2200, 0,
                polygon(50.8650, 4.6700, 50.8650, 4.6810, 50.8730, 4.6810, 50.8730, 4.6700));
        return faculties.saveAll(List.of(engineering, law, medicine, arts, economics, science));
    }

    private Faculty faculty(String name, String code, String color, String emoji,
                            int members, int points, String geoJson) {
        Faculty f = new Faculty();
        f.setName(name);
        f.setShortCode(code);
        f.setColor(color);
        f.setEmoji(emoji);
        f.setMembers(members);
        f.setPoints(points);
        f.setTerritoryGeoJson(geoJson);
        return f;
    }

    private void seedSegments(List<Faculty> facs) {
        Faculty arts = facs.stream().filter(f -> f.getShortCode().equals("ART")).findFirst().orElse(null);
        Faculty law = facs.stream().filter(f -> f.getShortCode().equals("LAW")).findFirst().orElse(null);
        Faculty eng = facs.stream().filter(f -> f.getShortCode().equals("ENG")).findFirst().orElse(null);
        Faculty eco = facs.stream().filter(f -> f.getShortCode().equals("ECO")).findFirst().orElse(null);
        Faculty med = facs.stream().filter(f -> f.getShortCode().equals("MED")).findFirst().orElse(null);
        Faculty sci = facs.stream().filter(f -> f.getShortCode().equals("SCI")).findFirst().orElse(null);

        segments.saveAll(List.of(
                segment("Oude Markt", "Centrum", 50.8780, 4.7010, arts),
                segment("Naamsestraat", "Centrum", 50.8762, 4.7008, law),
                segment("Tiensestraat", "Centrum", 50.8800, 4.7080, eco),
                segment("Vaartkom", "Noord", 50.8870, 4.7050, eng),
                segment("Heverlee Campus", "Heverlee", 50.8635, 4.6770, eng),
                segment("Gasthuisberg", "Zuid", 50.8525, 4.6720, med),
                segment("Sint-Maartensdal", "Centrum", 50.8830, 4.6960, arts),
                segment("Bondgenotenlaan", "Centrum", 50.8810, 4.7050, eco),
                segment("Parkstraat", "Heverlee", 50.8720, 4.6960, sci),
                segment("Diestsestraat", "Centrum", 50.8830, 4.7060, eco),
                segment("Brusselsestraat", "Centrum", 50.8775, 4.6960, law),
                segment("Kessel-Lo Station", "Kessel-Lo", 50.8830, 4.7240, eco)
        ));
    }

    private StreetSegment segment(String name, String district, double lat, double lng, Faculty f) {
        StreetSegment s = new StreetSegment();
        s.setName(name);
        s.setDistrict(district);
        s.setCenterLat(lat);
        s.setCenterLng(lng);
        s.setAiCleanlinessScore(80d);
        s.setReportCount30d(0);
        s.setLastEvaluatedAt(Instant.now());
        s.setFaculty(f);
        return s;
    }

    private void seedRewards() {
        rewards.saveAll(List.of(
                reward("Free Stella @ Oude Markt", "One pint at any participating bar around Oude Markt.",
                        "City of Leuven", "Drinks", 120, 50),
                reward("Cinema Ticket - Kinepolis", "Standard ticket valid any weekday.",
                        "Kinepolis", "Entertainment", 200, 30),
                reward("LIBIS Library Print Credit (€5)", "Printing credit on your student card.",
                        "KU Leuven Libraries", "Study", 80, 100),
                reward("Velo Bike Day Pass", "24h shared-bike access across Leuven.",
                        "Velo", "Transport", 60, 200),
                reward("Alma Lunch Voucher", "Free lunch at any Alma student restaurant.",
                        "Alma", "Food", 100, 75),
                reward("STUK Concert Ticket", "Entry to a STUK arts centre event.",
                        "STUK", "Entertainment", 220, 20),
                reward("Stadsmagazijn Tour", "Behind-the-scenes city services tour.",
                        "City of Leuven", "Experience", 30, 500),
                reward("Leuven Go Hoodie", "Limited edition campaign hoodie.",
                        "City of Leuven", "Merch", 350, 40)
        ));
    }

    private RewardItem reward(String title, String desc, String sponsor,
                              String category, int points, int stock) {
        RewardItem r = new RewardItem();
        r.setTitle(title);
        r.setDescription(desc);
        r.setSponsor(sponsor);
        r.setCategory(category);
        r.setCostPoints(points);
        r.setStock(stock);
        return r;
    }

    private void seedReports() {
        Random rnd = new Random(42);
        // Three real-feeling clusters: Oude Markt (party), Naamsestraat (student housing), Vaartkom
        seedCluster(rnd, 50.8780, 4.7010, 7, 1, "ART", "litter,glass,overflow");
        seedCluster(rnd, 50.8762, 4.7008, 4, 2, "LAW", "litter,packaging");
        seedCluster(rnd, 50.8870, 4.7050, 3, 2, "ENG", "graffiti,litter");
        seedCluster(rnd, 50.8635, 4.6770, 2, 4, "ENG", "leaves");
        seedCluster(rnd, 50.8830, 4.7240, 5, 1, "ECO", "overflow,packaging");

        // Backdate the timestamps so the trend chart has shape.
        List<Report> all = reports.findAll();
        for (int i = 0; i < all.size(); i++) {
            Report r = all.get(i);
            r.setReportedAt(Instant.now().minus(rnd.nextInt(96), ChronoUnit.HOURS));
            reports.save(r);
        }
    }

    private void seedCluster(Random rnd, double lat, double lng, int n,
                             int baseRating, String facCode, String tags) {
        for (int i = 0; i < n; i++) {
            double dLat = (rnd.nextDouble() - 0.5) * 0.0006; // ~30m
            double dLng = (rnd.nextDouble() - 0.5) * 0.0006;
            int rating = Math.max(0, Math.min(5, baseRating + rnd.nextInt(2)));
            reportService.submit(new ReportService.Submission(
                    lat + dLat, lng + dLng, rating,
                    "Demo report - " + tags, null, tags,
                    facCode, "seed-" + facCode + "-" + i));
        }
    }

    private String polygon(double... latLngs) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"Polygon\",\"coordinates\":[[");
        for (int i = 0; i < latLngs.length; i += 2) {
            if (i > 0) sb.append(",");
            // GeoJSON uses [lng, lat]
            sb.append("[").append(latLngs[i + 1]).append(",").append(latLngs[i]).append("]");
        }
        // close the ring
        sb.append(",[").append(latLngs[1]).append(",").append(latLngs[0]).append("]");
        sb.append("]]}");
        return sb.toString();
    }
}

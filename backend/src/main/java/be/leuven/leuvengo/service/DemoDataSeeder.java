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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    /**
     * Teams from the 24 Uur van Leuven (24 Hours of Leuven) student competition.
     * Territories are laid out on a 7x3 grid roughly covering Leuven's centre + campuses
     * so the student dashboard map shows a clean tiling rather than overlaps.
     */
    private List<Faculty> seedFaculties() {
        String[][] data = {
                {"Apolloon", "APO", "#1d4ed8", "🏃"},
                {"Ekonomika", "EKO", "#10b981", "📊"},
                {"LBK", "LBK", "#65a30d", "🌱"},
                {"VTK", "VTK", "#dc2626", "⚙️"},
                {"Wina", "WIN", "#ec4899", "🧮"},
                {"UCLL", "UCL", "#2563eb", "🎓"},
                {"Psychokring", "PSY", "#9333ea", "🧠"},
                {"Thomas Morus", "TMO", "#ca8a04", "📜"},
                {"Atmosphere", "ATM", "#0ea5e9", "🌍"},
                {"Runner's High", "RNH", "#f97316", "💨"},
                {"Industria", "IND", "#1e3a8a", "🏭"},
                {"Farmaceutica", "FAR", "#14b8a6", "💊"},
                {"VRG-Crimen", "VRG", "#991b1b", "⚖️"},
                {"Politika", "POL", "#0f172a", "🗳"},
                {"Run for Specials", "RFS", "#f59e0b", "🌟"},
                {"HMV", "HMV", "#a78bfa", "🎵"},
                {"Pedal", "PED", "#0d9488", "🚴"},
                {"Lerkeveld", "LRK", "#78350f", "🏠"},
                {"ESN", "ESN", "#3b82f6", "🌐"},
                {"Project Unseen", "PUS", "#475569", "👁"},
                {"Humasol", "HUM", "#eab308", "☀️"}
        };
        int[] members = {1200, 2400, 800, 2800, 1100, 1900, 1600, 700, 900, 600, 2200,
                1400, 1300, 1000, 500, 750, 850, 1150, 1500, 950, 1300};

        int cols = 7;
        double baseLat = 50.860;
        double baseLng = 4.668;
        double tileLat = 0.010;
        double tileLng = 0.0085;

        List<Faculty> list = new ArrayList<>();
        for (int i = 0; i < data.length; i++) {
            int col = i % cols;
            int row = i / cols;
            double minLat = baseLat + row * tileLat;
            double maxLat = minLat + tileLat;
            double minLng = baseLng + col * tileLng;
            double maxLng = minLng + tileLng;
            list.add(faculty(
                    data[i][0], data[i][1], data[i][2], data[i][3],
                    members[i], 0,
                    polygon(minLat, minLng, minLat, maxLng, maxLat, maxLng, maxLat, minLng)
            ));
        }
        return faculties.saveAll(list);
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
        Map<String, Faculty> by = new HashMap<>();
        facs.forEach(f -> by.put(f.getShortCode(), f));

        segments.saveAll(List.of(
                segment("Oude Markt", "Centrum", 50.8780, 4.7010, by.get("APO")),
                segment("Naamsestraat", "Centrum", 50.8762, 4.7008, by.get("EKO")),
                segment("Tiensestraat", "Centrum", 50.8800, 4.7080, by.get("VTK")),
                segment("Vaartkom", "Noord", 50.8870, 4.7050, by.get("IND")),
                segment("Heverlee Campus", "Heverlee", 50.8635, 4.6770, by.get("WIN")),
                segment("Gasthuisberg", "Zuid", 50.8525, 4.6720, by.get("FAR")),
                segment("Sint-Maartensdal", "Centrum", 50.8830, 4.6960, by.get("UCL")),
                segment("Bondgenotenlaan", "Centrum", 50.8810, 4.7050, by.get("ESN")),
                segment("Parkstraat", "Heverlee", 50.8720, 4.6960, by.get("PSY")),
                segment("Diestsestraat", "Centrum", 50.8830, 4.7060, by.get("LBK")),
                segment("Brusselsestraat", "Centrum", 50.8775, 4.6960, by.get("TMO")),
                segment("Kessel-Lo Station", "Kessel-Lo", 50.8830, 4.7240, by.get("PED"))
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
        seedCluster(rnd, 50.8780, 4.7010, 7, 1, "APO", "litter,glass,overflow");
        seedCluster(rnd, 50.8762, 4.7008, 4, 2, "EKO", "litter,packaging");
        seedCluster(rnd, 50.8870, 4.7050, 3, 2, "IND", "graffiti,litter");
        seedCluster(rnd, 50.8635, 4.6770, 2, 4, "WIN", "leaves");
        seedCluster(rnd, 50.8830, 4.7240, 5, 1, "PED", "overflow,packaging");

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

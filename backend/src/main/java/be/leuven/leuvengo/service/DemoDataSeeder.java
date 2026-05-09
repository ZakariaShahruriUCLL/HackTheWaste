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

    private List<Faculty> seedFaculties() {
        // 5 lat bands × irregular lng splits = 21 contiguous blocky territories
        Object[][] data = {
            // --- North band (50.890–50.898) ---
            {"Project Unseen",   "PUS", "#475569", "👁",   950, polygon(50.890,4.670, 50.898,4.670, 50.898,4.697, 50.890,4.697)},
            {"LBK",              "LBK", "#65a30d", "🌱",   800, polygon(50.890,4.697, 50.898,4.697, 50.898,4.710, 50.890,4.710)},
            {"ESN",              "ESN", "#3b82f6", "🌐",  1500, polygon(50.890,4.710, 50.898,4.710, 50.898,4.730, 50.890,4.730)},

            // --- Upper band (50.882–50.890) ---
            {"Run for Specials", "RFS", "#f59e0b", "🌟",   500, polygon(50.882,4.670, 50.890,4.670, 50.890,4.684, 50.882,4.684)},
            {"Apolloon",         "APO", "#1d4ed8", "🏃",  1200, polygon(50.882,4.684, 50.890,4.684, 50.890,4.699, 50.882,4.699)},
            {"Industria",        "IND", "#1e3a8a", "🏭",  2200, polygon(50.882,4.699, 50.890,4.699, 50.890,4.710, 50.882,4.710)},
            {"Atmosphere",       "ATM", "#0ea5e9", "🌍",   900, polygon(50.882,4.710, 50.890,4.710, 50.890,4.720, 50.882,4.720)},
            {"Humasol",          "HUM", "#eab308", "☀️",  1300, polygon(50.882,4.720, 50.890,4.720, 50.890,4.730, 50.882,4.730)},

            // --- Middle band (50.874–50.882) ---
            {"Psychokring",      "PSY", "#9333ea", "🧠",  1600, polygon(50.874,4.670, 50.882,4.670, 50.882,4.684, 50.874,4.684)},
            {"Ekonomika",        "EKO", "#10b981", "📊",  2400, polygon(50.874,4.684, 50.882,4.684, 50.882,4.699, 50.874,4.699)},
            {"Thomas Morus",     "TMO", "#ca8a04", "📜",   700, polygon(50.874,4.699, 50.882,4.699, 50.882,4.708, 50.874,4.708)},
            {"VRG-Crimen",       "VRG", "#991b1b", "⚖️",  1300, polygon(50.874,4.708, 50.882,4.708, 50.882,4.719, 50.874,4.719)},
            {"UCLL",             "UCL", "#2563eb", "🎓",  1900, polygon(50.874,4.719, 50.882,4.719, 50.882,4.730, 50.874,4.730)},

            // --- Lower band (50.866–50.874) ---
            {"Wina",             "WIN", "#ec4899", "🧮",  1100, polygon(50.866,4.670, 50.874,4.670, 50.874,4.684, 50.866,4.684)},
            {"Lerkeveld",        "LRK", "#78350f", "🏠",  1150, polygon(50.866,4.684, 50.874,4.684, 50.874,4.699, 50.866,4.699)},
            {"VTK",              "VTK", "#dc2626", "⚙️",  2800, polygon(50.866,4.699, 50.874,4.699, 50.874,4.708, 50.866,4.708)},
            {"HMV",              "HMV", "#a78bfa", "🎵",   750, polygon(50.866,4.708, 50.874,4.708, 50.874,4.719, 50.866,4.719)},
            {"Pedal",            "PED", "#0d9488", "🚴",   850, polygon(50.866,4.719, 50.874,4.719, 50.874,4.730, 50.866,4.730)},

            // --- South band (50.850–50.866) ---
            {"Farmaceutica",     "FAR", "#14b8a6", "💊",  1400, polygon(50.850,4.670, 50.866,4.670, 50.866,4.693, 50.850,4.693)},
            {"Politika",         "POL", "#0f172a", "🗳️", 1000, polygon(50.850,4.693, 50.866,4.693, 50.866,4.712, 50.850,4.712)},
            {"Runner's High",    "RNH", "#f97316", "💨",   600, polygon(50.850,4.712, 50.866,4.712, 50.866,4.730, 50.850,4.730)},
        };

        List<Faculty> list = new ArrayList<>();
        for (Object[] row : data) {
            list.add(faculty(
                    (String) row[0], (String) row[1], (String) row[2], (String) row[3],
                    (int) row[4], 0, (String) row[5]
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
                segment("Oude Markt",        "Centrum",   50.8780, 4.7010, by.get("APO")),
                segment("Naamsestraat",      "Centrum",   50.8762, 4.7008, by.get("EKO")),
                segment("Tiensestraat",      "Centrum",   50.8800, 4.7080, by.get("VTK")),
                segment("Vaartkom",          "Noord",     50.8870, 4.7050, by.get("IND")),
                segment("Heverlee Campus",   "Heverlee",  50.8635, 4.6770, by.get("WIN")),
                segment("Gasthuisberg",      "Zuid",      50.8525, 4.6720, by.get("FAR")),
                segment("Sint-Maartensdal",  "Centrum",   50.8830, 4.6960, by.get("UCL")),
                segment("Bondgenotenlaan",   "Centrum",   50.8810, 4.7050, by.get("ESN")),
                segment("Parkstraat",        "Heverlee",  50.8720, 4.6960, by.get("PSY")),
                segment("Diestsestraat",     "Centrum",   50.8830, 4.7060, by.get("LBK")),
                segment("Brusselsestraat",   "Centrum",   50.8775, 4.6960, by.get("TMO")),
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

        seedCluster(rnd, 50.8780, 4.7010, 15, 4, "APO", "litter,glass,party_night");
        seedCluster(rnd, 50.8740, 4.7000, 10, 3, "EKO", "packaging,takeaway");
        seedCluster(rnd, 50.8815, 4.7155,  8, 3, "PED", "overflow,coffee_cups");
        seedCluster(rnd, 50.8870, 4.7050,  6, 2, "IND", "packaging,bottles");
        seedCluster(rnd, 50.8635, 4.6770,  5, 2, "WIN", "food_waste");

        List<Report> all = reports.findAll();
        for (Report r : all) {
            r.setReportedAt(Instant.now().minus(rnd.nextInt(96), ChronoUnit.HOURS));
            reports.save(r);
        }
    }

    private void seedCluster(Random rnd, double lat, double lng, int n,
                             int baseRating, String facCode, String tags) {
        for (int i = 0; i < n; i++) {
            double dLat = rnd.nextGaussian() * 0.0008;
            double dLng = rnd.nextGaussian() * 0.0008;
            int rating = Math.max(1, Math.min(5, baseRating + (rnd.nextInt(3) - 1)));
            reportService.submit(new ReportService.Submission(
                    lat + dLat, lng + dLng, rating,
                    "Predictive Analysis: High probability zone (" + tags + ")",
                    null, tags,
                    facCode, "ai-seed-" + facCode + "-" + i + "-" + rnd.nextInt(1000)));
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

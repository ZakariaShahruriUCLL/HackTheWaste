package be.leuven.leuvengo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "leuvengo")
public class AppProperties {

    private Planon planon = new Planon();
    private Traffic traffic = new Traffic();
    private City city = new City();

    public Planon getPlanon() { return planon; }
    public Traffic getTraffic() { return traffic; }
    public City getCity() { return city; }

    public static class Planon {
        private String baseUrl;
        private String apiKey;
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    }

    public static class Traffic {
        private double clusterRadiusM = 40;
        private int reportTtlHours = 72;
        private int workOrderThreshold = 3;
        public double getClusterRadiusM() { return clusterRadiusM; }
        public void setClusterRadiusM(double v) { this.clusterRadiusM = v; }
        public int getReportTtlHours() { return reportTtlHours; }
        public void setReportTtlHours(int v) { this.reportTtlHours = v; }
        public int getWorkOrderThreshold() { return workOrderThreshold; }
        public void setWorkOrderThreshold(int v) { this.workOrderThreshold = v; }
    }

    public static class City {
        private String name;
        private double centerLat;
        private double centerLng;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public double getCenterLat() { return centerLat; }
        public void setCenterLat(double v) { this.centerLat = v; }
        public double getCenterLng() { return centerLng; }
        public void setCenterLng(double v) { this.centerLng = v; }
    }
}

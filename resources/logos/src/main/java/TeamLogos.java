package resources.logos.src.main.java;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class TeamLogos {
    // Base path for resources
    private static final String BASE_PATH = "/logos/";
    
    // Map to store team logo paths
    public static final Map<String, String> TEAM_LOGOS = new HashMap<>() {{
        put("Sunrisers Hyderabad", BASE_PATH + "Sunrisers Hyderabad.png");
        put("Royal Challengers Bengaluru", BASE_PATH + "Royal Challengers Bengaluru.png");
        put("Rajasthan Royals", BASE_PATH + "Rajasthan Royals.png");
        put("Punjab Kings", BASE_PATH + "Punjab Kings.png");
        put("Mumbai Indians", BASE_PATH + "Mumbai Indians.png");
        put("Lucknow Super Giants", BASE_PATH + "Lucknow Super Giants.png");
        put("Kolkata Knight Riders", BASE_PATH + "Kolkata Knight Riders.png");
        put("Gujarat Titans", BASE_PATH + "Gujarat Titans.png");
        put("Delhi Capitals", BASE_PATH + "Delhi Capitals.png");
        put("Chennai Super Kings", BASE_PATH + "Chennai Super Kings.png");
    }};
    
    // Utility method to load team logo
    public static ImageIcon getTeamLogo(String teamName) {
        try {
            String path = TEAM_LOGOS.get(teamName);
            if (path != null) {
                ImageIcon originalIcon = new ImageIcon(TeamLogos.class.getResource(path));
                // Resize image to 64x64
                Image image = originalIcon.getImage().getScaledInstance(64, 64, Image.SCALE_SMOOTH);
                return new ImageIcon(image);
            }
        } catch (Exception e) {
            System.err.println("Error loading logo for " + teamName + ": " + e.getMessage());
        }
        return createDefaultLogo(); // Return a default logo if loading fails
    }
    
    // Create a default logo if image loading fails
    private static ImageIcon createDefaultLogo() {
        BufferedImage defaultImage = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = defaultImage.createGraphics();
        g2d.setColor(Color.GRAY);
        g2d.fillOval(8, 8, 48, 48);
        g2d.dispose();
        return new ImageIcon(defaultImage);
    }
}
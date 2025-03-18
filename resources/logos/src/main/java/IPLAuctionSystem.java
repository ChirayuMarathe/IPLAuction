package resources.logos.src.main.java;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.text.NumberFormat;
import java.util.List;
import javax.swing.border.*;

import java.io.*;
import javax.swing.Timer;
import java.awt.image.BufferedImage;

public class IPLAuctionSystem extends JFrame {
    private final int MAX_PLAYERS_PER_TEAM = 25;
    private final int MIN_PLAYERS_PER_TEAM = 18;
    private final int TEAM_BUDGET = 100000; // Increased budget for more realistic values
    private final double BID_INCREMENT = 500; // Fixed increment
    private final int MIN_BID = 2000; // Minimum bid amount
    
    // Team colors
    private final Map<String, Color> TEAM_COLORS = new HashMap<>() {{
        put("Chennai Super Kings", new Color(255, 215, 0));     // #FFD700
        put("Delhi Capitals", new Color(0, 56, 117));          // #003875
        put("Gujarat Titans", new Color(51, 204, 255));        // #33CCFF
        put("Kolkata Knight Riders", new Color(58, 34, 93));   // #3A225D
        put("Lucknow Super Giants", new Color(255, 215, 0));   // #FFD700
        put("Mumbai Indians", new Color(0, 75, 160));          // #004BA0
        put("Punjab Kings", new Color(237, 27, 36));           // #ED1B24
        put("Rajasthan Royals", new Color(255, 20, 147));      // #FF1493
        put("Royal Challengers Bengaluru", new Color(236, 28, 36)); // #EC1C24
        put("Sunrisers Hyderabad", new Color(255, 130, 42));   // #FF822A
    }};

    private String[] teams = TEAM_COLORS.keySet().toArray(new String[0]);
    
    private Map<String, Integer> teamBudgets;
    private Map<String, List<PlayerInfo>> teamPlayers;
    private List<PlayerInfo> players;
    private Set<String> soldPlayers;
    
    private int currentPlayerIndex = 0;
    private double currentBid = MIN_BID;
    private String highestBidder = null;
    private Timer bidTimer;
    private Timer randomBidTimer;
    private int timeLeft = 30;
    private boolean isAuctionPaused = false;
    
    // GUI components
    private JLabel currentPlayerLabel, currentBidLabel, bidderLabel;
    private JLabel timerLabel, baseValueLabel;
    private JProgressBar timerProgressBar;
    private JPanel teamButtonsPanel;
    private JTextArea auctionLog;
    private JPanel playerInfoPanel;
    
    public static class PlayerInfo implements Serializable {
        String name;
        String role;
        int basePrice;
        int finalPrice;
        String purchasedBy;
        String nationality;
        
        PlayerInfo(String name, String role, int basePrice, String nationality) {
            this.name = name;
            this.role = role;
            this.basePrice = basePrice;
            this.nationality = nationality;
            this.finalPrice = 0;
            this.purchasedBy = null;
        }
    }

    public IPLAuctionSystem() {
        setTitle("IPL Auction System 2024");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        initializeData();
        initializeTimers();
        buildGUI();
    }
    
    private void initializeData() {
        teamBudgets = new HashMap<>();
        teamPlayers = new HashMap<>();
        soldPlayers = new HashSet<>();
        players = new ArrayList<>();
        
        // Initialize team data
        for (String team : teams) {
            teamBudgets.put(team, TEAM_BUDGET);
            teamPlayers.put(team, new ArrayList<>());
        }
        
        // Add sample players with realistic base prices
        String[][] playerData = {
            {"Virat Kohli", "Batsman", "20000", "India"},
            {"Rohit Sharma", "Batsman", "18000", "India"},
            {"MS Dhoni", "Wicketkeeper", "15000", "India"},
            {"Jos Buttler", "Wicketkeeper", "14000", "England"},
            {"Pat Cummins", "Bowler", "15000", "Australia"},
            {"Hardik Pandya", "All-Rounder", "15000", "India"},
            {"Ben Stokes", "All-Rounder", "16500", "England"},
            {"Jasprit Bumrah", "Bowler", "16000", "India"},
            {"Kane Williamson", "Batsman", "14000", "New Zealand"},
            {"Mitchell Starc", "Bowler", "15500", "Australia"}
        };
        
        for (String[] data : playerData) {
            players.add(new PlayerInfo(
                data[0], 
                data[1], 
                Integer.parseInt(data[2]),
                data[3]
            ));
        }
    }
    
    private void initializeTimers() {
        // Main auction timer
        bidTimer = new Timer(1000, e -> {
            if (!isAuctionPaused) {
                timeLeft--;
                updateTimer();
                if (timeLeft <= 0) {
                    ((Timer)e.getSource()).stop();
                    if (randomBidTimer != null) {
                        randomBidTimer.stop();
                    }
                    handleBidEnd();
                }
            }
        });
        
        // Random bidding timer
        randomBidTimer = new Timer(2000, e -> {
            if (!isAuctionPaused && timeLeft > 0) {
                generateRandomBid();
            }
        });
    }
    
    private void generateRandomBid() {
        Random rand = new Random();
        // Only generate bid if there's sufficient time and random chance
        if (timeLeft > 2 && rand.nextDouble() < 0.6) {
            List<String> eligibleTeams = new ArrayList<>();
            
            // Find teams that can afford the current bid and are not the highest bidder
            for (String team : teams) {
                if (!team.equals(highestBidder) && teamBudgets.get(team) >= currentBid + BID_INCREMENT) {
                    eligibleTeams.add(team);
                }
            }
            
            if (!eligibleTeams.isEmpty()) {
                String biddingTeam = eligibleTeams.get(rand.nextInt(eligibleTeams.size()));
                placeBid(biddingTeam);
            }
        }
    }
    
    private void handleBidEnd() {
        if (highestBidder != null) {
            PlayerInfo currentPlayer = players.get(currentPlayerIndex);
            currentPlayer.finalPrice = (int)currentBid;
            currentPlayer.purchasedBy = highestBidder;
            
            // Update team budget and player list
            teamBudgets.put(highestBidder, teamBudgets.get(highestBidder) - (int)currentBid);
            teamPlayers.get(highestBidder).add(currentPlayer);
            soldPlayers.add(currentPlayer.name);
            
            // Log the sale
            logAuctionEvent(String.format("%s sold to %s for ₹%,d\n", 
                currentPlayer.name, highestBidder, (int)currentBid));
        } else {
            logAuctionEvent(players.get(currentPlayerIndex).name + " went unsold\n");
        }
        
        nextPlayer();
    }
    
    private void placeBid(String team) {
        if (team.equals(highestBidder)) {
            JOptionPane.showMessageDialog(this, "The same team cannot place consecutive bids.", "Invalid Bid", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (teamBudgets.get(team) >= currentBid + BID_INCREMENT) {
            currentBid += BID_INCREMENT;
            highestBidder = team;
            updateUI();
            logAuctionEvent(String.format("%s bids ₹%,d\n", team, (int)currentBid));
        } else {
            JOptionPane.showMessageDialog(this, "Insufficient budget for this bid.", "Invalid Bid", JOptionPane.WARNING_MESSAGE);
        }
    }
    
    private void buildGUI() {
        setLayout(new BorderLayout(10, 10));
        
        // Create main panels
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));

        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        
        // Initialize GUI components
        currentPlayerLabel = new JLabel();
        currentBidLabel = new JLabel();
        bidderLabel = new JLabel();
        timerLabel = new JLabel("Time: 30s", SwingConstants.CENTER);
        baseValueLabel = new JLabel();
        timerProgressBar = new JProgressBar(0, 30);
        auctionLog = new JTextArea();
        
        // Create player info panel
        playerInfoPanel = new JPanel();
        playerInfoPanel.setLayout(new BoxLayout(playerInfoPanel, BoxLayout.Y_AXIS));
        playerInfoPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(0, 0, 0, 50), 1, true),
                "Current Player"
            ),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        currentPlayerLabel.setFont(new Font("Arial", Font.BOLD, 14));
        baseValueLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        currentBidLabel.setFont(new Font("Arial", Font.BOLD, 13));
        bidderLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        
        playerInfoPanel.add(currentPlayerLabel);
        playerInfoPanel.add(baseValueLabel);
        playerInfoPanel.add(currentBidLabel);
        playerInfoPanel.add(bidderLabel);
        
        // Create timer panel
        JPanel timerPanel = new JPanel(new BorderLayout());
        timerLabel.setFont(new Font("Arial", Font.BOLD, 20));
        timerProgressBar.setStringPainted(true);
        timerPanel.add(timerLabel, BorderLayout.NORTH);
        timerPanel.add(timerProgressBar, BorderLayout.CENTER);
        
        // Create team buttons panel
        teamButtonsPanel = new JPanel(new GridLayout(2, 5, 5, 5));
        createTeamButtons();
        
        // Add team buttons panel to a scroll pane
        JScrollPane teamButtonsScrollPane = new JScrollPane(teamButtonsPanel);
        teamButtonsScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        teamButtonsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        
        // Create auction log
        auctionLog = new JTextArea(10, 40); // Set fixed number of rows and columns
        auctionLog.setFont(new Font("Monospaced", Font.PLAIN, 12));
        auctionLog.setBackground(new Color(250, 250, 250));
        auctionLog.setEditable(false);
        
        // Create a scroll pane with fixed size for auction log
        JScrollPane auctionLogScrollPane = new JScrollPane(auctionLog);
        auctionLogScrollPane.setPreferredSize(new Dimension(800, 150)); // Fixed height
        auctionLogScrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150)); // Max height
        auctionLogScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        // Modify the center panel layout to use BorderLayout with fixed sizes
        centerPanel.setLayout(new BorderLayout(10, 10));
        
        // Create a panel for team buttons with fixed size
        JPanel teamButtonsContainer = new JPanel(new BorderLayout());
        teamButtonsContainer.setPreferredSize(new Dimension(800, 400)); // Fixed height for team buttons
        teamButtonsContainer.add(teamButtonsScrollPane, BorderLayout.CENTER);
        
        // Add components to center panel with fixed sizes
        centerPanel.add(teamButtonsContainer, BorderLayout.CENTER);
        centerPanel.add(auctionLogScrollPane, BorderLayout.SOUTH);
        
            // ... [Rest of the buildGUI method remains the same]
        // Add components to main panels
        topPanel.add(playerInfoPanel, BorderLayout.CENTER);
        topPanel.add(timerPanel, BorderLayout.EAST);
        
        centerPanel.add(teamButtonsScrollPane, BorderLayout.CENTER);
        centerPanel.add(auctionLogScrollPane, BorderLayout.SOUTH);
        
        // Add control buttons
        JPanel controlPanel = createControlPanel();
        bottomPanel.add(controlPanel, BorderLayout.CENTER);
        
        // Add main panels to frame
        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
        
        // Add menu bar
        setJMenuBar(createMenuBar());
        
        // Initialize UI
        updateUI();
    }
    
    private void createTeamButtons() {
        teamButtonsPanel.setLayout(new GridLayout(2, 5, 10, 10));
        teamButtonsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Map team names to exact image file names
        Map<String, String> teamLogoFiles = new HashMap<>() {{
            put("Chennai Super Kings", "Chennai Super Kings.png");
            put("Delhi Capitals", "Delhi Capitals.png");
            put("Gujarat Titans", "Gujarat Titans.png");
            put("Kolkata Knight Riders", "Kolkata Knight Riders.png");
            put("Lucknow Super Giants", "Lucknow Super Giants.png");
            put("Mumbai Indians", "Mumbai Indians.png");
            put("Punjab Kings", "Punjab Kings.png");
            put("Rajasthan Royals", "Rajasthan Royals.png");
            put("Royal Challengers Bengaluru", "Royal Challengers Bengaluru.png");
            put("Sunrisers Hyderabad", "Sunrisers Hyderabad.png");
        }};
        
        for (String team : teams) {
            // Create a custom JPanel that paints the logo as background
            JPanel teamCard = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2d = (Graphics2D) g;
                    
                    // Enable antialiasing
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    
                    // Draw background color
                    g2d.setColor(TEAM_COLORS.get(team));
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                    
                    try {
                        // Load and draw the team logo using exact file names
                        String logoPath = "/resources/logos/" + teamLogoFiles.get(team);
                        InputStream logoStream = getClass().getResourceAsStream(logoPath);
                        if (logoStream == null) {
                            throw new IOException("Could not find logo: " + logoPath);
                        }
                        BufferedImage logo = ImageIO.read(logoStream);
                        
                        // Calculate dimensions to maintain aspect ratio and fill card
                        double logoRatio = (double) logo.getWidth() / logo.getHeight();
                        double panelRatio = (double) getWidth() / getHeight();
                        int logoWidth, logoHeight;
                        int x = 0, y = 0;
                        
                        if (logoRatio > panelRatio) {
                            logoHeight = getHeight();
                            logoWidth = (int)(logoHeight * logoRatio);
                            x = (getWidth() - logoWidth) / 2;
                        } else {
                            logoWidth = getWidth();
                            logoHeight = (int)(logoWidth / logoRatio);
                            y = (getHeight() - logoHeight) / 2;
                        }
                        
                        // Draw the logo
                        g2d.drawImage(logo, x, y, logoWidth, logoHeight, null);
                        
                    } catch (Exception e) {
                        // If logo loading fails, log error and use a colored background
                        System.err.println("Error loading logo for " + team + ": " + e.getMessage());
                        g2d.setColor(TEAM_COLORS.get(team));
                        g2d.fillRect(0, 0, getWidth(), getHeight());
                    }
                }
            };
            
            teamCard.setLayout(new BoxLayout(teamCard, BoxLayout.Y_AXIS));
            teamCard.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            teamCard.setPreferredSize(new Dimension(180, 180));
            
            // Create budget label with drop shadow
            JLabel budgetLabel = createShadowLabel(
                "₹" + NumberFormat.getInstance().format(teamBudgets.get(team)), 
                12
            );
            budgetLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            
            // Add rigid area to push labels to bottom
            teamCard.add(Box.createVerticalGlue());
            teamCard.add(Box.createVerticalStrut(5));
            teamCard.add(budgetLabel);
            teamCard.add(Box.createVerticalStrut(10));
            
            // Add hover effects
            teamCard.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    placeBid(team);
                }
                
                public void mouseEntered(MouseEvent e) {
                    teamCard.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
                }
                
                public void mouseExited(MouseEvent e) {
                    teamCard.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                }
            });
            
            teamButtonsPanel.add(teamCard);
        }
    }

// Utility method to create labels with drop shadow effect for better visibility
private JLabel createShadowLabel(String text, int fontSize) {
    JLabel label = new JLabel(text) {
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, 
                               RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            
            // Draw shadow
            g2d.setColor(new Color(0, 0, 0, 160));
            g2d.drawString(getText(), 2, getHeight() - 4);
            
            // Draw text
            g2d.setColor(Color.WHITE);
            g2d.drawString(getText(), 1, getHeight() - 5);
        }
    };
    
    label.setFont(new Font("Arial", Font.BOLD, fontSize));
    label.setForeground(Color.WHITE);
    return label;
}
    
    private boolean isDarkColor(Color color) {
        double brightness = (0.299 * color.getRed() + 
                           0.587 * color.getGreen() + 
                           0.114 * color.getBlue()) / 255;
        return brightness < 0.5;
    }
    
    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout());
        
        JButton startButton = new JButton("Start Auction");
        startButton.addActionListener(e -> startAuction());
        
        JButton pauseButton = new JButton("Pause");
        pauseButton.addActionListener(e -> togglePause());
        
        JButton nextButton = new JButton("Next Player");
        nextButton.addActionListener(e -> nextPlayer());
        
        JButton statsButton = new JButton("Show Statistics");
        statsButton.addActionListener(e -> showAuctionStatistics());
        
        panel.add(startButton);
        panel.add(pauseButton);
        panel.add(nextButton);
        panel.add(statsButton);
        
        return panel;
    }
    
    private void startAuction() {
        timeLeft = 30;
        currentBid = players.get(currentPlayerIndex).basePrice;
        highestBidder = null;
        updateUI();
        bidTimer.start();
        randomBidTimer.start();
    }
    
    private void togglePause() {
        isAuctionPaused = !isAuctionPaused;
    }
    
    private void nextPlayer() {
        currentPlayerIndex++;
        if (currentPlayerIndex >= players.size()) {
            JOptionPane.showMessageDialog(this, "Auction Complete!");
            currentPlayerIndex = 0;
        }
        startAuction();
    }
    
    private void updateTimer() {
        timerLabel.setText("Time: " + timeLeft + "s");
        timerProgressBar.setValue(timeLeft);
    }
    
    private void updateUI() {
        PlayerInfo currentPlayer = players.get(currentPlayerIndex);
        currentPlayerLabel.setText(String.format("Player: %s (%s - %s)", 
            currentPlayer.name, currentPlayer.role, currentPlayer.nationality));
        baseValueLabel.setText(String.format("Base Price: ₹%,d", currentPlayer.basePrice));
        currentBidLabel.setText(String.format("Current Bid: ₹%,d", (int)currentBid));
        bidderLabel.setText("Highest Bidder: " + (highestBidder != null ? highestBidder : "None"));
        updateTimer();
    }
    
    private void logAuctionEvent(String event) {
        auctionLog.append(event);
        auctionLog.setCaretPosition(auctionLog.getDocument().getLength());
    }
    
    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        JMenu fileMenu = new JMenu("File");
        JMenuItem saveMenuItem = new JMenuItem("Save Auction");
        JMenuItem loadMenuItem = new JMenuItem("Load Auction");
        JMenuItem exitMenuItem = new JMenuItem("Exit");
        
        saveMenuItem.addActionListener(e -> saveAuctionState());
        loadMenuItem.addActionListener(e -> loadAuctionState());
        exitMenuItem.addActionListener(e -> System.exit(0));
        
        fileMenu.add(saveMenuItem);
        fileMenu.add(loadMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(exitMenuItem);
        
        menuBar.add(fileMenu);
        
        return menuBar;
    }
    
// ... [Previous code remains the same until showAuctionStatistics()] ...

    private void showAuctionStatistics() {
        StringBuilder stats = new StringBuilder("Auction Statistics\n\n");
        
        int totalSpent = 0;
        for (String team : teams) {
            int spent = TEAM_BUDGET - teamBudgets.get(team);
            totalSpent += spent;
            
            List<PlayerInfo> teamPlayerList = teamPlayers.get(team);
            stats.append(String.format("%s:\n", team));
            stats.append(String.format("Budget Remaining: ₹%,d\n", teamBudgets.get(team)));
            stats.append(String.format("Players Bought: %d\n", teamPlayerList.size()));
            
            // Show player details
            if (!teamPlayerList.isEmpty()) {
                stats.append("Players:\n");
                for (PlayerInfo player : teamPlayerList) {
                    stats.append(String.format("- %s (%s) - ₹%,d\n", 
                        player.name, player.role, player.finalPrice));
                }
            }
            stats.append("\n");
        }
        
        stats.append(String.format("\nTotal Amount Spent: ₹%,d\n", totalSpent));
        if (!soldPlayers.isEmpty()) {
            stats.append(String.format("Average Player Cost: ₹%,d\n", 
                totalSpent / soldPlayers.size()));
        }
        
        JTextArea textArea = new JTextArea(stats.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 400));
        
        JOptionPane.showMessageDialog(this, scrollPane, 
            "Auction Statistics", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void saveAuctionState() {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream("auction_state.dat"))) {
            
            // Create a state object with all necessary data
            AuctionState state = new AuctionState(
                teamBudgets,
                teamPlayers,
                players,
                soldPlayers,
                currentPlayerIndex,
                currentBid,
                highestBidder,
                timeLeft,
                isAuctionPaused
            );
            
            oos.writeObject(state);
            JOptionPane.showMessageDialog(this, 
                "Auction state saved successfully!", 
                "Save Success", 
                JOptionPane.INFORMATION_MESSAGE);
                
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, 
                "Error saving auction state: " + e.getMessage(),
                "Save Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void loadAuctionState() {
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream("auction_state.dat"))) {
                
            AuctionState state = (AuctionState) ois.readObject();
            
            // Restore the state
            teamBudgets = state.teamBudgets;
            teamPlayers = state.teamPlayers;
            players = state.players;
            soldPlayers = state.soldPlayers;
            currentPlayerIndex = state.currentPlayerIndex;
            currentBid = state.currentBid;
            highestBidder = state.highestBidder;
            timeLeft = state.timeLeft;
            isAuctionPaused = state.isAuctionPaused;
            
            updateUI();
            JOptionPane.showMessageDialog(this, 
                "Auction state loaded successfully!",
                "Load Success", 
                JOptionPane.INFORMATION_MESSAGE);
                
        } catch (IOException | ClassNotFoundException e) {
            JOptionPane.showMessageDialog(this, 
                "Error loading auction state: " + e.getMessage(),
                "Load Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Serializable class to save/load auction state
    private static class AuctionState implements Serializable {
        private static final long serialVersionUID = 1L;
        
        Map<String, Integer> teamBudgets;
        Map<String, List<PlayerInfo>> teamPlayers;
        List<PlayerInfo> players;
        Set<String> soldPlayers;
        int currentPlayerIndex;
        double currentBid;
        String highestBidder;
        int timeLeft;
        boolean isAuctionPaused;
        
        AuctionState(
            Map<String, Integer> teamBudgets,
            Map<String, List<PlayerInfo>> teamPlayers,
            List<PlayerInfo> players,
            Set<String> soldPlayers,
            int currentPlayerIndex,
            double currentBid,
            String highestBidder,
            int timeLeft,
            boolean isAuctionPaused) {
            
            this.teamBudgets = teamBudgets;
            this.teamPlayers = teamPlayers;
            this.players = players;
            this.soldPlayers = soldPlayers;
            this.currentPlayerIndex = currentPlayerIndex;
            this.currentBid = currentBid;
            this.highestBidder = highestBidder;
            this.timeLeft = timeLeft;
            this.isAuctionPaused = isAuctionPaused;
        }
    }
    
    public static void main(String[] args) {
        try {
            // Set system look and feel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> {
            IPLAuctionSystem auction = new IPLAuctionSystem();
            auction.setVisible(true);
        });
    }
}

// TeamLogos class moved to its own file
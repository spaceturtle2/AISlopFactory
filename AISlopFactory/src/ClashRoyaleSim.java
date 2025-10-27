import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A simple GUI-based simulation of a Clash Royale-like game using Swing.
 * The game features real-time unit movement, Elixir management, and tower destruction.
 */
public class ClashRoyaleSim extends JFrame {

    // --- Core Game Entities (Nested Classes) ---

    /** Represents a unit or card blueprint. */
    static class Card {
        private final String name;
        private final int cost; // Elixir cost
        private final int baseHealth;
        private final int damage;
        private final Color color;

        public Card(String name, int cost, int baseHealth, int damage, Color color) {
            this.name = name;
            this.cost = cost;
            this.baseHealth = baseHealth;
            this.damage = damage;
            this.color = color;
        }

        public String getName() { return name; }
        public int getCost() { return cost; }
        public int getBaseHealth() { return baseHealth; }
        public int getDamage() { return damage; }
        public Color getColor() { return color; }
    }

    /** Represents a unit actively deployed on the battlefield. */
    static class ActiveUnit extends Card {
        private int currentHealth;
        private int x, y; // Position on the board
        private final boolean isPlayerUnit;
        private final int size = 30; // Visual size of the unit

        public ActiveUnit(Card card, boolean isPlayerUnit, int startX, int startY) {
            super(card.getName(), card.getCost(), card.getBaseHealth(), card.getDamage(), card.getColor());
            this.currentHealth = card.getBaseHealth();
            this.isPlayerUnit = isPlayerUnit;
            this.x = startX;
            this.y = startY;
        }

        public int getCurrentHealth() { return currentHealth; }
        public boolean isAlive() { return currentHealth > 0; }
        public boolean isPlayerUnit() { return isPlayerUnit; }
        public int getX() { return x; }
        public int getY() { return y; }
        public int getSize() { return size; }

        public void takeDamage(int damage) {
            this.currentHealth -= damage;
            if (this.currentHealth < 0) this.currentHealth = 0;
        }

        /** Moves the unit one step towards the enemy tower. */
        public void move() {
            int speed = 2; // Movement speed
            if (isPlayerUnit) {
                // Player moves left (towards AI tower)
                x -= speed;
            } else {
                // AI moves right (towards Player tower)
                x += speed;
            }
        }

        /** Checks for collision with another unit. */
        public boolean collidesWith(ActiveUnit other) {
            int dx = Math.abs(this.x - other.x);
            int dy = Math.abs(this.y - other.y);
            return dx < (this.size) && dy < (this.size);
        }
    }

    /** Represents the objective, the King's Tower. */
    static class Tower {
        private final String name;
        private int health;
        private final int x, y; // Position
        private final int width = 50;
        private final int height = 100;
        private final Color color;

        public Tower(String name, int health, int x, int y, Color color) {
            this.name = name;
            this.health = health;
            this.x = x;
            this.y = y;
            this.color = color;
        }

        public int getHealth() { return health; }
        public int getX() { return x; }
        public int getY() { return y; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public Color getColor() { return color; }

        /** Applies damage to the tower. */
        public void takeDamage(int damage) {
            this.health -= damage;
            if (this.health < 0) {
                this.health = 0;
            }
        }
    }

    // --- Game Setup and Variables ---

    private static final int GAME_WIDTH = 800;
    private static final int GAME_HEIGHT = 500;
    private static final int MAX_ELIXIR = 10;
    private static final int STARTING_TOWER_HEALTH = 5000;
    private static final int AI_DEPLOY_INTERVAL = 100; // Deploy AI card every 100 game ticks

    // Card definitions for the game
    private static final Card[] ALL_CARDS = {
        new Card("Knight", 3, 600, 100, new Color(0, 128, 255)), // Blue
        new Card("Archers", 4, 300, 150, new Color(255, 200, 0)), // Gold
        new Card("Mini P.E.K.K.A", 4, 500, 350, new Color(128, 0, 128)), // Purple
        new Card("Goblins", 2, 200, 50, new Color(0, 192, 0)), // Green
        new Card("Giant", 5, 1200, 80, new Color(255, 100, 0)) // Orange
    };

    private int playerElixir = 5;
    private int aiElixir = 5;
    private Tower playerTower;
    private Tower aiTower;
    private List<ActiveUnit> deployedUnits = new ArrayList<>();
    private JLabel elixirLabel;
    private GamePanel gamePanel;
    private final Random random = new Random();
    private Timer gameTimer;
    private int gameTick = 0;
    private boolean gameOver = false;

    // --- Constructor and Initialization ---

    public ClashRoyaleSim() {
        setTitle("Simple Clash Royale GUI");
        setSize(GAME_WIDTH, GAME_HEIGHT + 150); // Add space for controls
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        // Initialize Towers
        // Player Tower (Right Side)
        int towerY = (GAME_HEIGHT / 2) - (new Tower("", 0, 0, 0, Color.BLACK).getHeight() / 2);
        playerTower = new Tower("Your Tower", STARTING_TOWER_HEALTH, GAME_WIDTH - 70, towerY, new Color(0, 100, 200));
        // AI Tower (Left Side)
        aiTower = new Tower("AI Tower", STARTING_TOWER_HEALTH, 20, towerY, new Color(200, 50, 0));

        // Setup Layout
        setLayout(new BorderLayout());

        // 1. Game Panel (Center)
        gamePanel = new GamePanel();
        add(gamePanel, BorderLayout.CENTER);

        // 2. Control Panel (Bottom)
        JPanel controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.SOUTH);

        // Start Elixir Regeneration and Game Loop
        Timer elixirRegenTimer = new Timer(1000, new ElixirRegenListener());
        elixirRegenTimer.start();

        gameTimer = new Timer(50, new GameLoopListener()); // 50ms tick rate (20 FPS)
        gameTimer.start();

        setLocationRelativeTo(null); // Center the window
    }

    // --- GUI Components ---

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(GAME_WIDTH, 100));
        panel.setBackground(new Color(40, 40, 40));

        // Elixir Display
        elixirLabel = new JLabel("Elixir: " + playerElixir + " / 10", SwingConstants.CENTER);
        elixirLabel.setFont(new Font("Arial", Font.BOLD, 24));
        elixirLabel.setForeground(Color.YELLOW);
        panel.add(elixirLabel, BorderLayout.NORTH);

        // Card Buttons
        JPanel cardButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        cardButtonPanel.setBackground(new Color(60, 60, 60));

        for (int i = 0; i < ALL_CARDS.length; i++) {
            Card card = ALL_CARDS[i];
            JButton btn = new JButton(
                String.format("%s (%d Elixir)", card.getName(), card.getCost())
            );
            btn.setFont(new Font("Arial", Font.PLAIN, 12));
            btn.setBackground(card.getColor());
            btn.setForeground(Color.WHITE);
            btn.setFocusPainted(false);
            btn.addActionListener(new CardDeployListener(card));
            cardButtonPanel.add(btn);
        }
        panel.add(cardButtonPanel, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Custom JPanel for drawing the game state.
     */
    class GamePanel extends JPanel {
        public GamePanel() {
            setPreferredSize(new Dimension(GAME_WIDTH, GAME_HEIGHT));
            setBackground(new Color(150, 200, 150)); // Green field
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw Towers
            drawTower(g2d, aiTower);
            drawTower(g2d, playerTower);

            // Draw Units
            for (ActiveUnit unit : deployedUnits) {
                drawUnit(g2d, unit);
            }

            // Draw Info Overlay
            if (gameOver) {
                drawGameOver(g2d);
            }
        }

        /** Draws a tower with its current health. */
        private void drawTower(Graphics2D g2d, Tower tower) {
            // Draw Tower Structure
            g2d.setColor(tower.getColor());
            g2d.fillRect(tower.getX(), tower.getY(), tower.getWidth(), tower.getHeight());

            // Draw Health Bar
            int maxHealth = STARTING_TOWER_HEALTH;
            double healthRatio = (double) tower.getHealth() / maxHealth;
            int healthBarHeight = 10;
            int barWidth = tower.getWidth();

            g2d.setColor(Color.RED);
            g2d.fillRect(tower.getX(), tower.getY() - healthBarHeight - 5, barWidth, healthBarHeight);
            g2d.setColor(Color.GREEN);
            g2d.fillRect(tower.getX(), tower.getY() - healthBarHeight - 5, (int)(barWidth * healthRatio), healthBarHeight);

            // Draw Text
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            String healthText = String.valueOf(tower.getHealth());
            int textX = tower.getX() + (tower.getWidth() / 2) - g2d.getFontMetrics().stringWidth(healthText) / 2;
            g2d.drawString(healthText, textX, tower.getY() + tower.getHeight() + 15);
        }

        /** Draws an active unit (a colored square). */
        private void drawUnit(Graphics2D g2d, ActiveUnit unit) {
            // Draw Unit Shape (Square)
            g2d.setColor(unit.getColor());
            g2d.fillRoundRect(unit.getX(), unit.getY(), unit.getSize(), unit.getSize(), 8, 8); // Rounded rectangle

            // Draw Health Bar
            double healthRatio = (double) unit.getCurrentHealth() / unit.getBaseHealth();
            int healthBarWidth = unit.getSize();
            int healthBarHeight = 5;

            g2d.setColor(Color.RED);
            g2d.fillRect(unit.getX(), unit.getY() - healthBarHeight - 2, healthBarWidth, healthBarHeight);
            g2d.setColor(Color.GREEN);
            g2d.fillRect(unit.getX(), unit.getY() - healthBarHeight - 2, (int)(healthBarWidth * healthRatio), healthBarHeight);

            // Draw Border to distinguish players
            g2d.setColor(unit.isPlayerUnit() ? Color.WHITE : Color.BLACK);
            g2d.drawRoundRect(unit.getX(), unit.getY(), unit.getSize(), unit.getSize(), 8, 8);

            // Draw Unit Name initial (optional)
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            String initial = String.valueOf(unit.getName().charAt(0));
            int textX = unit.getX() + (unit.getSize() / 2) - g2d.getFontMetrics().stringWidth(initial) / 2;
            g2d.drawString(initial, textX, unit.getY() + 20);
        }

        private void drawGameOver(Graphics2D g2d) {
            String message;
            if (playerTower.getHealth() <= 0) {
                message = "DEFEAT! AI Wins!";
                g2d.setColor(new Color(200, 0, 0, 200));
            } else if (aiTower.getHealth() <= 0) {
                message = "VICTORY! You Win!";
                g2d.setColor(new Color(0, 150, 0, 200));
            } else {
                message = "GAME OVER!";
                g2d.setColor(new Color(100, 100, 100, 200));
            }

            g2d.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 48));
            FontMetrics fm = g2d.getFontMetrics();
            int x = (GAME_WIDTH - fm.stringWidth(message)) / 2;
            int y = (GAME_HEIGHT / 2) + (fm.getAscent() / 2);
            g2d.drawString(message, x, y);
            gameTimer.stop();
        }
    }

    // --- Listeners and Game Loop ---

    /** Handles player card deployment. */
    class CardDeployListener implements ActionListener {
        private final Card card;

        public CardDeployListener(Card card) {
            this.card = card;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (gameOver) return;

            if (playerElixir >= card.getCost()) {
                playerElixir -= card.getCost();
                // Deploy player unit near the player's tower (Right side)
                int startX = playerTower.getX() - 50;
                // Random Y position in the middle lane for variety
                int startY = random.nextInt(GAME_HEIGHT - 60) + 30;
                ActiveUnit newUnit = new ActiveUnit(card, true, startX, startY);
                deployedUnits.add(newUnit);
                elixirLabel.setText("Elixir: " + playerElixir + " / 10");
            }
        }
    }

    /** Regenerates Elixir every second. */
    class ElixirRegenListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (gameOver) return;
            playerElixir = Math.min(MAX_ELIXIR, playerElixir + 1);
            aiElixir = Math.min(MAX_ELIXIR, aiElixir + 1);
            elixirLabel.setText("Elixir: " + playerElixir + " / 10");
            gamePanel.repaint();
        }
    }

    /** The main game loop, handling movement and combat. */
    class GameLoopListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (gameOver) return;

            gameTick++;

            // AI Deployment (Simple Strategy: Deploy a card every X ticks if affordable)
            if (gameTick % AI_DEPLOY_INTERVAL == 0 && aiElixir > 2) {
                aiDeployCard();
            }

            // 1. Move Units, Check Tower Damage, and Check Collisions
            for (int i = 0; i < deployedUnits.size(); i++) {
                ActiveUnit unit = deployedUnits.get(i);
                unit.move();

                // Check for Tower Hit
                if (unit.isPlayerUnit() && unit.getX() <= aiTower.getX() + aiTower.getWidth()) {
                    aiTower.takeDamage(unit.getDamage() / 10); // Less damage to tower per tick
                    deployedUnits.remove(i);
                    i--; // Adjust index
                    continue;
                } else if (!unit.isPlayerUnit() && unit.getX() + unit.getSize() >= playerTower.getX()) {
                    playerTower.takeDamage(unit.getDamage() / 10);
                    deployedUnits.remove(i);
                    i--;
                    continue;
                }

                // Check for Combat with other units
                for (int j = i + 1; j < deployedUnits.size(); j++) {
                    ActiveUnit other = deployedUnits.get(j);

                    // Only fight enemy units
                    if (unit.isPlayerUnit() != other.isPlayerUnit() && unit.collidesWith(other)) {
                        // Combat resolution: Stop movement and trade damage (simplified to happen instantly)
                        unit.takeDamage(other.getDamage() / 5); // Damage over time
                        other.takeDamage(unit.getDamage() / 5); // Damage over time

                        // Stop units in place while fighting
                        unit.x -= unit.isPlayerUnit() ? 2 : -2;
                        other.x -= other.isPlayerUnit() ? 2 : -2;

                        if (!unit.isAlive()) {
                            deployedUnits.remove(i);
                            i--;
                            break;
                        }
                        if (!other.isAlive()) {
                            deployedUnits.remove(j);
                            j--;
                        }
                    }
                }
            }

            // 2. Remove dead units (handled above but an extra cleanup loop is safer)
            deployedUnits.removeIf(unit -> !unit.isAlive());

            // 3. Check Game Over
            if (playerTower.getHealth() <= 0 || aiTower.getHealth() <= 0) {
                gameOver = true;
            }

            // 4. Redraw the board
            gamePanel.repaint();
        }
    }

    /** Simple AI logic: Deploys the most expensive affordable card in a random lane. */
    private void aiDeployCard() {
        List<Card> affordableCards = new ArrayList<>();
        for (Card card : ALL_CARDS) {
            if (card.getCost() <= aiElixir) {
                affordableCards.add(card);
            }
        }

        if (affordableCards.isEmpty()) {
            return;
        }

        // Deploy the most expensive affordable card
        Card bestCard = affordableCards.get(0);
        for (Card card : affordableCards) {
            if (card.getCost() > bestCard.getCost()) {
                bestCard = card;
            }
        }

        aiElixir -= bestCard.getCost();
        // Deploy AI unit near AI tower (Left side)
        int startX = aiTower.getX() + aiTower.getWidth() + 20;
        int startY = random.nextInt(GAME_HEIGHT - 60) + 30; // Random Y
        ActiveUnit newUnit = new ActiveUnit(bestCard, false, startX, startY);
        deployedUnits.add(newUnit);
    }

    // --- Entry Point ---
    public static void main(String[] args) {
        // Ensure the GUI runs on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            ClashRoyaleSim game = new ClashRoyaleSim();
            game.setVisible(true);
        });
    }
}

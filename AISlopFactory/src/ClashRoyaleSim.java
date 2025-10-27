import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A simple GUI-based simulation of a Clash Royale-like game using Swing.
 * The game features real-time unit movement, Elixir management, tower destruction,
 * and specific targeting rules (like ranged attacks and tower-only targeting).
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
        private final int range;
        private final int attackSpeedTicks; // Ticks between attacks (e.g., 20 ticks = 1 second)
        private final boolean onlyTargetsTowers; // Property for Giant

        public Card(String name, int cost, int baseHealth, int damage, Color color, int range, int attackSpeedTicks, boolean onlyTargetsTowers) {
            this.name = name;
            this.cost = cost;
            this.baseHealth = baseHealth;
            this.damage = damage;
            this.color = color;
            this.range = range;
            this.attackSpeedTicks = attackSpeedTicks;
            this.onlyTargetsTowers = onlyTargetsTowers;
        }

        public String getName() { return name; }
        public int getCost() { return cost; }
        public int getBaseHealth() { return baseHealth; }
        public int getDamage() { return damage; }
        public Color getColor() { return color; }
        public int getRange() { return range; }
        public int getAttackSpeedTicks() { return attackSpeedTicks; }
        public boolean onlyTargetsTowers() { return onlyTargetsTowers; }
    }

    /** Represents a unit actively deployed on the battlefield. */
    static class ActiveUnit extends Card {
        private int currentHealth;
        private int x, y; // Position on the board
        private final boolean isPlayerUnit;
        private final int size = 30; // Visual size of the unit
        private Object target; // Can be ActiveUnit or Tower
        private int attackCooldown = 0; // Ticks until next attack

        public ActiveUnit(Card card, boolean isPlayerUnit, int startX, int startY) {
            // Pass new card properties to the Card constructor
            super(card.getName(), card.getCost(), card.getBaseHealth(), card.getDamage(), card.getColor(),
                  card.getRange(), card.getAttackSpeedTicks(), card.onlyTargetsTowers());
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
        public Object getTarget() { return target; }
        
        public void setTarget(Object target) { this.target = target; }
        public void decreaseCooldown() { if (attackCooldown > 0) attackCooldown--; }
        public void resetCooldown() { attackCooldown = getAttackSpeedTicks(); }
        public boolean canAttack() { return attackCooldown <= 0; }

        public void takeDamage(int damage) {
            this.currentHealth -= damage;
            if (this.currentHealth < 0) this.currentHealth = 0;
        }

        /** * Moves the unit one step towards a specific target coordinate (X, Y). 
         * This uses vector math for diagonal movement.
         */
        public void moveTowards(int targetX, int targetY) {
            int speed = 2; // Movement speed
            int unitCenterX = this.x + this.size / 2;
            int unitCenterY = this.y + this.size / 2;

            // Calculate direction vectors (normalized)
            double dx = targetX - unitCenterX;
            double dy = targetY - unitCenterY;
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance > speed) { // Only move if the target is further than one step
                // Normalize and apply speed
                this.x += (int) (speed * (dx / distance));
                this.y += (int) (speed * (dy / distance));
            } else {
                // Snap to target if very close (prevents tiny jitters near melee range)
                this.x = targetX - this.size / 2;
                this.y = targetY - this.size / 2;
            }
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
    private static final int AI_DEPLOY_INTERVAL = 100; // Deploy AI card every 100 game ticks (5 seconds)

    // Card definitions for the game (Name, Cost, HP, Damage, Color, Range, AttackSpeedTicks, OnlyTargetsTowers)
    // Range: 35 is Melee, 150 is Ranged (Archers)
    // AttackSpeedTicks: 20 ticks = 1 second
    private static final Card[] ALL_CARDS = {
        new Card("Knight", 3, 600, 100, new Color(0, 128, 255), 35, 25, false), // Melee, standard target
        new Card("Archers", 4, 300, 150, new Color(255, 200, 0), 150, 30, false), // Ranged, standard target
        new Card("Mini P.E.K.K.A", 4, 500, 350, new Color(128, 0, 128), 35, 40, false), // Melee, heavy hitter
        new Card("Goblins", 2, 200, 50, new Color(0, 192, 0), 35, 20, false), // Melee, fastest attack
        new Card("Giant", 5, 1200, 80, new Color(255, 100, 0), 35, 50, true) // TOWER ONLY!
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
        setSize(GAME_WIDTH, GAME_HEIGHT + 150);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        // Initialize Towers
        int towerY = (GAME_HEIGHT / 2) - (new Tower("", 0, 0, 0, Color.BLACK).getHeight() / 2);
        playerTower = new Tower("Your Tower", STARTING_TOWER_HEALTH, GAME_WIDTH - 70, towerY, new Color(0, 100, 200));
        aiTower = new Tower("AI Tower", STARTING_TOWER_HEALTH, 20, towerY, new Color(200, 50, 0));

        // Setup Layout
        setLayout(new BorderLayout());

        // 1. Game Panel (Center)
        gamePanel = new GamePanel();
        add(gamePanel, BorderLayout.CENTER);

        // 2. Control Panel (Bottom)
        JPanel controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.SOUTH);

        // Start Elixir Regeneration (1 second tick)
        Timer elixirRegenTimer = new Timer(1000, new ElixirRegenListener());
        elixirRegenTimer.start();

        // Start Game Loop (50ms tick rate = 20 FPS)
        gameTimer = new Timer(50, new GameLoopListener());
        gameTimer.start();

        setLocationRelativeTo(null); // Center the window
    }

    // --- GUI Components ---

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(GAME_WIDTH, 100));
        panel.setBackground(new Color(40, 40, 40));

        // Elixir Display
        elixirLabel = new JLabel("Elixir: " + playerElixir + " / " + MAX_ELIXIR, SwingConstants.CENTER);
        elixirLabel.setFont(new Font("Arial", Font.BOLD, 24));
        elixirLabel.setForeground(Color.YELLOW);
        panel.add(elixirLabel, BorderLayout.NORTH);

        // Card Buttons
        JPanel cardButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        cardButtonPanel.setBackground(new Color(60, 60, 60));

        for (int i = 0; i < ALL_CARDS.length; i++) {
            Card card = ALL_CARDS[i];
            JButton btn = new JButton(
                String.format("%s (%d E)", card.getName(), card.getCost())
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

    /** Custom JPanel for drawing the game state. */
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

        private void drawUnit(Graphics2D g2d, ActiveUnit unit) {
            // Draw Unit Shape (Square)
            g2d.setColor(unit.getColor());
            g2d.fillRoundRect(unit.getX(), unit.getY(), unit.getSize(), unit.getSize(), 8, 8);

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

    // --- Utility Methods for Targeting and Combat ---

    /** Calculates the distance between the center of a unit and a target object (unit or tower). */
    private double getDistance(ActiveUnit unit, Object target) {
        int targetX, targetY;
        
        if (target instanceof ActiveUnit) {
            ActiveUnit t = (ActiveUnit) target;
            targetX = t.getX() + t.getSize() / 2;
            targetY = t.getY() + t.getSize() / 2;
        } else if (target instanceof Tower) {
            Tower t = (Tower) target;
            targetX = t.getX() + t.getWidth() / 2;
            targetY = t.getY() + t.getHeight() / 2;
        } else {
            return Double.MAX_VALUE;
        }

        int unitCenterX = unit.getX() + unit.getSize() / 2;
        int unitCenterY = unit.getY() + unit.getSize() / 2;

        return Math.sqrt(Math.pow(unitCenterX - targetX, 2) + Math.pow(unitCenterY - targetY, 2));
    }
    
    /** Checks if the unit is within attack range of the target object. */
    private boolean isWithinRange(ActiveUnit unit, Object target) {
        return getDistance(unit, target) <= unit.getRange();
    }

    /** Finds the best target (closest) for a given unit based on its targeting preference. */
    private Object findTarget(ActiveUnit currentUnit) {
        Tower enemyTower = currentUnit.isPlayerUnit() ? aiTower : playerTower;

        // 1. If unit only targets towers (like the Giant), return the enemy tower.
        if (currentUnit.onlyTargetsTowers() && enemyTower.getHealth() > 0) {
            return enemyTower;
        }

        // 2. Otherwise (standard troops), find the closest enemy unit first.
        ActiveUnit closestUnit = null;
        double minDistance = Double.MAX_VALUE;

        for (ActiveUnit enemy : deployedUnits) {
            if (enemy.isPlayerUnit() != currentUnit.isPlayerUnit() && enemy.isAlive()) {
                double distance = getDistance(currentUnit, enemy);
                if (distance < minDistance) {
                    minDistance = distance;
                    closestUnit = enemy;
                }
            }
        }
        
        // If an enemy unit is found, target it
        if (closestUnit != null) {
            return closestUnit;
        }

        // 3. If no enemy units, target the Tower.
        if (enemyTower.getHealth() > 0) {
            return enemyTower;
        }

        return null; // No target available
    }

    /** Executes the attack on the unit's target. */
    private void attackTarget(ActiveUnit attacker) {
        Object target = attacker.getTarget();
        
        if (target == null) return;

        if (target instanceof ActiveUnit) {
            ActiveUnit targetUnit = (ActiveUnit) target;
            targetUnit.takeDamage(attacker.getDamage());
            
            if (!targetUnit.isAlive()) {
                attacker.setTarget(null); // Force retarget next tick
            }
            
        } else if (target instanceof Tower) {
            Tower targetTower = (Tower) target;
            // Tower damage is sustained damage over time
            targetTower.takeDamage(attacker.getDamage() / 5); 

            if (targetTower.getHealth() <= 0) {
                gameOver = true;
            }
        }
    }


    // --- Listeners and Game Loop ---

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
                elixirLabel.setText("Elixir: " + playerElixir + " / " + MAX_ELIXIR);
            }
        }
    }

    class ElixirRegenListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (gameOver) return;
            playerElixir = Math.min(MAX_ELIXIR, playerElixir + 1);
            aiElixir = Math.min(MAX_ELIXIR, aiElixir + 1);
            elixirLabel.setText("Elixir: " + playerElixir + " / " + MAX_ELIXIR);
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
            if (gameTick % AI_DEPLOY_INTERVAL == 0 && aiElixir >= 3) {
                aiDeployCard();
            }

            // 1. Process all units: targeting, movement, and attacking
            for (ActiveUnit unit : deployedUnits) {
                unit.decreaseCooldown();

                // A. Target Acquisition/Validation
                // If the unit has no target, or its target is dead/destroyed, find a new one.
                if (unit.getTarget() == null || (unit.getTarget() instanceof ActiveUnit && !((ActiveUnit) unit.getTarget()).isAlive())) {
                    unit.setTarget(findTarget(unit));
                }

                if (unit.getTarget() != null) {
                    // B. Calculate Target Center Coordinates
                    int targetCenterX, targetCenterY;
                    if (unit.getTarget() instanceof ActiveUnit) {
                        ActiveUnit targetUnit = (ActiveUnit) unit.getTarget();
                        targetCenterX = targetUnit.getX() + targetUnit.getSize() / 2;
                        targetCenterY = targetUnit.getY() + targetUnit.getSize() / 2;
                    } else { // Must be a Tower
                        Tower targetTower = (Tower) unit.getTarget();
                        targetCenterX = targetTower.getX() + targetTower.getWidth() / 2;
                        targetCenterY = targetTower.getY() + targetTower.getHeight() / 2;
                    }


                    // C. Movement and Attack
                    if (isWithinRange(unit, unit.getTarget())) {
                        // Stop movement and attack
                        if (unit.canAttack()) {
                            attackTarget(unit);
                            unit.resetCooldown();
                        }
                    } else {
                        // Move towards the target's center point
                        unit.moveTowards(targetCenterX, targetCenterY);
                    }
                }
            }

            // 2. Remove dead units
            deployedUnits.removeIf(unit -> !unit.isAlive());

            // 3. Check Game Over
            if (playerTower.getHealth() <= 0 || aiTower.getHealth() <= 0) {
                gameOver = true;
            }

            // 4. Redraw the board
            gamePanel.repaint();
        }
    }

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
        SwingUtilities.invokeLater(() -> {
            ClashRoyaleSim game = new ClashRoyaleSim();
            game.setVisible(true);
        });
    }
}

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Random;

public class AimTrainer extends JPanel implements ActionListener, MouseListener, MouseMotionListener {
    private static final int WIDTH = 1200;
    private static final int HEIGHT = 800;
    private static final Color BACKGROUND = new Color(18, 18, 24);
    private static final Color TARGET_COLOR = new Color(255, 65, 54);
    private static final Color TARGET_HOVER = new Color(255, 100, 90);
    private static final Color TARGET_HIT = new Color(255, 200, 50);
    private static final Color TEXT_COLOR = new Color(240, 240, 240);
    private static final Color STATS_COLOR = new Color(100, 200, 255);
    
    private ArrayList<Target> targets;
    private Random random;
    private Timer timer;
    private Timer gameTimer;
    
    // Game stats
    private int hits;
    private int misses;
    private int totalShots;
    private long startTime;
    private double accuracy;
    private int score;
    private int combo;
    private int maxCombo;
    private double averageTimeBetweenShots;
    private long lastShotTime;
    private double totalTimeBetweenShots;
    private int timedShots;
    private boolean gameRunning;
    private int gameDuration = 60; // 60 seconds
    private int timeRemaining;
    
    // Crosshair and Recoil
    private Point crosshair;
    private Point originalCrosshair;
    private boolean showCrosshair = true;
    private double recoilOffsetX;
    private double recoilOffsetY;
    private double recoilRecovery = 0.9;
    private boolean isRecoiling = false;
    
    // Multi-shot targets
    private boolean multiShotMode = true; // Set to false for 1-shot kills
    
    public AimTrainer() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(BACKGROUND);
        
        targets = new ArrayList<>();
        random = new Random();
        crosshair = new Point(WIDTH / 2, HEIGHT / 2);
        originalCrosshair = new Point(WIDTH / 2, HEIGHT / 2);
        
        addMouseListener(this);
        addMouseMotionListener(this);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    showCrosshair = !showCrosshair;
                    repaint();
                } else if (e.getKeyCode() == KeyEvent.VK_R) {
                    resetGame();
                } else if (e.getKeyCode() == KeyEvent.VK_M) {
                    multiShotMode = !multiShotMode;
                    resetGame();
                }
            }
        });
        
        setFocusable(true);
        requestFocus();
        
        startGame();
    }
    
    private void startGame() {
        hits = 0;
        misses = 0;
        totalShots = 0;
        score = 0;
        combo = 0;
        maxCombo = 0;
        averageTimeBetweenShots = 0;
        lastShotTime = System.currentTimeMillis();
        totalTimeBetweenShots = 0;
        timedShots = 0;
        gameRunning = true;
        timeRemaining = gameDuration;
        startTime = System.currentTimeMillis();
        targets.clear();
        
        // Reset recoil
        recoilOffsetX = 0;
        recoilOffsetY = 0;
        isRecoiling = false;
        
        // Spawn initial targets
        for (int i = 0; i < 3; i++) {
            spawnTarget();
        }
        
        timer = new Timer(16, this); // ~60 FPS
        timer.start();
        
        // Game timer for 60 seconds
        gameTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (gameRunning) {
                    timeRemaining--;
                    if (timeRemaining <= 0) {
                        endGame();
                    }
                }
            }
        });
        gameTimer.start();
    }
    
    private void endGame() {
        gameRunning = false;
        timer.stop();
        gameTimer.stop();
        JOptionPane.showMessageDialog(this, 
            "Game Over!\n" +
            "Final Score: " + score + "\n" +
            "Accuracy: " + String.format("%.1f%%", accuracy) + "\n" +
            "Hits: " + hits + "\n" +
            "Max Combo: " + maxCombo + "\n" +
            "Avg Time Between Shots: " + String.format("%.3fs", averageTimeBetweenShots) + "\n\n" +
            "Press R to play again!",
            "Game Over", 
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void resetGame() {
        if (timer != null) timer.stop();
        if (gameTimer != null) gameTimer.stop();
        startGame();
    }
    
    private void spawnTarget() {
        int size = random.nextInt(30) + 20; // 20-50 pixels
        int x = random.nextInt(WIDTH - size * 2) + size;
        int y = random.nextInt(HEIGHT - size * 2) + size;
        
        // Avoid spawning too close to existing targets
        boolean validPosition = true;
        for (Target existing : targets) {
            double distance = Math.sqrt(Math.pow(x - existing.x, 2) + Math.pow(y - existing.y, 2));
            if (distance < size + existing.size + 20) {
                validPosition = false;
                break;
            }
        }
        
        if (validPosition) {
            targets.add(new Target(x, y, size));
        } else {
            // Try again with recursive call (with safety limit)
            if (targets.size() < 10) {
                spawnTarget();
            }
        }
    }
    
    private void applyRecoil() {
        // Random recoil direction with upward bias (like real guns)
        double angle = random.nextDouble() * Math.PI - (Math.PI / 2); // Mostly upward
        double strength = random.nextDouble() * 8 + 4; // 4-12 pixels
        
        recoilOffsetX = Math.cos(angle) * strength;
        recoilOffsetY = Math.sin(angle) * strength;
        isRecoiling = true;
        
        // Update crosshair position with recoil
        crosshair.x = originalCrosshair.x + (int)recoilOffsetX;
        crosshair.y = originalCrosshair.y + (int)recoilOffsetY;
    }
    
    private void updateRecoil() {
        if (isRecoiling) {
            // Gradually recover from recoil
            recoilOffsetX *= recoilRecovery;
            recoilOffsetY *= recoilRecovery;
            
            crosshair.x = originalCrosshair.x + (int)recoilOffsetX;
            crosshair.y = originalCrosshair.y + (int)recoilOffsetY;
            
            // Stop recoiling when offset is very small
            if (Math.abs(recoilOffsetX) < 0.5 && Math.abs(recoilOffsetY) < 0.5) {
                isRecoiling = false;
                recoilOffsetX = 0;
                recoilOffsetY = 0;
                crosshair.x = originalCrosshair.x;
                crosshair.y = originalCrosshair.y;
            }
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw targets
        for (Target target : targets) {
            target.draw(g2d);
        }
        
        // Draw crosshair
        if (showCrosshair) {
            drawCrosshair(g2d);
        }
        
        // Draw UI
        drawUI(g2d);
    }
    
    private void drawCrosshair(Graphics2D g2d) {
        int size = 15;
        
        // Draw recoil line showing original position
        if (isRecoiling) {
            g2d.setColor(new Color(255, 100, 100, 100));
            g2d.setStroke(new BasicStroke(1f));
            g2d.drawLine(originalCrosshair.x, originalCrosshair.y, crosshair.x, crosshair.y);
        }
        
        // Outer cross
        g2d.setColor(new Color(255, 255, 255, 180));
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawLine(crosshair.x - size - 2, crosshair.y, crosshair.x + size + 2, crosshair.y);
        g2d.drawLine(crosshair.x, crosshair.y - size - 2, crosshair.x, crosshair.y + size + 2);
        
        // Inner dot with recoil color effect
        if (isRecoiling) {
            g2d.setColor(new Color(255, 100, 100, 200));
        } else {
            g2d.setColor(new Color(255, 50, 50, 200));
        }
        g2d.fillOval(crosshair.x - 1, crosshair.y - 1, 3, 3);
        
        // Inner cross (thin)
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(1f));
        g2d.drawLine(crosshair.x - size, crosshair.y, crosshair.x + size, crosshair.y);
        g2d.drawLine(crosshair.x, crosshair.y - size, crosshair.x, crosshair.y + size);
    }
    
    private void drawUI(Graphics2D g2d) {
        long currentTime = System.currentTimeMillis();
        double timeElapsed = (currentTime - startTime) / 1000.0;
        
        // Main stats
        g2d.setFont(new Font("Consolas", Font.BOLD, 16));
        g2d.setColor(TEXT_COLOR);
        
        String accuracyText = String.format("Accuracy: %.1f%%", accuracy);
        String scoreText = String.format("Score: %d", score);
        String comboText = String.format("Combo: %d (Max: %d)", combo, maxCombo);
        String timeText = String.format("Time: %ds", timeRemaining);
        String hitsText = String.format("Hits: %d", hits);
        String shotsText = String.format("Shots: %d", totalShots);
        String speedText = String.format("Avg Shot Speed: %.3fs", averageTimeBetweenShots);
        String modeText = String.format("Mode: %s", multiShotMode ? "2-Shot Kill" : "1-Shot Kill");
        
        g2d.drawString(accuracyText, 20, 30);
        g2d.drawString(scoreText, 20, 55);
        g2d.drawString(comboText, 20, 80);
        g2d.drawString(timeText, 20, 105);
        g2d.drawString(hitsText, 20, 130);
        g2d.drawString(shotsText, 20, 155);
        g2d.drawString(speedText, 20, 180);
        g2d.drawString(modeText, 20, 205);
        
        // Game over indicator
        if (!gameRunning) {
            g2d.setFont(new Font("Consolas", Font.BOLD, 32));
            g2d.setColor(new Color(255, 50, 50));
            g2d.drawString("GAME OVER - PRESS R", WIDTH / 2 - 200, HEIGHT / 2);
        }
        
        // Controls help
        g2d.setColor(new Color(150, 150, 150));
        g2d.setFont(new Font("Consolas", Font.PLAIN, 12));
        g2d.drawString("SPACE: Crosshair | R: Reset | M: Toggle Mode", WIDTH - 350, HEIGHT - 20);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameRunning) return;
        
        // Update recoil
        updateRecoil();
        
        // Update targets
        for (int i = targets.size() - 1; i >= 0; i--) {
            Target target = targets.get(i);
            target.update();
            
            // Remove old targets
            if (target.lifetime <= 0) {
                targets.remove(i);
                misses++;
                combo = 0;
                spawnTarget(); // Replace removed target
            }
        }
        
        // Occasionally spawn new targets
        if (targets.size() < 5 && random.nextDouble() < 0.02) {
            spawnTarget();
        }
        
        repaint();
    }
    
    @Override
    public void mouseMoved(MouseEvent e) {
        originalCrosshair = e.getPoint();
        
        // Only update crosshair if not recoiling
        if (!isRecoiling) {
            crosshair = e.getPoint();
        }
        
        // Check for hover effects
        for (Target target : targets) {
            target.isHovered = target.contains(crosshair.x, crosshair.y);
        }
        
        repaint();
    }
    
    @Override
    public void mousePressed(MouseEvent e) {
        if (!gameRunning) return;
        
        if (e.getButton() == MouseEvent.BUTTON1) {
            long currentTime = System.currentTimeMillis();
            
            // Calculate time between shots (for stats)
            if (lastShotTime > 0) {
                double timeBetween = (currentTime - lastShotTime) / 1000.0;
                totalTimeBetweenShots += timeBetween;
                timedShots++;
                averageTimeBetweenShots = totalTimeBetweenShots / timedShots;
            }
            lastShotTime = currentTime;
            
            totalShots++;
            boolean hit = false;
            
            // Apply recoil
            applyRecoil();
            
            for (int i = targets.size() - 1; i >= 0; i--) {
                Target target = targets.get(i);
                if (target.contains(crosshair.x, crosshair.y)) {
                    hit = true;
                    
                    if (multiShotMode) {
                        // 2-shot system
                        target.hits++;
                        target.flashTimer = 10; // Visual feedback
                        
                        if (target.hits >= 2) {
                            // Target destroyed
                            int points = (int)((50 - target.size) * (1 + target.speed * 2));
                            score += points * (1 + combo * 0.1);
                            hits++;
                            combo++;
                            maxCombo = Math.max(maxCombo, combo);
                            targets.remove(i);
                            spawnTarget();
                        } else {
                            // First hit - target damaged but not destroyed
                            score += 5; // Small points for hit
                        }
                    } else {
                        // 1-shot kill mode
                        int points = (int)((50 - target.size) * (1 + target.speed * 2));
                        score += points * (1 + combo * 0.1);
                        hits++;
                        combo++;
                        maxCombo = Math.max(maxCombo, combo);
                        targets.remove(i);
                        spawnTarget();
                    }
                    break;
                }
            }
            
            if (!hit) {
                misses++;
                combo = 0;
            }
            
            // Update accuracy
            accuracy = totalShots > 0 ? (hits * 100.0 / totalShots) : 0;
        }
    }
    
    // Required interface methods
    @Override public void mouseDragged(MouseEvent e) { mouseMoved(e); }
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    
    private class Target {
        double x, y;
        int size;
        double speed;
        double angle;
        long creationTime;
        long lifetime;
        boolean isHovered;
        int hits; // For multi-shot mode
        int flashTimer;
        
        Target(int x, int y, int size) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.speed = random.nextDouble() * 2 + 0.5; // 0.5 - 2.5 pixels per frame
            this.angle = random.nextDouble() * Math.PI * 2;
            this.creationTime = System.currentTimeMillis();
            this.lifetime = 3000 + random.nextInt(2000); // 3-5 seconds
            this.hits = 0;
            this.flashTimer = 0;
        }
        
        void update() {
            // Move target
            x += Math.cos(angle) * speed;
            y += Math.sin(angle) * speed;
            
            // Bounce off walls
            if (x < size || x > WIDTH - size) {
                angle = Math.PI - angle;
                x = Math.max(size, Math.min(WIDTH - size, x));
            }
            if (y < size || y > HEIGHT - size) {
                angle = -angle;
                y = Math.max(size, Math.min(HEIGHT - size, y));
            }
            
            // Update lifetime
            lifetime -= 16; // Assuming 60 FPS
            
            // Update flash timer
            if (flashTimer > 0) {
                flashTimer--;
            }
        }
        
        void draw(Graphics2D g2d) {
            Color color;
            
            if (flashTimer > 0) {
                // Flash when hit
                color = TARGET_HIT;
            } else if (isHovered) {
                color = TARGET_HOVER;
            } else {
                color = TARGET_COLOR;
            }
            
            // Pulsing effect based on lifetime
            double pulse = 1.0 + 0.1 * Math.sin(System.currentTimeMillis() * 0.01);
            int drawSize = (int)(size * pulse);
            
            // Main circle
            g2d.setColor(color);
            g2d.fill(new Ellipse2D.Double(x - drawSize, y - drawSize, drawSize * 2, drawSize * 2));
            
            // Show hit status in multi-shot mode
            if (multiShotMode && hits > 0) {
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Consolas", Font.BOLD, 14));
                String hitText = (2 - hits) + " HIT";
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(hitText);
                g2d.drawString(hitText, (int)x - textWidth/2, (int)y + 5);
            }
            
            // Outer ring
            g2d.setStroke(new BasicStroke(2f));
            g2d.setColor(Color.WHITE);
            g2d.draw(new Ellipse2D.Double(x - drawSize, y - drawSize, drawSize * 2, drawSize * 2));
            
            // Inner dot
            g2d.setColor(Color.WHITE);
            g2d.fillOval((int)x - 3, (int)y - 3, 6, 6);
        }
        
        boolean contains(int px, int py) {
            double distance = Math.sqrt(Math.pow(px - x, 2) + Math.pow(py - y, 2));
            return distance <= size;
        }
    }
    
    public static void main(String[] args) {
        JFrame frame = new JFrame("Advanced Aim Trainer");
        AimTrainer game = new AimTrainer();
        
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.add(game);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
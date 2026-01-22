import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Random;

public class VoidCrawler extends JFrame {
    private static final int GRID_SIZE = 20;
    private static final int TILE_SIZE = 30;
    private GamePanel gamePanel;

    public VoidCrawler() {
        setTitle("Void Crawler");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        
        gamePanel = new GamePanel();
        add(gamePanel);
        pack();
        
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(VoidCrawler::new);
    }

    // --- Inner Game Logic Class ---
    class GamePanel extends JPanel {
        private int playerX = 1, playerY = 1;
        private int goldX, goldY;
        private int score = 0;
        private boolean gameOver = false;
        private ArrayList<Point> enemies = new ArrayList<>();
        private boolean[][] walls = new boolean[GRID_SIZE][GRID_SIZE];
        private Random random = new Random();

        public GamePanel() {
            setPreferredSize(new Dimension(GRID_SIZE * TILE_SIZE, GRID_SIZE * TILE_SIZE));
            setBackground(Color.BLACK);
            setFocusable(true);
            initGame();

            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (!gameOver) {
                        handleInput(e.getKeyCode());
                    } else if (e.getKeyCode() == KeyEvent.VK_R) {
                        initGame();
                    }
                }
            });
        }

        private void initGame() {
            playerX = 1; playerY = 1;
            score = 0;
            gameOver = false;
            enemies.clear();
            generateMap();
            spawnGold();
            enemies.add(new Point(GRID_SIZE - 2, GRID_SIZE - 2));
            repaint();
        }

        private void generateMap() {
            for (int i = 0; i < GRID_SIZE; i++) {
                for (int j = 0; j < GRID_SIZE; j++) {
                    // Create borders and random walls (20% chance)
                    walls[i][j] = i == 0 || i == GRID_SIZE - 1 || j == 0 || j == GRID_SIZE - 1 || (random.nextInt(5) == 0);
                }
            }
            walls[playerX][playerY] = false; // Ensure start is clear
        }

        private void spawnGold() {
            do {
                goldX = random.nextInt(GRID_SIZE);
                goldY = random.nextInt(GRID_SIZE);
            } while (walls[goldX][goldY]);
        }

        private void handleInput(int keyCode) {
            int nextX = playerX, nextY = playerY;

            if (keyCode == KeyEvent.VK_W) nextY--;
            if (keyCode == KeyEvent.VK_S) nextY++;
            if (keyCode == KeyEvent.VK_A) nextX--;
            if (keyCode == KeyEvent.VK_D) nextX++;

            // Collision with walls
            if (!walls[nextX][nextY]) {
                playerX = nextX;
                playerY = nextY;
                moveEnemies();
                checkCollisions();
            }
            repaint();
        }

        private void moveEnemies() {
            for (Point e : enemies) {
                if (e.x < playerX && !walls[e.x + 1][e.y]) e.x++;
                else if (e.x > playerX && !walls[e.x - 1][e.y]) e.x--;
                
                if (e.y < playerY && !walls[e.x][e.y + 1]) e.y++;
                else if (e.y > playerY && !walls[e.x][e.y - 1]) e.y--;
            }
        }

        private void checkCollisions() {
            // Check Gold
            if (playerX == goldX && playerY == goldY) {
                score++;
                spawnGold();
                // Add a new enemy every 3 points to increase difficulty
                if (score % 3 == 0) {
                    enemies.add(new Point(GRID_SIZE - 2, GRID_SIZE - 2));
                }
            }
            // Check Enemies
            for (Point e : enemies) {
                if (playerX == e.x && playerY == e.y) {
                    gameOver = true;
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            // Draw Walls
            g.setColor(Color.DARK_GRAY);
            for (int i = 0; i < GRID_SIZE; i++) {
                for (int j = 0; j < GRID_SIZE; j++) {
                    if (walls[i][j]) g.fillRect(i * TILE_SIZE, j * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                }
            }

            // Draw Gold
            g.setColor(Color.YELLOW);
            g.fillOval(goldX * TILE_SIZE + 5, goldY * TILE_SIZE + 5, TILE_SIZE - 10, TILE_SIZE - 10);

            // Draw Player
            g.setColor(Color.CYAN);
            g.fillRect(playerX * TILE_SIZE, playerY * TILE_SIZE, TILE_SIZE, TILE_SIZE);

            // Draw Enemies
            g.setColor(Color.RED);
            for (Point e : enemies) {
                g.fillRect(e.x * TILE_SIZE, e.y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            }

            // Draw UI
            g.setColor(Color.WHITE);
            g.drawString("Score: " + score, 10, 20);

            if (gameOver) {
                g.setColor(new Color(0, 0, 0, 150));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Color.WHITE);
                g.setFont(new Font("Arial", Font.BOLD, 30));
                g.drawString("GAME OVER", getWidth() / 4, getHeight() / 2);
                g.setFont(new Font("Arial", Font.PLAIN, 15));
                g.drawString("Press 'R' to Restart", getWidth() / 3, getHeight() / 2 + 40);
            }
        }
    }
}
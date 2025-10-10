import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

public class SchrödingersCatSimulator {
    private JFrame frame;
    private JPanel mainPanel, buttonPanel, dialogPanel;
    private JLabel statusLabel, animationLabel, dialogLabel;
    private JButton addCatButton, addSubstanceButton, lookButton, resetButton;
    private boolean catInBox = false;
    private boolean substanceInBox = false;
    private boolean boxObserved = false;
    private boolean catIsAlive = true;
    private boolean easterEggTriggered = false;
    private boolean easterEggShownThisSession = false;
    private boolean isEasterEggActive = false;
    private Random random = new Random();
    private Timer animationTimer, dialogTimer, shakeTimer;
    private int animationFrame = 0;
    private Point originalBoxPosition;
    
    // Survival probability starts high and decreases with each observation
    private double survivalProbability = 0.8; // 80% chance to survive first time
    
    // Animation frames
    private final String[] BOX_ANIMATION = {"[     ]", "[▌    ]", "[██   ]", "[███  ]", "[████ ]", "[█████]"};
    private final String[] CAT_ANIMATION = {"(=^･ω･^=)", "(=^･ｪ･^=)", "(=^◕ᴥ◕^=)"};
    private final String[] SUBSTANCE_ANIMATION = {"☢", "☢", "☢", "⚡", "☢"};
    
    public SchrödingersCatSimulator() {
        initializeGUI();
    }
    
    private void initializeGUI() {
        // Create main frame
        frame = new JFrame("Schrödinger's Cat Simulator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(new Color(45, 45, 45));
        
        // Create main panel with dark theme
        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(new Color(45, 45, 45));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Dialog panel (for easter egg - initially hidden)
        dialogPanel = new JPanel();
        dialogPanel.setLayout(new BorderLayout());
        dialogPanel.setBackground(new Color(30, 30, 60));
        dialogPanel.setForeground(Color.WHITE);
        dialogPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(100, 149, 237), 2),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        dialogPanel.setVisible(false);
        
        dialogLabel = new JLabel("", SwingConstants.CENTER);
        dialogLabel.setForeground(Color.WHITE);
        dialogLabel.setFont(new Font("Arial", Font.BOLD, 14));
        dialogPanel.add(dialogLabel, BorderLayout.CENTER);
        
        // Status label
        statusLabel = new JLabel("Welcome to Schrödinger's Cat Experiment!", SwingConstants.CENTER);
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 16));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 30, 10));
        
        // Animation label
        animationLabel = new JLabel("📦", SwingConstants.CENTER);
        animationLabel.setForeground(Color.WHITE);
        animationLabel.setFont(new Font("Monospaced", Font.BOLD, 48));
        animationLabel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
        
        // Create buttons with modern styling
        addCatButton = createStyledButton("Put Cat in Box 🐱");
        addSubstanceButton = createStyledButton("Add Radioactive Substance ☢️");
        lookButton = createStyledButton("Look Inside the Box 👀");
        resetButton = createStyledButton("Reset Experiment 🔄");
        
        // Button panel
        buttonPanel = new JPanel(new GridLayout(2, 2, 15, 15));
        buttonPanel.setBackground(new Color(45, 45, 45));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 50, 20, 50));
        
        buttonPanel.add(addCatButton);
        buttonPanel.add(addSubstanceButton);
        buttonPanel.add(lookButton);
        buttonPanel.add(resetButton);
        
        // Add components to main panel
        mainPanel.add(dialogPanel, BorderLayout.NORTH);
        mainPanel.add(statusLabel, BorderLayout.CENTER);
        mainPanel.add(animationLabel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        frame.add(mainPanel);
        frame.setSize(600, 500);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        
        // Initialize original box position
        originalBoxPosition = new Point(0, 0);
        
        setupEventHandlers();
        updateUI();
    }
    
    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBackground(new Color(70, 130, 180));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(100, 149, 237), 2),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Hover effects
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(100, 149, 237));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(70, 130, 180));
            }
        });
        
        return button;
    }
    
    private void setupEventHandlers() {
        addCatButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!catInBox && !isEasterEggActive) {
                    catInBox = true;
                    catIsAlive = true; // New cat is always alive
                    playBoxAnimation("🐱 Adding cat to box...", CAT_ANIMATION);
                    updateStatus("Cat is now in the box! (Quantum state: Superposition)");
                } else if (!isEasterEggActive) {
                    showMessage("There's already a cat in the box!");
                }
            }
        });
        
        addSubstanceButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!substanceInBox && !isEasterEggActive) {
                    substanceInBox = true;
                    playBoxAnimation("☢️ Adding radioactive substance...", SUBSTANCE_ANIMATION);
                    updateStatus("Radioactive substance added! Quantum decay may occur.");
                } else if (!isEasterEggActive) {
                    showMessage("Radioactive substance is already in the box!");
                }
            }
        });
        
        lookButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isEasterEggActive) {
                    return; // Ignore clicks during easter egg
                }
                
                if (!catInBox && !substanceInBox) {
                    showMessage("The box is completely empty! Add a cat or radioactive substance first.");
                    return;
                }
                
                if (!catInBox) {
                    showMessage("You look inside and see only the radioactive substance. No cat in the box!");
                    return;
                }
                
                if (!substanceInBox) {
                    showMessage("You look inside and see the cat sleeping peacefully. No radioactive substance in the box!");
                    return;
                }
                
                // If cat is already dead, show appropriate message
                if (!catIsAlive) {
                    showMessage("💀 The cat remains deceased. The quantum decay was irreversible.");
                    updateStatus("Observation: Cat is still DEAD. Quantum states cannot be reversed.");
                    return;
                }
                
                // Cat is alive and both elements are present - determine survival
                boxObserved = true;
                boolean catSurvives = determineSurvival();
                
                if (catSurvives) {
                    // Check if we should show easter egg (only once per session)
                    boolean showEasterEgg = !easterEggShownThisSession;
                    if (showEasterEgg) {
                        // Skip normal observation and go straight to easter egg
                        updateStatus("Observing quantum state... Something unexpected happens!");
                        playEasterEggDirectly();
                        easterEggShownThisSession = true;
                    } else {
                        playObservationAnimation("Observation complete: Cat is ALIVE! Wavefunction collapsed.", "😺 Cat is alive and well!", false);
                    }
                    // Decrease survival probability for next time
                    survivalProbability = Math.max(0.1, survivalProbability - 0.15); // Minimum 10% chance
                } else {
                    // Cat dies - irreversible
                    catIsAlive = false;
                    playObservationAnimation("Observation complete: Cat is DEAD. Wavefunction collapsed.", "💀 Cat did not survive quantum decay", false);
                    survivalProbability = 0.0; // No chance of survival once dead
                }
            }
        });
        
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!isEasterEggActive) {
                    resetExperiment();
                }
            }
        });
    }
    
    private boolean determineSurvival() {
        // Higher probability to survive at the start, decreases over time
        return random.nextDouble() < survivalProbability;
    }
    
    private void playBoxAnimation(String message, String[] animationFrames) {
        updateStatus(message);
        
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
        
        animationFrame = 0;
        animationTimer = new Timer(300, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (animationFrame < animationFrames.length) {
                    animationLabel.setText(animationFrames[animationFrame]);
                    animationFrame++;
                } else {
                    animationTimer.stop();
                    animationLabel.setText("📦");
                    updateUI();
                }
            }
        });
        animationTimer.start();
    }
    
    private void playObservationAnimation(final String statusMessage, final String resultMessage, final boolean triggerEasterEgg) {
        // First show the observing status
        updateStatus("Observing quantum state... Wavefunction collapsing...");
        
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
        
        animationFrame = 0;
        final String[] observationAnimation = {"🔍", "⚡", "🌌", "💫"};
        
        animationTimer = new Timer(400, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (animationFrame < observationAnimation.length) {
                    // Update animation emoji
                    animationLabel.setText(observationAnimation[animationFrame]);
                    animationFrame++;
                } else {
                    // Animation complete - show result and final status
                    animationTimer.stop();
                    showMessage(resultMessage);
                    animationLabel.setText("📦");
                    updateStatus(statusMessage);
                    updateUI();
                }
            }
        });
        animationTimer.start();
    }
    
    private void playEasterEggDirectly() {
        isEasterEggActive = true;
        updateUI(); // Disable buttons during easter egg
        
        // Store current box position for shaking
        originalBoxPosition = animationLabel.getLocation();
        if (originalBoxPosition == null) {
            originalBoxPosition = new Point(0, 0);
        }
        
        // Small delay before easter egg starts
        Timer delayTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                triggerEasterEgg();
                ((Timer)e.getSource()).stop();
            }
        });
        delayTimer.setRepeats(false);
        delayTimer.start();
    }
    
    private void triggerEasterEgg() {
        easterEggTriggered = true;
        
        // Store the dialog panel's preferred size for animation
        dialogPanel.setPreferredSize(new Dimension(frame.getWidth(), 40));
        mainPanel.revalidate();
        
        Timer easterEggTimer = new Timer(2000, new ActionListener() {
            private int stage = 0;
            
            @Override
            public void actionPerformed(ActionEvent e) {
                switch (stage) {
                    case 0:
                        // Scientist speaks - slide dialog down
                        showDialog("Scientist: You don't know whether the cat inside the box is alive or dead.", true);
                        break;
                    case 1:
                        // Cat meows - shake the box
                        showDialog("Cat: Meow.", false);
                        startBoxShake();
                        break;
                    case 2:
                        // Scientist responds
                        showDialog("Scientist: Shut up.", false);
                        stopBoxShake();
                        break;
                    case 3:
                        // Hide dialog after a delay
                        hideDialog();
                        easterEggTriggered = false;
                        isEasterEggActive = false;
                        updateStatus("The cat seems to have... opinions about quantum mechanics.");
                        updateUI(); // Re-enable buttons
                        ((Timer)e.getSource()).stop();
                        break;
                }
                stage++;
            }
        });
        easterEggTimer.setInitialDelay(500);
        easterEggTimer.start();
    }
    
    private void showDialog(String text, boolean slideIn) {
        dialogLabel.setText(text);
        
        if (slideIn) {
            // Slide in animation
            dialogPanel.setVisible(true);
            dialogPanel.setLocation(0, -50);
            
            Timer slideTimer = new Timer(20, new ActionListener() {
                private int yPos = -50;
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (yPos < 0) {
                        yPos += 5;
                        dialogPanel.setLocation(0, yPos);
                    } else {
                        ((Timer)e.getSource()).stop();
                        dialogPanel.setLocation(0, 0);
                    }
                }
            });
            slideTimer.start();
        } else {
            dialogPanel.setVisible(true);
            dialogPanel.setLocation(0, 0);
        }
        
        mainPanel.revalidate();
        mainPanel.repaint();
    }
    
    private void hideDialog() {
        Timer slideTimer = new Timer(20, new ActionListener() {
            private int yPos = 0;
            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (yPos > -50) {
                    yPos -= 5;
                    dialogPanel.setLocation(0, yPos);
                } else {
                    ((Timer)e.getSource()).stop();
                    dialogPanel.setVisible(false);
                }
            }
        });
        slideTimer.start();
    }
    
    private void startBoxShake() {
        if (shakeTimer != null && shakeTimer.isRunning()) {
            shakeTimer.stop();
        }
        
        final int shakeIntensity = 5;
        shakeTimer = new Timer(100, new ActionListener() {
            private int shakeCount = 0;
            private boolean direction = true;
            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (shakeCount < 8) { // Shake for 8 cycles
                    int offset = direction ? shakeIntensity : -shakeIntensity;
                    animationLabel.setLocation(originalBoxPosition.x + offset, originalBoxPosition.y);
                    direction = !direction;
                    shakeCount++;
                } else {
                    shakeTimer.stop();
                    animationLabel.setLocation(originalBoxPosition);
                }
            }
        });
        shakeTimer.start();
    }
    
    private void stopBoxShake() {
        if (shakeTimer != null && shakeTimer.isRunning()) {
            shakeTimer.stop();
        }
        if (originalBoxPosition != null) {
            animationLabel.setLocation(originalBoxPosition);
        }
    }
    
    private void resetExperiment() {
        catInBox = false;
        substanceInBox = false;
        boxObserved = false;
        catIsAlive = true;
        easterEggTriggered = false;
        easterEggShownThisSession = false;
        isEasterEggActive = false;
        survivalProbability = 0.8; // Reset to high survival chance
        
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
        if (shakeTimer != null && shakeTimer.isRunning()) {
            shakeTimer.stop();
        }
        if (dialogTimer != null && dialogTimer.isRunning()) {
            dialogTimer.stop();
        }
        
        // Hide dialog if visible
        dialogPanel.setVisible(false);
        stopBoxShake();
        
        // Reset box position safely
        animationLabel.setLocation(0, 0);
        originalBoxPosition = new Point(0, 0);
        
        playBoxAnimation("Resetting experiment...", new String[]{"🔄", "⚡", "🌀", "📦"});
        updateStatus("Experiment reset! Ready for new quantum superposition.");
    }
    
    private void updateUI() {
        boolean buttonsEnabled = !isEasterEggActive;
        
        addCatButton.setEnabled(!catInBox && buttonsEnabled);
        addSubstanceButton.setEnabled(!substanceInBox && buttonsEnabled);
        lookButton.setEnabled(catInBox && buttonsEnabled);
        resetButton.setEnabled(buttonsEnabled);
        
        // Update button states visually
        addCatButton.setBackground(catInBox ? new Color(100, 100, 100) : new Color(70, 130, 180));
        addSubstanceButton.setBackground(substanceInBox ? new Color(100, 100, 100) : new Color(70, 130, 180));
        
        // If cat is dead, change look button color to indicate irreversible state
        if (catInBox && substanceInBox && !catIsAlive) {
            lookButton.setBackground(new Color(150, 0, 0)); // Red color for dead cat
            lookButton.setForeground(Color.WHITE);
        } else {
            lookButton.setBackground(new Color(70, 130, 180));
            lookButton.setForeground(Color.WHITE);
        }
    }
    
    private void updateStatus(String message) {
        statusLabel.setText("<html><div style='text-align: center;'>" + message + "</div></html>");
    }
    
    private void showMessage(String message) {
        JOptionPane.showMessageDialog(frame, message, "Quantum Observation", JOptionPane.INFORMATION_MESSAGE);
    }
    
    public void show() {
        frame.setVisible(true);
    }
    
    public static void main(String[] args) {
        // Use SwingUtilities to ensure thread safety
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new SchrödingersCatSimulator().show();
            }
        });
    }
}
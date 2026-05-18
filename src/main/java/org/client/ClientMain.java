package org.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.image.Image;

public class ClientMain extends Application {

    private enum ZombieType {
        NORMAL,
        BIG,
        FAST
    }

    private enum MatchState {
        RUNNING,
        LOST,
        WON
    }

    private final Canvas canvas = new Canvas(800, 600);
    private NetworkClient networkClient;
    private Image zombieImage;
    private Image bigZombieImage;
    private Image fastZombieImage;
    private Image player1Image;
    private Image player2Image;
    private int myId = -1;
    private MatchState matchState = MatchState.RUNNING;
    private long restartCountdownMs = 0;

    private final List<PlayerView> players = new ArrayList<>();
    private final List<ZombieView> zombies = new ArrayList<>();
    private final List<Entity> medkits = new ArrayList<>();
    private final List<ShotView> shots = new ArrayList<>();
    private final List<RectView> walls = new ArrayList<>();
    private final List<Entity> trees = new ArrayList<>();

    private RectView river = new RectView(0, 260, 800, 80);
    private ObjectiveView objective = new ObjectiveView(400, 300, 200, 200);

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane(canvas);
        Scene scene = new Scene(root, 800, 600);

        zombieImage = loadImage("/images/zombie.png");
        bigZombieImage = loadImage("/images/bigzombie.png");
        fastZombieImage = loadImage("/images/fastzombie.png");
        player1Image = loadImage("/images/player1.png");
        player2Image = loadImage("/images/player2.png");

//3.70.131.13
        networkClient = new NetworkClient("3.70.131.13", 5000, this::onServerMessage);
        networkClient.start();

        scene.setOnKeyPressed(event -> {
            if (!isInputEnabled()) return;

            KeyCode code = event.getCode();

            if (code == KeyCode.W || code == KeyCode.UP) networkClient.send("MOVE_UP");
            if (code == KeyCode.S || code == KeyCode.DOWN) networkClient.send("MOVE_DOWN");
            if (code == KeyCode.A || code == KeyCode.LEFT) networkClient.send("MOVE_LEFT");
            if (code == KeyCode.D || code == KeyCode.RIGHT) networkClient.send("MOVE_RIGHT");
        });

        scene.setOnMouseClicked(event -> {
            if (!isInputEnabled()) return;

            networkClient.send("SHOOT " + event.getX() + " " + event.getY());
        });

        stage.setTitle("Zombie Survival Client");
        stage.setScene(scene);
        stage.show();

        draw();
    }

    private void onServerMessage(String message) {
        Platform.runLater(() -> {
            if (message.startsWith("WELCOME")) {
                myId = Integer.parseInt(message.split(" ")[1]);
                System.out.println("Il mio ID è " + myId);
            }

            if (message.startsWith("STATE")) {
                parseState(message);
                draw();
            }
        });
    }

    private void parseState(String message) {
        players.clear();
        zombies.clear();
        medkits.clear();
        shots.clear();
        walls.clear();
        trees.clear();

        String[] parts = message.split("\\|");

        for (String part : parts) {
            if (part.equals("STATE")) continue;

            if (part.startsWith("MATCH")) {
                parseMatchState(part);
                continue;
            }

            String[] data = part.split(",");

            if (data.length < 4) continue;

            String type = data[0];
            int id = Integer.parseInt(data[1]);
            double x = Double.parseDouble(data[2]);
            double y = Double.parseDouble(data[3]);

            switch (type) {
                case "PLAYER" -> {
                    int health = Integer.parseInt(data[4]);
                    int ammo = Integer.parseInt(data[5]);
                    players.add(new PlayerView(id, x, y, health, ammo));
                }
                case "ZOMBIE" -> zombies.add(parseZombieView(id, x, y, data));
                case "MEDKIT" -> medkits.add(new Entity(id, x, y));
                case "SHOT" -> {
                    double x2 = Double.parseDouble(data[4]);
                    double y2 = Double.parseDouble(data[5]);
                    shots.add(new ShotView(x, y, x2, y2));
                }
                case "WALL" -> {
                    double w = Double.parseDouble(data[4]);
                    double h = Double.parseDouble(data[5]);
                    walls.add(new RectView(x, y, w, h));
                }
                case "TREE" -> trees.add(new Entity(id, x, y));
                case "RIVER" -> {
                    double w = Double.parseDouble(data[4]);
                    double h = Double.parseDouble(data[5]);
                    river = new RectView(x, y, w, h);
                }
                case "OBJECTIVE" -> {
                    int hp = Integer.parseInt(data[4]);
                    int maxHp = Integer.parseInt(data[5]);
                    objective = new ObjectiveView(x, y, hp, maxHp);
                }
            }
        }
    }

    private ZombieView parseZombieView(int id, double x, double y, String[] data) {
        ZombieType zombieType = ZombieType.NORMAL;
        int health = 1;

        if (data.length >= 5) {
            try {
                zombieType = ZombieType.valueOf(data[4]);
            } catch (IllegalArgumentException ignored) {
                zombieType = ZombieType.NORMAL;
            }
        }

        if (data.length >= 6) {
            try {
                health = Integer.parseInt(data[5]);
            } catch (NumberFormatException ignored) {
                health = zombieType == ZombieType.BIG ? 2 : 1;
            }
        } else if (zombieType == ZombieType.BIG) {
            health = 2;
        }

        return new ZombieView(id, x, y, zombieType, health);
    }

    private Image loadImage(String path) {
        var url = getClass().getResource(path);
        return url == null ? null : new Image(url.toExternalForm());
    }

    private double screenX(double worldX) {
        return worldX;
    }

    private double screenY(double worldY) {
        return worldY;
    }

    private void parseMatchState(String part) {
        String[] data = part.split(",");
        if (data.length < 3) return;

        try {
            matchState = MatchState.valueOf(data[1]);
        } catch (IllegalArgumentException ignored) {
            matchState = MatchState.RUNNING;
        }

        try {
            restartCountdownMs = Long.parseLong(data[2]);
        } catch (NumberFormatException ignored) {
            restartCountdownMs = 0;
        }
    }

    private void draw() {
        GraphicsContext g = canvas.getGraphicsContext2D();

        drawBackground(g);
        drawRiver(g);
        drawTrees(g);
        drawWalls(g);
        drawObjective(g);
        drawMedkits(g);
        drawShots(g);
        drawZombies(g);
        drawPlayers(g);
        drawHud(g);
        drawMatchOverlay(g);
    }

    private void drawBackground(GraphicsContext g) {
        g.setFill(Color.rgb(42, 72, 42));
        g.fillRect(0, 0, 800, 600);

        g.setFill(Color.rgb(35, 60, 35));
        for (int x = 0; x < 800; x += 80) {
            g.fillRect(x, 0, 2, 600);
        }
        for (int y = 0; y < 600; y += 80) {
            g.fillRect(0, y, 800, 2);
        }
    }

    private void drawRiver(GraphicsContext g) {
        g.setFill(Color.rgb(40, 110, 180));
        g.fillRect(river.x, river.y, river.width, river.height);

        g.setFill(Color.rgb(90, 170, 230, 0.45));
        for (int x = 0; x < 800; x += 55) {
            g.fillOval(x, river.y + 20, 45, 8);
            g.fillOval(x + 20, river.y + 50, 45, 8);
        }
    }

    private void drawTrees(GraphicsContext g) {
        for (Entity tree : trees) {
            g.setFill(Color.rgb(90, 55, 25));
            g.fillRect(tree.x - 4, tree.y, 8, 18);

            g.setFill(Color.rgb(20, 100, 35));
            g.fillOval(tree.x - 18, tree.y - 22, 36, 36);

            g.setFill(Color.rgb(35, 135, 45));
            g.fillOval(tree.x - 10, tree.y - 30, 28, 28);
        }
    }

    private void drawWalls(GraphicsContext g) {
        for (RectView wall : walls) {
            g.setFill(Color.rgb(80, 80, 80));
            g.fillRect(wall.x, wall.y, wall.width, wall.height);

            g.setStroke(Color.rgb(135, 135, 135));
            g.strokeRect(wall.x, wall.y, wall.width, wall.height);
        }
    }

    private void drawObjective(GraphicsContext g) {
        if (objective.health <= 0) {
            g.setFill(Color.rgb(70, 40, 40));
            g.fillOval(objective.x - 32, objective.y - 32, 64, 64);

            g.setFill(Color.WHITE);
            g.fillText("DISTRUTTO", objective.x - 30, objective.y + 5);
            return;
        }

        g.setFill(Color.rgb(120, 30, 150));
        g.fillOval(objective.x - 32, objective.y - 32, 64, 64);

        g.setFill(Color.rgb(190, 80, 240));
        g.fillOval(objective.x - 20, objective.y - 20, 40, 40);

        g.setFill(Color.WHITE);
        g.fillText("OBIETTIVO", objective.x - 35, objective.y - 42);

        double barWidth = 90;
        double barHeight = 8;
        double bx = objective.x - barWidth / 2;
        double by = objective.y + 40;

        g.setFill(Color.DARKRED);
        g.fillRect(bx, by, barWidth, barHeight);

        g.setFill(Color.LIME);
        g.fillRect(bx, by, barWidth * objective.health / objective.maxHealth, barHeight);

        g.setStroke(Color.WHITE);
        g.strokeRect(bx, by, barWidth, barHeight);

        g.setFill(Color.WHITE);
        g.fillText(objective.health + "/" + objective.maxHealth, objective.x - 25, by + 22);
    }

    private void drawMedkits(GraphicsContext g) {
        for (Entity medkit : medkits) {
            g.setFill(Color.RED);
            g.fillRect(medkit.x - 8, medkit.y - 8, 16, 16);

            g.setFill(Color.WHITE);
            g.fillRect(medkit.x - 2, medkit.y - 7, 4, 14);
            g.fillRect(medkit.x - 7, medkit.y - 2, 14, 4);
        }
    }

    private void drawShots(GraphicsContext g) {
        for (ShotView shot : shots) {
            g.setStroke(Color.YELLOW);
            g.setLineWidth(3);
            g.strokeLine(shot.x1, shot.y1, shot.x2, shot.y2);
            g.setLineWidth(1);
        }
    }

    private void drawZombies(GraphicsContext g) {
        for (ZombieView zombie : zombies) {
            drawZombie(g, zombie);
        }
    }

    private void drawZombie(GraphicsContext g, ZombieView zombie) {
        double size = zombie.getRenderSize();
        Image sprite = switch (zombie.type) {
            case BIG -> bigZombieImage != null ? bigZombieImage : zombieImage;
            case FAST -> fastZombieImage != null ? fastZombieImage : zombieImage;
            default -> zombieImage;
        };

        if (sprite != null) {
            g.drawImage(sprite, screenX(zombie.x) - size / 2, screenY(zombie.y) - size / 2, size, size);
            return;
        }

        g.setFill(switch (zombie.type) {
            case BIG -> Color.DARKRED;
            case FAST -> Color.LIGHTSEAGREEN;
            default -> Color.DARKGREEN;
        });
        g.fillOval(screenX(zombie.x) - size / 2, screenY(zombie.y) - size / 2, size, size);
    }

    private void drawPlayers(GraphicsContext g) {
        for (PlayerView player : players) {
            boolean isMe = player.id == myId;
            double x = screenX(player.x);
            double y = screenY(player.y);
            boolean isFirstPlayer = player.id == 1;
            boolean isSecondPlayer = player.id == 2;

            if (isFirstPlayer || isSecondPlayer) {
                double spriteWidth = 36;
                double spriteHeight = 48;
                Image sprite = isFirstPlayer ? player1Image : player2Image;

                if (sprite != null) {
                    g.drawImage(sprite, x - spriteWidth / 2, y - spriteHeight / 2, spriteWidth, spriteHeight);
                } else {
                    g.setFill(isFirstPlayer ? Color.LIGHTGRAY : Color.PURPLE);
                    g.fillOval(x - spriteWidth / 2, y - spriteHeight / 2, spriteWidth, spriteHeight);
                }
            } else {
                g.setFill(isMe ? Color.DODGERBLUE : Color.ORANGE);
                g.fillOval(x - 12, y - 12, 24, 24);
            }

            g.setFill(Color.WHITE);
            g.fillText("P" + player.id, x - 8, y - 20);

            drawHealthBar(g, player);
        }
    }

    private void drawHud(GraphicsContext g) {
        PlayerView me = null;

        for (PlayerView player : players) {
            if (player.id == myId) {
                me = player;
                break;
            }
        }

        g.setFill(Color.rgb(0, 0, 0, 0.45));
        g.fillRect(8, 8, 360, 120);

        g.setFill(Color.WHITE);
        g.fillText("WASD/Frecce = movimento | Click = spara", 15, 25);
        g.fillText("Fiume = velocità -50% | Obiettivo centrale: 200 HP", 15, 45);
        g.fillText("Stato: " + matchLabel(), 15, 105);

        if (me != null) {
            g.fillText("Vita: " + me.health + "/100", 15, 65);
            g.fillText("Munizioni: " + me.ammo + "/5", 15, 85);
        }
    }

    private void drawHealthBar(GraphicsContext g, PlayerView player) {
        double barWidth = 40;
        double barHeight = 6;
        double x = screenX(player.x) - barWidth / 2;
        double y = screenY(player.y) + 18;

        g.setFill(Color.DARKRED);
        g.fillRect(x, y, barWidth, barHeight);

        g.setFill(Color.LIME);
        g.fillRect(x, y, barWidth * player.health / 100.0, barHeight);

        g.setStroke(Color.WHITE);
        g.strokeRect(x, y, barWidth, barHeight);
    }

    private void drawMatchOverlay(GraphicsContext g) {
        if (matchState == MatchState.RUNNING) return;

        double pulse = 0.25 + 0.15 * Math.sin(System.currentTimeMillis() / 140.0);

        if (matchState == MatchState.LOST) {
            g.setFill(Color.rgb(140, 0, 0, 0.55 + pulse));
        } else {
            g.setFill(Color.rgb(0, 90, 0, 0.50 + pulse));
        }

        g.fillRect(0, 0, 800, 600);

        g.setFill(Color.WHITE);
        g.setFont(Font.font("Arial", 34));
        g.fillText(matchTitle(), 130, 255);

        g.setFont(Font.font("Arial", 20));
        g.fillText(matchSubtitle(), 175, 290);
        g.fillText("Riavvio automatico tra " + (restartCountdownMs / 1000.0) + "s", 230, 325);
    }

    private String matchTitle() {
        return switch (matchState) {
            case LOST -> "SEI MORTO";
            case WON -> "COMPLIMENTI, HAI VINTO!";
            default -> "";
        };
    }

    private String matchSubtitle() {
        return switch (matchState) {
            case LOST -> "Il server riparte tra pochi secondi.";
            case WON -> "GIOCA DI NUOVO - la partita ricomincia a breve.";
            default -> "";
        };
    }

    private String matchLabel() {
        return switch (matchState) {
            case LOST -> "FINE PARTITA";
            case WON -> "VITTORIA";
            default -> "IN CORSO";
        };
    }

    private boolean isInputEnabled() {
        return matchState == MatchState.RUNNING && networkClient != null;
    }

    @Override
    public void stop() {
        if (networkClient != null) {
            networkClient.close();
        }
    }

    private static class Entity {
        int id;
        double x;
        double y;

        Entity(int id, double x, double y) {
            this.id = id;
            this.x = x;
            this.y = y;
        }
    }

    private static class ZombieView extends Entity {
        ZombieType type;
        int health;

        ZombieView(int id, double x, double y, ZombieType type, int health) {
            super(id, x, y);
            this.type = type;
            this.health = health;
        }

        double getRenderSize() {
            return switch (type) {
                case BIG -> 38;
                case FAST -> 24;
                default -> 30;
            };
        }
    }

    private static class PlayerView extends Entity {
        int health;
        int ammo;

        PlayerView(int id, double x, double y, int health, int ammo) {
            super(id, x, y);
            this.health = health;
            this.ammo = ammo;
        }
    }

    private static class ShotView {
        double x1, y1, x2, y2;

        ShotView(double x1, double y1, double x2, double y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }
    }

    private static class RectView {
        double x, y, width, height;

        RectView(double x, double y, double width, double height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    private static class ObjectiveView {
        double x, y;
        double health, maxHealth;

        ObjectiveView(double x, double y, double health, double maxHealth) {
            this.x = x;
            this.y = y;
            this.health = health;
            this.maxHealth = maxHealth;
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
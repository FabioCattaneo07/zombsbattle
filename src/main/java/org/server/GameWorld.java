package org.server;

import java.util.*;

public class GameWorld {

    private final Map<Integer, Player> players = new HashMap<>();
    private final List<Zombie> zombies = new ArrayList<>();
    private final List<Medkit> medkits = new ArrayList<>();
    private final List<Shot> shots = new ArrayList<>();

    private final List<Wall> walls = new ArrayList<>();
    private final List<Tree> trees = new ArrayList<>();

    private final Objective objective = new Objective(400, 300);

    private final Random random = new Random();

    private int nextPlayerId = 1;
    private int nextZombieId = 1;
    private int nextMedkitId = 1;
    private int nextTreeId = 1;

    private long lastZombieSpawnTime = System.currentTimeMillis();

    private final double riverX = 0;
    private final double riverY = 260;
    private final double riverWidth = 800;
    private final double riverHeight = 80;

    public GameWorld() {
        createMap();

        zombies.add(new Zombie(nextZombieId++, 250, 200));
        zombies.add(new Zombie(nextZombieId++, 500, 350));
        zombies.add(new Zombie(nextZombieId++, 650, 150));

        spawnMedkit();
        spawnMedkit();
        spawnMedkit();
        spawnMedkit();
    }

    private void createMap() {
        walls.add(new Wall(120, 120, 160, 25));
        walls.add(new Wall(520, 110, 150, 25));
        walls.add(new Wall(140, 430, 170, 25));
        walls.add(new Wall(520, 455, 160, 25));
        walls.add(new Wall(340, 150, 25, 120));
        walls.add(new Wall(455, 330, 25, 130));

        trees.add(new Tree(nextTreeId++, 80, 80));
        trees.add(new Tree(nextTreeId++, 720, 80));
        trees.add(new Tree(nextTreeId++, 100, 520));
        trees.add(new Tree(nextTreeId++, 720, 520));
        trees.add(new Tree(nextTreeId++, 230, 360));
        trees.add(new Tree(nextTreeId++, 610, 250));
        trees.add(new Tree(nextTreeId++, 380, 80));
        trees.add(new Tree(nextTreeId++, 380, 520));
    }

    public synchronized int addPlayer() {
        int id = nextPlayerId++;
        players.put(id, new Player(id, 100 + id * 40, 100));
        return id;
    }

    public synchronized void removePlayer(int id) {
        players.remove(id);
    }

    public synchronized void handleCommand(int playerId, String command) {
        Player player = players.get(playerId);
        if (player == null) return;

        switch (command) {
            case "MOVE_UP" -> movePlayer(player, 0, -1);
            case "MOVE_DOWN" -> movePlayer(player, 0, 1);
            case "MOVE_LEFT" -> movePlayer(player, -1, 0);
            case "MOVE_RIGHT" -> movePlayer(player, 1, 0);
            default -> {
                if (command.startsWith("SHOOT")) {
                    handleShoot(player, command);
                }
            }
        }
    }

    private void movePlayer(Player player, double dx, double dy) {
        double speed = isInRiver(player.x, player.y) ? 5 : 10;

        double oldX = player.x;
        double oldY = player.y;

        player.x += dx * speed;
        player.y += dy * speed;

        player.x = Math.max(10, Math.min(790, player.x));
        player.y = Math.max(10, Math.min(590, player.y));

        if (collidesWithWall(player.x, player.y, 12)) {
            player.x = oldX;
            player.y = oldY;
        }
    }

    private void handleShoot(Player player, String command) {
        if (player.ammo <= 0) return;

        String[] parts = command.split(" ");
        if (parts.length != 3) return;

        double targetX = Double.parseDouble(parts[1]);
        double targetY = Double.parseDouble(parts[2]);

        player.ammo--;

        double dx = targetX - player.x;
        double dy = targetY - player.y;
        double length = Math.sqrt(dx * dx + dy * dy);

        if (length == 0) return;

        dx /= length;
        dy /= length;

        double maxRange = 700;

        Zombie hitZombie = null;
        double bestDistance = Double.MAX_VALUE;

        for (Zombie zombie : zombies) {
            double distanceAlongRay = (zombie.x - player.x) * dx + (zombie.y - player.y) * dy;

            if (distanceAlongRay < 0 || distanceAlongRay > maxRange) continue;

            double closestX = player.x + dx * distanceAlongRay;
            double closestY = player.y + dy * distanceAlongRay;

            double distanceToLine = distance(zombie.x, zombie.y, closestX, closestY);

            if (distanceToLine < 18 && distanceAlongRay < bestDistance) {
                bestDistance = distanceAlongRay;
                hitZombie = zombie;
            }
        }

        boolean hitObjective = false;
        double objectiveDistance = (objective.x - player.x) * dx + (objective.y - player.y) * dy;

        if (objective.health > 0 && objectiveDistance > 0 && objectiveDistance < maxRange) {
            double closestX = player.x + dx * objectiveDistance;
            double closestY = player.y + dy * objectiveDistance;

            if (distance(objective.x, objective.y, closestX, closestY) < 35) {
                if (objectiveDistance < bestDistance) {
                    hitObjective = true;
                    bestDistance = objectiveDistance;
                }
            }
        }

        double endX = player.x + dx * maxRange;
        double endY = player.y + dy * maxRange;

        if (hitObjective) {
            objective.health -= 5;
            if (objective.health < 0) objective.health = 0;

            endX = objective.x;
            endY = objective.y;
        } else if (hitZombie != null) {
            endX = hitZombie.x;
            endY = hitZombie.y;
            zombies.remove(hitZombie);
        }

        shots.add(new Shot(player.x, player.y, endX, endY));
    }

    public synchronized void update() {
        for (Player player : players.values()) {
            player.updateReload();
        }

        spawnZombiesOverTime();

        for (Zombie zombie : zombies) {
            Player nearest = findNearestPlayer(zombie);

            if (nearest != null) {
                moveZombieToward(zombie, nearest.x, nearest.y);
            }

            zombie.separateFromOtherZombies(zombies);

            zombie.x = Math.max(10, Math.min(790, zombie.x));
            zombie.y = Math.max(10, Math.min(590, zombie.y));
        }

        checkZombieDamage();
        checkMedkitPickup();

        shots.removeIf(Shot::isExpired);
    }

    private void moveZombieToward(Zombie zombie, double targetX, double targetY) {

        double originalSpeed = zombie.speed;

        if (isInRiver(zombie.x, zombie.y)) {
            zombie.speed = originalSpeed * 0.5;
        }

        zombie.moveToward(targetX, targetY);

        zombie.speed = originalSpeed;
    }

    private boolean isInRiver(double x, double y) {
        return x >= riverX && x <= riverX + riverWidth &&
                y >= riverY && y <= riverY + riverHeight;
    }

    private boolean collidesWithWall(double x, double y, double radius) {
        for (Wall wall : walls) {
            if (wall.collides(x, y, radius)) {
                return true;
            }
        }

        return false;
    }

    private void spawnZombiesOverTime() {
        long now = System.currentTimeMillis();

        if (now - lastZombieSpawnTime >= 5000) {
            spawnZombie();
            lastZombieSpawnTime = now;
        }
    }

    private void spawnZombie() {
        int side = random.nextInt(4);
        double x;
        double y;

        switch (side) {
            case 0 -> {
                x = 20;
                y = random.nextInt(560) + 20;
            }
            case 1 -> {
                x = 780;
                y = random.nextInt(560) + 20;
            }
            case 2 -> {
                x = random.nextInt(760) + 20;
                y = 20;
            }
            default -> {
                x = random.nextInt(760) + 20;
                y = 580;
            }
        }

        zombies.add(new Zombie(nextZombieId++, x, y));
    }

    private Player findNearestPlayer(Zombie zombie) {
        Player nearest = null;
        double bestDistance = Double.MAX_VALUE;

        for (Player player : players.values()) {
            double d = distance(player.x, player.y, zombie.x, zombie.y);

            if (d < bestDistance) {
                bestDistance = d;
                nearest = player;
            }
        }

        return nearest;
    }

    private void checkZombieDamage() {
        long now = System.currentTimeMillis();

        for (Player player : players.values()) {
            for (Zombie zombie : zombies) {
                double distance = distance(player.x, player.y, zombie.x, zombie.y);

                if (distance < 25 && now - player.lastDamageTime > 700) {
                    player.health -= 10;
                    player.lastDamageTime = now;

                    if (player.health < 0) {
                        player.health = 0;
                    }
                }
            }
        }
    }

    private void checkMedkitPickup() {
        List<Medkit> pickedMedkits = new ArrayList<>();

        for (Medkit medkit : medkits) {
            for (Player player : players.values()) {
                double distance = distance(player.x, player.y, medkit.x, medkit.y);

                if (distance < 25) {
                    player.health += 5;

                    if (player.health > 100) {
                        player.health = 100;
                    }

                    pickedMedkits.add(medkit);
                    break;
                }
            }
        }

        for (Medkit medkit : pickedMedkits) {
            medkits.remove(medkit);
            spawnMedkit();
        }
    }

    private void spawnMedkit() {
        double x;
        double y;

        do {
            x = 40 + random.nextInt(720);
            y = 40 + random.nextInt(520);
        } while (collidesWithWall(x, y, 12));

        medkits.add(new Medkit(nextMedkitId++, x, y));
    }

    private double distance(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public synchronized String serialize() {
        StringBuilder sb = new StringBuilder();

        sb.append("STATE");

        sb.append("|RIVER,0,")
                .append((int) riverX).append(",")
                .append((int) riverY).append(",")
                .append((int) riverWidth).append(",")
                .append((int) riverHeight);

        sb.append("|OBJECTIVE,0,")
                .append((int) objective.x).append(",")
                .append((int) objective.y).append(",")
                .append(objective.health).append(",")
                .append(Objective.MAX_HEALTH);

        for (Wall wall : walls) {
            sb.append("|WALL,0,")
                    .append((int) wall.x).append(",")
                    .append((int) wall.y).append(",")
                    .append((int) wall.width).append(",")
                    .append((int) wall.height);
        }

        for (Tree tree : trees) {
            sb.append("|TREE,")
                    .append(tree.id).append(",")
                    .append((int) tree.x).append(",")
                    .append((int) tree.y);
        }

        for (Player p : players.values()) {
            sb.append("|PLAYER,")
                    .append(p.id).append(",")
                    .append((int) p.x).append(",")
                    .append((int) p.y).append(",")
                    .append(p.health).append(",")
                    .append(p.ammo);
        }

        for (Zombie z : zombies) {
            sb.append("|ZOMBIE,")
                    .append(z.id).append(",")
                    .append((int) z.x).append(",")
                    .append((int) z.y);
        }

        for (Medkit m : medkits) {
            sb.append("|MEDKIT,")
                    .append(m.id).append(",")
                    .append((int) m.x).append(",")
                    .append((int) m.y);
        }

        for (Shot s : shots) {
            sb.append("|SHOT,0,")
                    .append((int) s.x1).append(",")
                    .append((int) s.y1).append(",")
                    .append((int) s.x2).append(",")
                    .append((int) s.y2);
        }

        return sb.toString();
    }
}
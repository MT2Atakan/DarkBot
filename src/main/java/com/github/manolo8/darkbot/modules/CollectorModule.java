package com.github.manolo8.darkbot.modules;

import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.config.Config;
import com.github.manolo8.darkbot.core.entities.Box;
import com.github.manolo8.darkbot.core.entities.Ship;
import com.github.manolo8.darkbot.core.itf.Module;
import com.github.manolo8.darkbot.core.manager.HeroManager;
import com.github.manolo8.darkbot.core.objects.LocationInfo;
import com.github.manolo8.darkbot.core.utils.Drive;
import com.github.manolo8.darkbot.core.utils.Location;
import com.github.manolo8.darkbot.extensions.features.Feature;
import com.github.manolo8.darkbot.modules.utils.SafetyFinder;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.manolo8.darkbot.Main.API;
import static java.lang.Math.cos;
import static java.lang.StrictMath.sin;

@Feature(name = "Collector", description = "Resource-only collector module. Can cloack.")
public class CollectorModule implements Module {

    private Main main;

    private List<Box> boxes;
    private List<Ship> ships;
    private List<Ship> list1;
    private List<Ship> list2;

    private Config config;

    private HeroManager hero;
    private Drive drive;

    private long invisibleTime;

    public Box current;

    private long waiting;

    protected SafetyFinder safety;
    protected long refreshing;

    private boolean didFirstCheck;
    private long firstCheckTime;

    private int DISTANCE_FROM_DANGEROUS = 1500;

    @Override
    public void install(Main main) {
        this.main = main;
        this.safety = new SafetyFinder(main);

        this.hero = main.hero;
        this.drive = main.hero.drive;

        this.config = main.config;
        this.boxes = main.mapManager.entities.boxes;
        this.ships = main.mapManager.entities.ships;
    }

    @Override
    public void uninstall() {
        safety.uninstall();
    }

    @Override
    public String status() {
        if (current == null) return "Roaming";

        return current.isCollected() ? "Collecting " + current.type + " " + (waiting - System.currentTimeMillis()) + "ms"
                : "Moving to " + current.type;
    }

    @Override
    public boolean canRefresh() {
        return isNotWaiting();
    }

    @Override
    public void tick() {

        if (isNotWaiting() && checkDangerousAndCurrentMap()) {
            main.guiManager.pet.setEnabled(true);
            checkInvisibility();
            checkNearbyShips();
            checkDangerous();

            findBox();

            if (!tryCollectNearestBox() && (!drive.isMoving() || drive.isOutOfMap())) {
                drive.moveRandom();
            }
        }
    }

    private boolean checkDangerousAndCurrentMap() {
        safety.setRefreshing(System.currentTimeMillis() <= refreshing);
        return safety.tick() && checkMap();
    }

    private boolean checkMap() {
        if (this.config.GENERAL.WORKING_MAP != this.hero.map.id && !main.mapManager.entities.portals.isEmpty()) {
            this.main.setModule(new MapModule())
                    .setTarget(this.main.starManager.byId(this.main.config.GENERAL.WORKING_MAP));
            return false;
        }
        return true;
    }

    public boolean isNotWaiting() {
        /*if (!current.isCollected() && main.statsManager.currentBox == current.address) {
            current.setCollected();
            waiting = System.currentTimeMillis() + current.boxInfo.waitTime +
                    hero.timeTo(hero.locationInfo.distance(current)) + 30;
        }*/
        return System.currentTimeMillis() > waiting || current == null || current.removed;
    }

    public boolean tryCollectNearestBox() {

        if (current != null) {
            collectBox();
            return true;
        }

        return false;
    }

    private void collectBox() {
        double distance = hero.locationInfo.distance(current);

        if (distance < 200) {
            drive.stop(false);
            current.clickable.setRadius(800);
            drive.clickCenter(true, current.locationInfo.now);
            current.clickable.setRadius(0);

            current.setCollected();

            waiting = System.currentTimeMillis() + current.boxInfo.waitTime
                    + Math.min(1_000, current.getRetries() * 100) // Add 100ms per retry, max 1 second
                    + hero.timeTo(distance) + 30;
        } else {
            drive.move(current);
        }
    }

    public void checkNearbyShips() {
        if (config.COLLECT.PREVENT_COLLECTING_TOGETHER && firstCheckTime < System.currentTimeMillis() - 5000) {
            if (!didFirstCheck) {
                list1 = main.mapManager.entities.ships.stream().filter(s -> hero.locationInfo.distance(s.locationInfo) <= 300).collect(Collectors.toList());
                firstCheckTime = System.currentTimeMillis();
                didFirstCheck = true;
            }
            else {
                list2 = main.mapManager.entities.ships.stream().filter(s -> hero.locationInfo.distance(s.locationInfo) <= 300).filter(list1::contains).collect(Collectors.toList());
                didFirstCheck = false;
            }
        }
    }

    private void checkDangerous() {
        if (config.COLLECT.STAY_AWAY_FROM_ENEMIES) {

            Location dangerous = findClosestEnemyAndAddToDangerousList();

            if (dangerous != null) stayAwayFromLocation(dangerous);
        }
    }

    public void checkInvisibility() {
        if (config.COLLECT.AUTO_CLOACK
                && !hero.invisible
                && System.currentTimeMillis() - invisibleTime > 60000) {
            invisibleTime = System.currentTimeMillis();
            API.keyboardClick(config.COLLECT.AUTO_CLOACK_KEY);
        }
    }

    private void stayAwayFromLocation(Location awayLocation) {

        Location heroLocation = hero.locationInfo.now;

        double angle = awayLocation.angle(heroLocation);
        double moveDistance = hero.shipInfo.speed;
        double distance = DISTANCE_FROM_DANGEROUS + 100;

        Location target = new Location(
                awayLocation.x - cos(angle) * distance,
                awayLocation.y - sin(angle) * distance
        );

        moveDistance = moveDistance - target.distance(heroLocation);

        if (moveDistance > 0) {

            angle += moveDistance / 3000;

            target.x = awayLocation.x - cos(angle) * distance;
            target.y = awayLocation.y - sin(angle) * distance;
        }

        drive.move(target);
    }

    public void findBox() {
        LocationInfo heroLoc = hero.locationInfo;

        if (config.COLLECT.PREVENT_COLLECTING_TOGETHER && !list2.isEmpty()) {
            Box best = boxes
                    .stream()
                    .filter(this::canCollect)
                    .min(Comparator.<Box>comparingInt(b -> b.boxInfo.priority) [CONTINUE WITH FILTER UNDER 1000 DISTANCE AND PICK ANY RANDOMLY];
            this.current = current == null || best == null || current.isCollected() || isBetter(best) ? best : current;
        }
        else {
            Box best = boxes
                    .stream()
                    .filter(this::canCollect)
                    .min(Comparator.<Box>comparingInt(b -> b.boxInfo.priority)
                            .thenComparingDouble(heroLoc::distance)).orElse(null);
            this.current = current == null || best == null || current.isCollected() || isBetter(best) ? best : current;
        }
    }

    private boolean canCollect(Box box) {
        return box.boxInfo.collect
                && !box.isCollected()
                && drive.canMove(box.locationInfo.now)
                && (!box.type.equals("FROM_SHIP") || main.statsManager.deposit < main.statsManager.depositTotal);
    }

    private Location findClosestEnemyAndAddToDangerousList() {
        for (Ship ship : ships) {
            if (ship.playerInfo.isEnemy()
                    && !ship.invisible
                    && ship.locationInfo.distance(hero) < DISTANCE_FROM_DANGEROUS) {

                if (ship.isInTimer()) {
                    return ship.locationInfo.now;
                } else if (ship.isAttacking(hero)) {
                    ship.setTimerTo(config.GENERAL.RUNNING.REMEMBER_ENEMIES_FOR * 1000);
                    return ship.locationInfo.now;
                }

            }
        }

        return null;
    }

    private boolean isBetter(Box box) {

        double currentDistance = current.locationInfo.distance(hero);
        double newDistance = box.locationInfo.distance(hero);

        return currentDistance > 100 && currentDistance - 150 > newDistance;
    }

}
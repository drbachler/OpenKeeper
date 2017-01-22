/*
 * Copyright (C) 2014-2016 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper.  If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.world.creature.steering;

import com.badlogic.gdx.ai.pfa.GraphPath;
import com.badlogic.gdx.ai.steer.SteeringBehavior;
import com.badlogic.gdx.ai.steer.behaviors.FollowPath;
import com.badlogic.gdx.ai.steer.behaviors.PrioritySteering;
import com.badlogic.gdx.ai.steer.utils.paths.LinePath;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import java.awt.Point;
import toniarts.openkeeper.world.MapLoader;
import toniarts.openkeeper.world.TileData;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.creature.CreatureControl;
import toniarts.openkeeper.world.pathfinding.PathFindable;

/**
 * A steering factory, static methods for constructing steerings for creatures
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class CreatureSteeringCreator {

    private CreatureSteeringCreator() {
    }

    public static SteeringBehavior<Vector2> navigateToPoint(final WorldState worldState, final PathFindable pathFindable, final CreatureControl creature, final Point p) {
        return navigateToPoint(worldState, pathFindable, creature, p, null);
    }

    public static SteeringBehavior<Vector2> navigateToPoint(final WorldState worldState, final PathFindable pathFindable, final CreatureControl creature, final Point p, final Point faceTarget) {
        GraphPath<TileData> outPath = worldState.findPath(WorldState.getTileCoordinates(creature.getSpatial().getWorldTranslation()), p, pathFindable);
        return navigateToPoint(outPath, faceTarget, creature);
    }

    public static SteeringBehavior<Vector2> navigateToPoint(GraphPath<TileData> outPath, final Point faceTarget, final CreatureControl creature) {
        if ((outPath != null && outPath.getCount() > 1) || faceTarget != null) {
//            PrioritySteering<Vector2> prioritySteering = new PrioritySteering(creature, 0.0001f);

            if (outPath != null && outPath.getCount() > 1) {

                PrioritySteering<Vector2> prioritySteering = new PrioritySteering(creature, 0.0001f);

                // Add regular avoidance
//                CollisionAvoidance<Vector2> ca = new CollisionAvoidance<>(creature, new ProximityBase<Vector2>(creature, null) {
//
//                    @Override
//                    public int findNeighbors(Proximity.ProximityCallback<Vector2> callback) {
//                        List<CreatureControl> creatures = new ArrayList<>(creature.getVisibleCreatures());
//                        int neighborCount = 0;
//                        int agentCount = creatures.size();
//                        for (int i = 0; i < agentCount; i++) {
//                            Steerable<Vector2> currentAgent = creatures.get(i);
//
//                            // Skip if this is us, when sharing collission avoidance i.e. this can contain us
//                            if (!currentAgent.equals(owner)) {
//                                if (callback.reportNeighbor(currentAgent)) {
//                                    neighborCount++;
//                                }
//                            }
//                        }
//
//                        return neighborCount;
//                    }
//                });
//                prioritySteering.add(ca);
// Debug
// worldHandler.drawPath(new LinePath<>(pathToArray(outPath)));
// Navigate
                FollowPath<Vector2, LinePath.LinePathParam> followPath = new FollowPath(creature, new LinePath<>(pathToArray(outPath), true), 2);
                followPath.setDecelerationRadius(1f);
                followPath.setArrivalTolerance(0.2f);
                prioritySteering.add(followPath);

                return prioritySteering;
            }

//            if (faceTarget != null) {
//
//                // Add reach orientation
//                ReachOrientation orient = new ReachOrientation(creature, new TargetLocation(new Vector2(faceTarget.x - 0.5f, faceTarget.y - 0.5f), new Vector2(p.x - 0.5f, p.y - 0.5f)));
//                orient.setDecelerationRadius(1.5f);
//                orient.setTimeToTarget(0.001f);
//                orient.setAlignTolerance(0.6f);
//                prioritySteering.add(orient);
//            }
//            return prioritySteering;
        }

        return null;
    }

    private static Array<Vector2> pathToArray(GraphPath<TileData> outPath) {
        Array<Vector2> path = new Array<>(outPath.getCount());
        for (TileData tile : outPath) {
            path.add(new Vector2(
                    MapLoader.TILE_WIDTH * (tile.getX() - 0.5f),
                    MapLoader.TILE_WIDTH * (tile.getY() - 0.5f)));
        }
        return path;
    }

}

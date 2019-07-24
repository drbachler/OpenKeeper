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
package toniarts.openkeeper.game.controller.room.storage;

import com.simsilica.es.EntityId;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Holds out the researchers populating a room
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class RoomResearcherControl extends AbstractRoomObjectControl<EntityId> {

    public RoomResearcherControl(IRoomController parent, IObjectsController objectsController) {
        super(parent, objectsController);
    }

    @Override
    public int getCurrentCapacity() {
        return objectsByCoordinate.size();
    }

    @Override
    protected int getObjectsPerTile() {
        return 1;
    }

    @Override
    public ObjectType getObjectType() {
        return ObjectType.RESEARCHER;
    }

    @Override
    public EntityId addItem(EntityId researcher, Point p) {
        setRoomStorageToItem(researcher, false);
        return researcher;
    }

    @Override
    public void destroy() {

        // TODO: The researcher can't do his/her job
    }

    @Override
    public void captured(short playerId) {

    }

    @Override
    protected Collection<Point> getCoordinates() {

        // Only furniture
        List<Point> coordinates = new ArrayList<>(parent.getFloorFurnitureCount() + parent.getWallFurnitureCount());
        for (EntityId oc : parent.getFloorFurniture()) {
            coordinates.add(WorldUtils.vectorToPoint(objectsController.getEntityData().getComponent(oc, Position.class).position));
        }
        for (EntityId oc : parent.getWallFurniture()) {
            coordinates.add(WorldUtils.vectorToPoint(objectsController.getEntityData().getComponent(oc, Position.class).position));
        }
        return coordinates;
    }

}

/*
 * Copyright (C) 2014-2019 OpenKeeper
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
import toniarts.openkeeper.game.component.RoomStorage;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.tools.convert.map.KwdFile;

/**
 * Controls chickens in a room
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class RoomFoodControl extends AbstractRoomObjectControl<EntityId> {

    private int chickens = 0;

    public RoomFoodControl(KwdFile kwdFile, IRoomController parent, IObjectsController objectsController) {
        super(parent, objectsController);
    }

    @Override
    public int getCurrentCapacity() {
        return chickens;
    }

    @Override
    public ObjectType getObjectType() {
        return ObjectType.FOOD;
    }

    @Override
    public EntityId addItem(EntityId entity, Point p) {
        Collection<EntityId> objects = objectsByCoordinate.get(p);
        if (objects == null) {
            objects = new ArrayList<>();
        }
        objects.add(entity);
        objectsByCoordinate.put(p, objects);
        setRoomStorageToItem(entity, true);
        chickens++;
        return entity;
    }

    @Override
    public void destroy() {

        // We don't destroy the chickens, that would be inhumane, but we set them free
        List<Collection<EntityId>> objectList = new ArrayList<>(objectsByCoordinate.values());
        for (Collection<EntityId> objects : objectList) {
            for (EntityId obj : objects) {
                objectsController.getEntityData().removeComponent(obj, RoomStorage.class);
            }
        }
    }

    @Override
    public void removeItem(EntityId object) {
        super.removeItem(object);
        chickens--;
    }

    @Override
    protected int getObjectsPerTile() {
        return 1;
    }

}

/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.appengine.api.datastore;

import com.google.common.collect.Maps;
import com.google.storage.onestore.v3.OnestoreEntity.EntityProto;
import com.google.storage.onestore.v3.OnestoreEntity.Path;
import com.google.storage.onestore.v3.OnestoreEntity.Reference;
import java.util.Collection;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * {@code EntityTranslator} contains the logic to translate an {@code Entity} into the protocol
 * buffers that are used to pass it to the implementation of the API.
 *
 */
public class EntityTranslator {

  // Note: We'd like to make {@code EntityTranslator} package protected, but
  // we need to use it from {@link LocalDatastoreService}, so it's public.
  // We attempted to move {@code EntityTranslator} (and other classes) into
  // an impl sub-package, but they use package-protected methods on
  // classes such as {@link Entity} and {@link Key} which we can not move.

  public static Entity createFromPb(EntityProto proto, Collection<Projection> projections) {
    Key key = KeyTranslator.createFromPb(proto.getKey());

    Entity entity = new Entity(key);
    Map<String, @Nullable Object> values = Maps.newHashMap();
    DataTypeTranslator.extractPropertiesFromPb(proto, values);
    for (Projection projection : projections) {
      entity.setProperty(projection.getName(), projection.getValue(values));
    }
    return entity;
  }

  public static Entity createFromPb(EntityProto proto) {
    Key key = KeyTranslator.createFromPb(proto.getKey());

    Entity entity = new Entity(key);
    DataTypeTranslator.extractPropertiesFromPb(proto, entity.getPropertyMap());
    return entity;
  }

  public static Entity createFromPbBytes(byte[] pbBytes) {
    EntityProto proto = new EntityProto();
    boolean parsed = proto.mergeFrom(pbBytes);
    if (!parsed || !proto.isInitialized()) {
      throw new IllegalArgumentException("Could not parse EntityProto bytes");
    }
    return createFromPb(proto);
  }

  public static EntityProto convertToPb(Entity entity) {
    Reference reference = KeyTranslator.convertToPb(entity.getKey());

    EntityProto proto = new EntityProto();
    proto.setKey(reference);

    // If we've already been stored, make sure the entity group is set
    // to match our key.
    Path entityGroup = proto.getMutableEntityGroup();
    Key key = entity.getKey();
    if (key.isComplete()) {
      entityGroup.addElement(reference.getPath().elements().get(0));
    }

    DataTypeTranslator.addPropertiesToPb(entity.getPropertyMap(), proto);
    return proto;
  }

  // All methods are static.  Do not instantiate.
  private EntityTranslator() {}
}

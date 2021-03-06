/*
 * Copyright 2016, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.model;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;
import org.openremote.model.asset.AssetMeta;
import org.openremote.model.util.JsonUtil;

import java.util.function.Predicate;

/**
 * A named arbitrary {@link JsonValue}.
 * <p>
 * Name should be a URI, thus avoiding collisions and representing "ownership" of the meta item.
 */
public class MetaItem extends AbstractValueHolder {

    public MetaItem() {
        this(Json.createObject());
    }

    public MetaItem(JsonObject jsonObject) {
        super(jsonObject);
    }

    public MetaItem(AssetMeta assetMeta) {
        this(assetMeta, null);
    }

    public MetaItem(String name) {
        this(name, null);
    }

    public MetaItem(AssetMeta assetMeta, JsonValue value) {
        this(assetMeta.getUrn(), value);
    }

    public MetaItem(String name, JsonValue value) {
        super(Json.createObject());
        setName(name);
        setValue(value);
    }

    public JsonObject getJsonObject() {
        return jsonObject;
    }

    public String getName() {
        return jsonObject.hasKey("name") ? jsonObject.get("name").asString() : null;
    }

    public MetaItem setName(String name) {
        jsonObject.put("name", name);
        return this;
    }

    public MetaItem copy() {
        return new MetaItem(Json.parse(getJsonObject().toJson()));
    }

    public boolean isValid() {
        return getName() != null && getName().length() > 0 && hasValue();
    }

    public static Predicate<MetaItem> matches(MetaItem item) {
        return matches(item.getName(), item.getValue());
    }

    public static Predicate<MetaItem> matches(String name) {
        return item -> item.getName().equals(name);
    }

    public static Predicate<MetaItem> matches(String name, JsonValue value) {
        return item -> item.getName().equals(name) && item.getValue() != null && JsonUtil.equals(item.getValue(), value);
    }

    public static <T extends JsonValue> Predicate<MetaItem> matches(String name, Class<T> clazz) {
        return item -> item.getName().equals(name) && item.getValue() != null && item.getValue().getClass().getSuperclass() == clazz;
    }
}

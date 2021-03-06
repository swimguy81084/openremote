/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.model.asset;

import elemental.json.Json;
import org.openremote.model.AttributeType;
import org.openremote.model.Meta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.openremote.model.AttributeType.INTEGER;
import static org.openremote.model.AttributeType.STRING;
import static org.openremote.model.Constants.ASSET_NAMESPACE;
import static org.openremote.model.asset.AssetMeta.*;

/**
 * Asset type is an arbitrary string. It should be URI. This enum contains
 * the well-known URIs for functionality we want to depend on in our platform.
 * <p>
 * TODO https://people.eecs.berkeley.edu/~arka/papers/buildsys2015_metadatasurvey.pdf
 */
public enum AssetType {

    CUSTOM(null, "cube", null),

    BUILDING(ASSET_NAMESPACE + ":building", "building", Arrays.asList(
        new AssetAttribute("area", INTEGER)
            .setMeta(new Meta()
            .add(createMetaItem(LABEL, Json.create("Surface area")))
            .add(createMetaItem(DESCRIPTION, Json.create("Floor area of building measured in m²")))
            .add(createMetaItem(ABOUT, Json.create("http://project-haystack.org/tag/area")))
        ),
        new AssetAttribute("geoStreet", STRING)
            .setMeta(new Meta()
            .add(createMetaItem(LABEL, Json.create("Street")))
            .add(createMetaItem(ABOUT, Json.create("http://project-haystack.org/tag/geoStreet")))
        ),
        new AssetAttribute("geoPostalCode", AttributeType.INTEGER)
            .setMeta(new Meta()
            .add(createMetaItem(LABEL, Json.create("Postal Code")))
            .add(createMetaItem(ABOUT, Json.create("http://project-haystack.org/tag/geoPostalCode")))
        ),
        new AssetAttribute("geoCity", STRING)
            .setMeta(new Meta()
            .add(createMetaItem(LABEL, Json.create("City")))
            .add(createMetaItem(ABOUT, Json.create("http://project-haystack.org/tag/geoCity")))
        ),
        new AssetAttribute("geoCountry", STRING)
            .setMeta(new Meta()
            .add(createMetaItem(LABEL, Json.create("Country")))
            .add(createMetaItem(ABOUT, Json.create("http://project-haystack.org/tag/geoCountry")))
        ))),

    FLOOR(ASSET_NAMESPACE + ":floor", "server", null),

    RESIDENCE(ASSET_NAMESPACE + ":residence", "cubes", null),

    ROOM(ASSET_NAMESPACE + ":room", "cube", null),

    AGENT(ASSET_NAMESPACE + ":agent", "gears", null),

    /**
     * When a Thing asset is modified (created, updated, deleted), its attributes are examined
     * and linked to and unlinked from the configured Protocol.
     */
    THING(ASSET_NAMESPACE + ":thing", "gear", null);

    final protected String value;
    final protected String icon;
    final protected List<AssetAttribute> defaultAttributes;

    AssetType(String value, String icon, List<AssetAttribute> defaultAttributes) {
        this.value = value;
        this.icon = icon;
        this.defaultAttributes = defaultAttributes;
    }

    public String getValue() {
        return value;
    }

    public String getIcon() {
        return icon;
    }

    public Stream<AssetAttribute> getDefaultAttributes() {
        return defaultAttributes != null ? defaultAttributes.stream() : Stream.empty();
    }

    public static AssetType[] valuesSorted() {
        List<AssetType> list = new ArrayList<>(Arrays.asList(values()));

        list.sort(Comparator.comparing(Enum::name));
        if (list.contains(CUSTOM)) {
            // CUSTOM should be first
            list.remove(CUSTOM);
            list.add(0, CUSTOM);
        }

        return list.toArray(new AssetType[list.size()]);
    }

    public static AssetType getByValue(String value) {
        if (value == null)
            return CUSTOM;
        for (AssetType assetType : values()) {
            if (value.equals(assetType.getValue()))
                return assetType;
        }
        return CUSTOM;
    }
}

/*
 * EasyAsPi: A phone-based interface for the Raspberry Pi.
 * Copyright (C) 2017  vtcakavsmoace
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Raspberry Pi is a trademark of the Raspberry Pi Foundation.
 */

package io.github.trulyfree.easyaspi.lib.module.conf;

/**
 * Config for dependencies and the superclass of the Config for Modules.
 *
 * @see io.github.trulyfree.easyaspi.lib.module.conf.ModuleConfig
 *
 * @author vtcakavsmoace
 * @since v0.0.1-alpha
 */
public class Config {
    /**
     * The name attributed to this item.
     */
    private String name;

    /**
     * The URL of the jar for this item.
     */
    private String jarUrl;

    /**
     * Standard constructor for the Config, which just defines initial non-null values for fields.
     */
    public Config() {
        this.name = "";
        this.jarUrl = "";
    }

    /**
     * Returns the name attributed to this item.
     *
     * @return name The name attributed to this item.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name attributed to this item.
     *
     * @param name The name attributed to this item.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the URL of the jar for this item.
     *
     * @return jarUrl The URL of the jar for this item.
     */
    public String getJarUrl() {
        return jarUrl;
    }

    /**
     * Sets the URL of the jar for this item.
     *
     * @param jarUrl The URL of the jar for this item.
     */
    public void setJarUrl(String jarUrl) {
        this.jarUrl = jarUrl;
    }
}

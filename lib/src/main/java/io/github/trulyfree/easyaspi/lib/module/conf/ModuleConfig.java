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
 * Config for Modules, which must exist jsonified for Module downloads.
 *
 * @author vtcakavsmoace
 * @since v0.0.1-alpha
 */
public class ModuleConfig extends Config {
    /**
     * The version of this module.
     */
    private String version;

    /**
     * The URL for the configuration file of this module.
     */
    private String confUrl;

    /**
     * The fully qualified classname of the target displayable module.
     */
    private String targetModule;

    /**
     * An array of configs for the dependencies of this module.
     */
    private Config[] dependencies;

    /**
     * Standard constructor for the ModuleConfig, which just defines initial non-null values for fields.
     */
    public ModuleConfig() {
        this.version = "";
        this.confUrl = "";
        this.targetModule = "";
        this.dependencies = new Config[0];
    }

    /**
     * Returns the version of this module.
     *
     * @return version The version of this module.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the version of this module.
     *
     * @param version The version of this module.
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Returns the URL for the configuration file of this module.
     *
     * @return confUrl The URL for the configuration file of this module.
     */
    public String getConfUrl() {
        return confUrl;
    }

    /**
     * Sets the URL for the configuration file of this module.
     *
     * @param confUrl The URL for the configuration file of this module.
     */
    public void setConfUrl(String confUrl) {
        this.confUrl = confUrl;
    }

    /**
     * Returns the fully qualified classname of the target displayable module.
     *
     * @return targetModule The fully qualified classname of the target displayable module.
     */
    public String getTargetModule() {
        return targetModule;
    }

    /**
     * Set the fully qualified classname of the target displayable module.
     *
     * @param targetModule The fully qualified classname of the target displayable module.
     */
    public void setTargetModule(String targetModule) {
        this.targetModule = targetModule;
    }

    /**
     * Returns the array of configs for the dependencies of this module.
     *
     * @return dependencies An array of configs for the dependencies of this module.
     */
    public Config[] getDependencies() {
        return dependencies;
    }

    /**
     * Sets the array of configs for the dependencies of this module.
     *
     * @param dependencies An array of configs for the dependencies of this module.
     */
    public void setDependencies(Config[] dependencies) {
        this.dependencies = dependencies;
    }
}

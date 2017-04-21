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

public class ModuleConfig extends Config {
    private String version;
    private String confUrl;
    private String targetModule;
    private Config[] dependencies;

    public ModuleConfig() {
        this.version = "";
        this.confUrl = "";
        this.targetModule = "";
        this.dependencies = new Config[0];
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getConfUrl() {
        return confUrl;
    }

    public void setConfUrl(String confUrl) {
        this.confUrl = confUrl;
    }

    public String getTargetModule() {
        return targetModule;
    }

    public void setTargetModule(String targetModule) {
        this.targetModule = targetModule;
    }

    public Config[] getDependencies() {
        return dependencies;
    }

    public void setDependencies(Config[] dependencies) {
        this.dependencies = dependencies;
    }
}

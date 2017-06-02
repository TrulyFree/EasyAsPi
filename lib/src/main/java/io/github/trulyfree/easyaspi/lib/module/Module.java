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

package io.github.trulyfree.easyaspi.lib.module;

/**
 * Interface which all Module-based classes must implement.
 *
 * @author vtcakavsmoace
 * @since v0.0.1-alpha
 */
public interface Module {
    /**
     * Sets up this module. There will likely be prerequisite method calls.
     *
     * @return success The success of the setup operation.
     */
    public boolean setup();

    /**
     * Checks whether or not the Module is ready for use.
     *
     * @return ready The readiness of this module.
     */
    public boolean isReady();

    /**
     * Terminates this module. The module's methods should NOT be invoked after this is called.
     *
     * @return success The success of the destroy operation.
     */
    public boolean destroy();
}

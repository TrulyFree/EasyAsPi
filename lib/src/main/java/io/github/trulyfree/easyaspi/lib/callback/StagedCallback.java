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

package io.github.trulyfree.easyaspi.lib.callback;

/**
 * A Callback interface extension which defines the <code>setStages</code> method. This allows
 * sections of progress to be defined. Note that <code>onStart</code>, <code>onProgress</code> and
 * <code>onFinish</code> methods should be called on the start, progress, and finish of each stage,
 * respectively.
 *
 * @author vtcakavsmoace
 * @since v0.0.1-alpha
 */
public interface StagedCallback extends Callback {
    /**
     * Method to set the names of all the stages.
     * @param names Names of the stages.
     */
    public void setStages(String[] names);
}

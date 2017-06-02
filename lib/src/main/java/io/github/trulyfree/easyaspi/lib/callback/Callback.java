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
 * Simple callback for progress monitoring.
 *
 * @author vtcakavsmoace
 * @since v0.0.1-alpha
 */
public interface Callback {

    /**
     * To be called on startup of the target method by the target method.
     */
    void onStart();

    /**
     * To be called on definable progress of the target method by the target method.
     *
     * @param current The current progress of the target method (by percentage completion).
     */
    void onProgress(int current);

    /**
     * To be called on termination of the target method by the target method.
     */
    void onFinish();

}

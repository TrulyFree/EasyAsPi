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

package io.github.trulyfree.easyaspi.lib.disp;

import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;

import java.util.concurrent.ExecutorService;

import io.github.trulyfree.easyaspi.lib.EAPActivity;
import io.github.trulyfree.easyaspi.lib.module.Module;

/**
 * Interface to be implemented by all displayable modules used in EasyAsPi.
 *
 * @author vtcakavsmoace
 * @since v0.0.1-alpha
 */
public interface EAPDisplayableModule extends Module {
    /**
     * Sets the activity of the target module. This will always be called before <code>setup</code>.
     *
     * @param activity The current EAPDisplay instance.
     */
    public void setActivity(EAPDisplay activity);

    /**
     * Returns the current EAPDisplay instance assigned to this module.
     *
     * @return activity The current EAPDisplay instance.
     */
    public EAPDisplay getActivity();

    /**
     * Sets the executor service of the target module. This executor service does not necessarily
     * have to be used by the target module.
     *
     * @param executorService The executor service intended for the target module.
     */
    public void setExecutorService(ExecutorService executorService);

    /**
     * Returns the executor service of the target module.
     *
     * @return executorService The executor service of the target module.
     */
    public ExecutorService getExecutorService();

    /**
     * Returns the root view to be used by the activity, according to the Intent.
     *
     * @param data The Intent that was passed to the current EAPDisplay instance.
     * @return root The root view of the target module.
     */
    public View getRootView(Intent data);

    /**
     * Returns the LayoutParams of the target module. This is almost expected to be null.
     *
     * @return layoutParams The LayoutParams to be used for the root view by the current activity.
     */
    public ViewGroup.LayoutParams getLayoutParams();
}

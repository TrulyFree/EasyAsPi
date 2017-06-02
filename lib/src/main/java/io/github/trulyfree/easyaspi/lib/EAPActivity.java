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

package io.github.trulyfree.easyaspi.lib;

import android.content.Context;

import java.io.File;
import java.util.concurrent.ExecutorService;

import io.github.trulyfree.easyaspi.lib.disp.EAPDisplayableModule;
import io.github.trulyfree.easyaspi.lib.dl.DownloadHandler;
import io.github.trulyfree.easyaspi.lib.io.FileHandler;
import io.github.trulyfree.easyaspi.lib.module.Module;
import io.github.trulyfree.easyaspi.lib.module.ModuleHandler;

/**
 * The interface which all EAPActivities must extend. This is defined to allow mockable activities.
 *
 * @author vtcakavsmoace
 * @since v0.0.1-alpha
 */
public interface EAPActivity extends Module {
    /**
     * Returns the download handler owned by this activity.
     *
     * @return handler The download handler owned by this activity.
     */
    public DownloadHandler getDownloadHandler();

    /**
     * Returns the file handler owned by this activity.
     *
     * @return handler The file handler owned by this activity.
     */
    public FileHandler getFileHandler();

    /**
     * Returns the module handler owned by this activity.
     *
     * @return handler The module handler owned by this activity.
     */
    public ModuleHandler getModuleHandler();

    /**
     * Returns the executor service owned by this activity.
     *
     * @return executorService The executor service owned by this activity.
     */
    public ExecutorService getExecutorService();

    /**
     * Returns the module currently displayed by this activity.
     *
     * @return module The module currently displayed by this activity.
     */
    public EAPDisplayableModule getDisplayableModule();

    /**
     * Sets the module currently displayed by this activity.
     *
     * @param displayableModule The module currently displayed by this activity.
     * @return success If the module was successfully loaded by the activity.
     */
    public boolean setDisplayableModule(EAPDisplayableModule displayableModule);

    /**
     * @see android.app.Application#getDir(String, int)
     */
    public File getDir(String appdir, int modePrivate);

    /**
     * @see android.app.Application#getClassLoader()
     */
    public ClassLoader getClassLoader();

    /**
     * @see android.app.Activity#runOnUiThread(Runnable)
     */
    public void runOnUiThread(Runnable runnable);

    /**
     * @see android.widget.Toast#makeText(Context, CharSequence, int)
     */
    public void displayToUser(String text, int time);
}

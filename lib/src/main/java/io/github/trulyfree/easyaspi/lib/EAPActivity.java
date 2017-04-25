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

import android.support.v7.app.AppCompatActivity;

import io.github.trulyfree.easyaspi.lib.dl.DownloadHandler;
import io.github.trulyfree.easyaspi.lib.io.FileHandler;
import io.github.trulyfree.easyaspi.lib.module.ModuleHandler;
import io.github.trulyfree.modular6.action.Action;
import io.github.trulyfree.modular6.action.handlers.ActionHandler;

public abstract class EAPActivity extends AppCompatActivity {
    public abstract DownloadHandler getDownloadHandler();
    public abstract FileHandler getFileHandler();
    public abstract ModuleHandler getModuleHandler();
    public abstract ActionHandler<Action> getActionHandler();
}

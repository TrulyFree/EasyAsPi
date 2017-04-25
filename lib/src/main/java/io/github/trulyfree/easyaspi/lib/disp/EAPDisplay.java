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
import android.os.Bundle;

import java.util.Collection;

import io.github.trulyfree.easyaspi.lib.EAPActivity;
import io.github.trulyfree.easyaspi.lib.dl.DownloadHandler;
import io.github.trulyfree.easyaspi.lib.io.FileHandler;
import io.github.trulyfree.easyaspi.lib.module.ModuleHandler;
import io.github.trulyfree.easyaspi.lib.module.conf.ModuleConfig;
import io.github.trulyfree.modular6.action.Action;
import io.github.trulyfree.modular6.action.handlers.ActionHandler;
import io.github.trulyfree.modular6.action.handlers.BackgroundGeneralizedActionHandler;
import io.github.trulyfree.modular6.display.Display;
import io.github.trulyfree.modular6.display.DisplayableModule;
import io.github.trulyfree.modular6.display.except.DisplayableException;

public final class EAPDisplay extends EAPActivity implements Display<EAPDisplayable> {

    private DownloadHandler downloadHandler;
    private FileHandler fileHandler;
    private ModuleHandler moduleHandler;
    private ActionHandler<Action> actionHandler;

    private EAPDisplayableModule currentModule;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!setup()) {
            finish();
        }
    }

    @Override
    public EAPDisplayableModule getDisplayableModule() {
        return currentModule;
    }

    @Override
    public boolean setDisplayableModule(DisplayableModule<EAPDisplayable> displayableModule) throws DisplayableException {
        EAPDisplayable target = null;
        if (displayableModule instanceof EAPDisplayableModule) {
            EAPDisplayableModule targetModule = (EAPDisplayableModule) displayableModule;
            try {
                targetModule.setActivity(this);
                targetModule.setup();
                Collection<EAPDisplayable> displayables = displayableModule.getDisplayables();
                target = displayables.iterator().next();
                if (target.getLayoutParams() == null) {
                    setContentView(target.getRootView());
                } else {
                    setContentView(target.getRootView(), target.getLayoutParams());
                }
                this.currentModule = (EAPDisplayableModule) displayableModule;
                this.currentModule.setActivity(this);
            } catch (Exception e) {
                if (target == null) {
                    e.printStackTrace();
                    throw new NullPointerException("No EAPDisplayable provided.");
                } else {
                    throw new DisplayableException(e, target);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean setup() {
        Intent intent = this.getIntent();
        Bundle extras = intent.getExtras();
        if (extras == null) {
            Intent returned = new Intent();
            returned.putExtra("error", "No extras were sent as part of the launching intent.");
            setResult(RESULT_CANCELED, returned);
            return false;
        }
        this.moduleHandler = new ModuleHandler(this);
        this.fileHandler = new FileHandler(this);
        this.downloadHandler = new DownloadHandler(this);
        this.actionHandler = new BackgroundGeneralizedActionHandler((byte) (Runtime.getRuntime().availableProcessors() * 2));
        moduleHandler.setup();
        actionHandler.setup();
        actionHandler.enact();
        try {
            String config = extras.getString("targetModule");
            System.out.println(config);
            ModuleConfig moduleConfig = moduleHandler.fromJson(config);
            EAPDisplayableModule module = moduleHandler.loadModule(moduleConfig);
            this.setDisplayableModule(module);
        } catch (DisplayableException e) {
            e.printStackTrace();
            Intent returned = new Intent();
            returned.putExtra("error", "Module failed to be rendered.");
            setResult(RESULT_CANCELED, returned);
            return false;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            Intent returned = new Intent();
            returned.putExtra("error", "Module was not accessible.");
            setResult(RESULT_CANCELED, returned);
            return false;
        } catch (InstantiationException e) {
            e.printStackTrace();
            Intent returned = new Intent();
            returned.putExtra("error", "Module was not instantiable (check constructor).");
            setResult(RESULT_CANCELED, returned);
            return false;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            Intent returned = new Intent();
            returned.putExtra("error", "Class defined by module was not found.");
            setResult(RESULT_CANCELED, returned);
            return false;
        } catch (NullPointerException e) {
            e.printStackTrace();
            Intent returned = new Intent();
            returned.putExtra("error", "No module information sent.");
            setResult(RESULT_CANCELED, returned);
            return false;
        }
        return true;
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public boolean destroy() {
        setResult(RESULT_OK);
        finish();
        return true;
    }

    public DownloadHandler getDownloadHandler() {
        return downloadHandler;
    }

    public FileHandler getFileHandler() {
        return fileHandler;
    }

    public ModuleHandler getModuleHandler() {
        return moduleHandler;
    }

    @Override
    public ActionHandler<Action> getActionHandler() {
        return actionHandler;
    }
}

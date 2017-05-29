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
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.trulyfree.easyaspi.lib.EAPActivity;
import io.github.trulyfree.easyaspi.lib.dl.DownloadHandler;
import io.github.trulyfree.easyaspi.lib.io.FileHandler;
import io.github.trulyfree.easyaspi.lib.module.ModuleHandler;
import io.github.trulyfree.easyaspi.lib.module.conf.ModuleConfig;

public final class EAPDisplay extends AppCompatActivity implements EAPActivity {

    private DownloadHandler downloadHandler;
    private FileHandler fileHandler;
    private ModuleHandler moduleHandler;
    private ExecutorService executorService;

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
    public boolean setDisplayableModule(EAPDisplayableModule displayableModule) {
        try {
            displayableModule.setActivity(this);
            displayableModule.setup();
            if (displayableModule.getLayoutParams() == null) {
                setContentView(displayableModule.getRootView());
            } else {
                setContentView(displayableModule.getRootView(), displayableModule.getLayoutParams());
            }
            this.currentModule = displayableModule;
            this.currentModule.setActivity(this);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public void displayToUser(String text, int time) {
        Toast.makeText(this, text, time).show();
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
        this.executorService = Executors.newCachedThreadPool();
        moduleHandler.setup();
        try {
            String config = extras.getString("targetModule");
            System.out.println(config);
            ModuleConfig moduleConfig = moduleHandler.fromJson(config);
            EAPDisplayableModule module = moduleHandler.loadModule(moduleConfig);
            this.setDisplayableModule(module);
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
    public ExecutorService getExecutorService() {
        return executorService;
    }

}

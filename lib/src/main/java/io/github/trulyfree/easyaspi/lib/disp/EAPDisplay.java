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
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.trulyfree.easyaspi.lib.EAPActivity;
import io.github.trulyfree.easyaspi.lib.dl.DownloadHandler;
import io.github.trulyfree.easyaspi.lib.io.FileHandler;
import io.github.trulyfree.easyaspi.lib.module.ModuleHandler;
import io.github.trulyfree.easyaspi.lib.module.conf.ModuleConfig;

/**
 * Implementation of EAPActivity which is guaranteed to be the implementation passed to modules on
 * startup.
 *
 * @author vtcakavsmoace
 * @since v0.0.1-alpha
 */
public final class EAPDisplay extends AppCompatActivity implements EAPActivity {

    /**
     * DownloadHandler of this EAPActivity implementation.
     */
    private volatile DownloadHandler downloadHandler;

    /**
     * FileHandler of this EAPActivity implementation.
     */
    private volatile FileHandler fileHandler;

    /**
     * ModuleHandler of this EAPActivity implementation.
     */
    private volatile ModuleHandler moduleHandler;

    /**
     * ExecutorService of this EAPActivity implementation.
     */
    private volatile ExecutorService executorService;

    /**
     * Current module held by this EAPActivity implementation.
     */
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
    public boolean setDisplayableModule(final EAPDisplayableModule displayableModule) {
        try {
            displayableModule.setActivity(this);
            displayableModule.setExecutorService(executorService);
            displayableModule.setup();
            final Object lock = new Object();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (displayableModule.getLayoutParams() == null) {
                        setContentView(displayableModule.getRootView(getIntent()));
                    } else {
                        setContentView(displayableModule.getRootView(getIntent()), displayableModule.getLayoutParams());
                    }
                    synchronized (lock) {
                        lock.notifyAll();
                    }
                }
            });
            synchronized (lock) {
                lock.wait();
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
    public void displayToUser(final String text, final int time) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(EAPDisplay.this, text, time).show();
            }
        });
    }

    @Override
    public boolean setup() {
        Intent intent = this.getIntent();
        final Bundle extras = intent.getExtras();
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
        executorService.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                boolean success = true;
                try {
                    String config = extras.getString("targetModule");
                    System.out.println(config);
                    ModuleConfig moduleConfig = moduleHandler.fromJson(config);
                    EAPDisplayableModule module = moduleHandler.loadModule(moduleConfig);
                    setDisplayableModule(module);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    Intent returned = new Intent();
                    returned.putExtra("error", "Module was not accessible.");
                    setResult(RESULT_CANCELED, returned);
                    success = false;
                } catch (InstantiationException e) {
                    e.printStackTrace();
                    Intent returned = new Intent();
                    returned.putExtra("error", "Module was not instantiable (check constructor).");
                    setResult(RESULT_CANCELED, returned);
                    success = false;
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    Intent returned = new Intent();
                    returned.putExtra("error", "Class defined by module was not found.");
                    setResult(RESULT_CANCELED, returned);
                    success = false;
                } catch (NullPointerException e) {
                    e.printStackTrace();
                    Intent returned = new Intent();
                    returned.putExtra("error", "No module information sent.");
                    setResult(RESULT_CANCELED, returned);
                    success = false;
                }
                if (!success) {
                    finish();
                }
                return success;
            }
        });
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (currentModule != null && currentModule instanceof PreferenceManager.OnActivityResultListener) {
            ((PreferenceManager.OnActivityResultListener) currentModule).onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public DownloadHandler getDownloadHandler() {
        return downloadHandler;
    }

    @Override
    public FileHandler getFileHandler() {
        return fileHandler;
    }

    @Override
    public ModuleHandler getModuleHandler() {
        return moduleHandler;
    }

    @Override
    public ExecutorService getExecutorService() {
        return executorService;
    }

}

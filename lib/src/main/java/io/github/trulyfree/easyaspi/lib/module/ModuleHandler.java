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

import android.content.Context;
import android.support.annotation.NonNull;

import com.android.dx.command.dexer.Main;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Stack;

import dalvik.system.DexClassLoader;
import io.github.trulyfree.easyaspi.lib.EAPActivity;
import io.github.trulyfree.easyaspi.lib.callback.StagedCallback;
import io.github.trulyfree.easyaspi.lib.disp.EAPDisplayableModule;
import io.github.trulyfree.easyaspi.lib.dl.DownloadHandler;
import io.github.trulyfree.easyaspi.lib.io.FileHandler;
import io.github.trulyfree.easyaspi.lib.module.conf.Config;
import io.github.trulyfree.easyaspi.lib.module.conf.ModuleConfig;
import io.github.trulyfree.modular6.module.Module;

public class ModuleHandler implements Module {
    private final EAPActivity activity;
    private ModuleConfig[] configs;
    private Gson gson;
    private File configDir, undexedDir, dexedJar, optimizedDexDir;
    private DexClassLoader classLoader;

    public ModuleHandler(@NonNull EAPActivity activity) {
        this.activity = activity;
    }

    public ModuleConfig getModuleConfig(@NonNull String configUrl) throws IOException, JsonParseException {
        DownloadHandler downloadHandler = activity.getDownloadHandler();
        String stringConfig = downloadHandler.download(null, configUrl);
        System.out.println("Downloaded JSON: " + stringConfig);
        return gson.fromJson(stringConfig, ModuleConfig.class);
    }

    public boolean getNewModule(StagedCallback callback, @NonNull ModuleConfig config) throws IOException, JsonParseException {
        DownloadHandler downloadHandler = activity.getDownloadHandler();
        FileHandler fileHandler = activity.getFileHandler();

        Stack<File> writtenFiles = new Stack<>();

        try {
            String stringConfig = gson.toJson(config);
            writtenFiles.push(fileHandler.generateFile("config", config.getName() + ".json"));
            if (writtenFiles.peek().exists()) {
                writtenFiles.peek().delete();
            }
            fileHandler.writeFile(stringConfig, null, writtenFiles.peek());

            if (callback != null) {
                String[] stages = new String[config.getDependencies().length + 1];

                stages[0] = "Getting main jar (" + config.getName() + ")...";
                StringBuilder stringBuilder;
                for (int i = 1; i < stages.length; i++) {
                    stringBuilder = new StringBuilder("Getting dependency ");
                    stringBuilder.append(config.getDependencies()[i - 1].getName());
                    stringBuilder.append(" (");
                    stringBuilder.append(i);
                    stringBuilder.append("/");
                    stringBuilder.append(stages.length - 1);
                    stringBuilder.append(")...");
                    stages[i] = stringBuilder.toString();
                }
                callback.setStages(stages);
            }

            writtenFiles.push(fileHandler.generateFile("undexed", config.getName() + ".jar"));
            if (writtenFiles.peek().exists()) {
                writtenFiles.peek().delete();
            }
            fileHandler.writeFile(downloadHandler.getDownloadStream(config.getJarUrl()),
                    callback,
                    false,
                    writtenFiles.peek());

            for (Config dependency : config.getDependencies()) {
                File dependencyFile = fileHandler.generateFile("undexed", dependency.getName() + ".jar");
                if (dependencyFile.exists()) {
                    dependencyFile.delete();
                }
                writtenFiles.push(dependencyFile);
                fileHandler.writeFile(downloadHandler.getDownloadStream(dependency.getJarUrl()),
                        callback,
                        false,
                        writtenFiles.peek());
            }
            refreshConfigs();
            refreshDexed();
        } catch (IOException e) {
            for (File file : writtenFiles) {
                if (file.exists()) {
                    file.delete();
                }
            }
            throw e;
        }
        return true;
    }

    public boolean remove(StagedCallback callback, @NonNull ModuleConfig config) throws IOException {
        FileHandler fileHandler = activity.getFileHandler();
        if (fileHandler.generateFile("config", config.getName() + ".json").delete() &&
                fileHandler.generateFile("undexed", config.getName() + ".jar").delete()) {
            refreshAll(callback);
            return true;
        } else {
            return false;
        }
    }

    private void clearUntrackedJars() {
        FileHandler fileHandler = activity.getFileHandler();
        ArrayList<String> jarStrings = new ArrayList<>();
        for (String file : activity.getDir("undexed", Context.MODE_PRIVATE).list()) {
            jarStrings.add(file);
        }
        for (ModuleConfig config : getConfigs()) {
            jarStrings.remove(config.getName() + ".jar");
            for (Config dependency : config.getDependencies()) {
                jarStrings.remove(dependency.getName() + ".jar");
            }
        }
        for (String jar : jarStrings) {
            fileHandler.generateFile("undexed", jar).delete();
        }
    }

    public void refreshAll(final StagedCallback callback) throws IOException, JsonParseException {
        refreshConfigs();
        File jardir = activity.getDir("undexed", Context.MODE_PRIVATE);
        File backupJarFolder = activity.getDir("undexed_backup", Context.MODE_PRIVATE);
        if (backupJarFolder.exists()) {
            activity.getFileHandler().deleteFile(backupJarFolder);
        }
        jardir.renameTo(backupJarFolder);
        jardir.mkdirs();
        try {
            String[] stages = new String[configs.length];
            for (int i = 0; i < stages.length; i++) {
                stages[i] = "Getting module " + configs[i].getName();
            }
            if (callback != null) {
                callback.setStages(stages);
            }
            for (ModuleConfig config : configs) {
                if (callback != null) {
                    callback.onStart();
                }
                getNewModule(new StagedCallback() {
                    int stageCount = 1, current = 0;

                    @Override
                    public void setStages(String[] names) {
                        stageCount = names.length;
                    }

                    @Override
                    public void onStart() {
                        // Do nothing.
                    }

                    @Override
                    public void onProgress(int current) {
                        int numerator = this.current * 100 + current;
                        int denominator = this.stageCount;
                        if (callback != null) {
                            callback.onProgress(numerator / denominator);
                        }
                    }

                    @Override
                    public void onFinish() {
                        current++;
                    }
                }, config);
                if (callback != null) {
                    callback.onFinish();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            activity.getFileHandler().deleteFile(jardir);
            backupJarFolder.renameTo(jardir);
            throw e;
        }
        refreshDexed();
    }

    public ModuleConfig fromJson(String json) throws JsonParseException {
        return gson.fromJson(json, ModuleConfig.class);
    }

    public String toJson(ModuleConfig json) {
        return gson.toJson(json);
    }

    public EAPDisplayableModule loadModule(ModuleConfig config)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        System.out.println("Loading JSON: " + toJson(config));
        return (EAPDisplayableModule) instantiate(config.getTargetModule());
    }

    public Object instantiate(String classname) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        return classLoader.loadClass(classname).newInstance();
    }

    @Override
    public boolean setup() {
        gson = new Gson();

        configDir = activity.getDir("config", Context.MODE_PRIVATE);
        undexedDir = activity.getDir("undexed", Context.MODE_PRIVATE);
        dexedJar = activity.getFileHandler().generateFile("dexed", "classes.jar");
        optimizedDexDir = activity.getDir("optdex", Context.MODE_PRIVATE);

        configDir.mkdirs();
        dexedJar.getParentFile().mkdirs();
        undexedDir.mkdirs();
        optimizedDexDir.mkdirs();

        try {
            refreshConfigs();
            refreshDexed();
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public boolean isReady() {
        return configs != null &&
                gson != null &&
                classLoader != null &&
                configDir != null &&
                undexedDir != null &&
                dexedJar != null &&
                optimizedDexDir != null;
    }

    @Override
    public boolean destroy() {
        configs = null;
        gson = null;
        classLoader = null;
        configDir = null;
        undexedDir = null;
        dexedJar = null;
        optimizedDexDir = null;
        return true;
    }

    private void refreshConfigs() throws IOException {
        FileHandler fileHandler = activity.getFileHandler();
        File[] configFiles = configDir.listFiles();
        configs = new ModuleConfig[configFiles.length];
        for (int i = 0; i < configFiles.length; i++) {
            configs[i] = gson.fromJson(fileHandler.readFile(null, configFiles[i]), ModuleConfig.class);
        }
    }

    private void refreshDexed() throws IOException {
        if (dexedJar.exists()) {
            dexedJar.delete();
        }
        dexedJar.getParentFile().mkdirs();

        clearUntrackedJars();

        if (undexedDir.list().length == 0) {
            return;
        }

        ArrayList<String> list = new ArrayList<>();
        list.add("--keep-classes");
        list.add("--output=" + dexedJar.getAbsolutePath());

        for (File file : undexedDir.listFiles()) {
            if (file.getAbsolutePath().endsWith(".jar")) {
                list.add(file.getAbsolutePath());
            }
        }

        Main.main(list.toArray(new String[list.size()]));

        classLoader = new DexClassLoader(activity.getFileHandler().generateFile("dexed", "classes.jar").getAbsolutePath(),
                activity.getDir("optdex", Context.MODE_PRIVATE).getAbsolutePath(),
                null,
                activity.getClassLoader());
    }

    public ModuleConfig[] getConfigs() {
        return configs;
    }
}

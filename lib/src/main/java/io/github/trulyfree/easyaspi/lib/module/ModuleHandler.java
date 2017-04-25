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
import android.widget.Toast;

import com.android.dx.command.dexer.Main;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Stack;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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
    private File configDir, jarDir, undexedDir, dexedJar, optimizedDexDir;
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

    public boolean getNewModule(StagedCallback callback, @NonNull ModuleConfig config, Stack<String> alreadyDownloaded, boolean refreshDexed) throws IOException, JsonParseException {
        DownloadHandler downloadHandler = activity.getDownloadHandler();
        FileHandler fileHandler = activity.getFileHandler();

        if (alreadyDownloaded == null) {
            alreadyDownloaded = new Stack<>();
        }

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

            writtenFiles.push(fileHandler.generateFile("jars", config.getName() + ".jar"));
            if (writtenFiles.peek().exists()) {
                writtenFiles.peek().delete();
            }
            if (!alreadyDownloaded.contains(config.getJarUrl())) {
                fileHandler.writeFile(downloadHandler.getDownloadStream(config.getJarUrl()),
                        callback,
                        false,
                        writtenFiles.peek());
                alreadyDownloaded.push(config.getJarUrl());
            } else if (callback != null) {
                callback.onStart();
                callback.onProgress(100);
                callback.onFinish();
            }

            for (Config dependency : config.getDependencies()) {
                if (!alreadyDownloaded.contains(dependency.getJarUrl())) {
                    File dependencyFile = fileHandler.generateFile("jars", dependency.getName() + ".jar");
                    if (dependencyFile.exists()) {
                        dependencyFile.delete();
                    }
                    writtenFiles.push(dependencyFile);
                    fileHandler.writeFile(downloadHandler.getDownloadStream(dependency.getJarUrl()),
                            callback,
                            false,
                            writtenFiles.peek());
                    alreadyDownloaded.push(dependency.getJarUrl());
                } else if (callback != null) {
                    callback.onStart();
                    callback.onProgress(100);
                    callback.onFinish();
                }
            }
            refreshConfigs();
            if (refreshDexed)
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
        File configFile = fileHandler.generateFile("config", config.getName() + ".json");
        if (!configFile.exists() || configFile.delete()) {
            refreshAll(callback);
            return true;
        } else {
            return false;
        }
    }

    private void clearUntrackedJars() {
        FileHandler fileHandler = activity.getFileHandler();
        ArrayList<String> jarStrings = new ArrayList<String>();
        for (String file : jarDir.list()) {
            int dir = file.lastIndexOf(File.separatorChar);
            if (dir != -1) {
                jarStrings.add(file.substring(dir));
            } else {
                jarStrings.add(file);
            }
        }
        for (ModuleConfig config : getConfigs()) {
            jarStrings.remove(config.getName() + ".jar");
            for (Config dependency : config.getDependencies()) {
                jarStrings.remove(dependency.getName() + ".jar");
            }
        }
        for (String jar : jarStrings) {
            fileHandler.generateFile("jars", jar).delete();
        }
    }

    public void refreshAll(final StagedCallback callback) throws IOException, JsonParseException {
        refreshConfigs();
        File undexedDir = activity.getDir("undexed", Context.MODE_PRIVATE);
        File backupClassesFolder = activity.getDir("undexed_backup", Context.MODE_PRIVATE);
        if (backupClassesFolder.exists()) {
            activity.getFileHandler().deleteFile(backupClassesFolder);
        }
        undexedDir.renameTo(backupClassesFolder);
        undexedDir.mkdirs();
        try {
            String[] stages = new String[configs.length];
            for (int i = 0; i < stages.length; i++) {
                stages[i] = "Getting module " + configs[i].getName();
            }
            if (callback != null) {
                callback.setStages(stages);
            }
            Stack<String> alreadyDownloaded = new Stack<String>();
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
                }, config, alreadyDownloaded, false);
                if (callback != null) {
                    callback.onFinish();
                }
            }
            refreshDexed();
        } catch (IOException | JsonParseException e) {
            e.printStackTrace();
            activity.getFileHandler().deleteFile(undexedDir);
            backupClassesFolder.renameTo(undexedDir);
            throw e;
        }
    }

    public ModuleConfig fromJson(String json) throws JsonParseException {
        return gson.fromJson(json, ModuleConfig.class);
    }

    public String toJson(ModuleConfig json) {
        return gson.toJson(json);
    }

    public EAPDisplayableModule loadModule(ModuleConfig config)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        return (EAPDisplayableModule) instantiate(config.getTargetModule());
    }

    public Object instantiate(String classname) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        if (classLoader == null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, "Refresh all modules before launching a module.", Toast.LENGTH_LONG).show();
                }
            });
            return null;
        }
        return classLoader.loadClass(classname).newInstance();
    }

    @Override
    public boolean setup() {
        gson = new Gson();

        configDir = activity.getDir("config", Context.MODE_PRIVATE);
        jarDir = activity.getDir("jars", Context.MODE_PRIVATE);
        undexedDir = activity.getDir("undexed", Context.MODE_PRIVATE);
        dexedJar = activity.getFileHandler().generateFile("dexed", "classes.jar");
        optimizedDexDir = activity.getDir("optdex", Context.MODE_PRIVATE);

        configDir.mkdirs();
        dexedJar.getParentFile().mkdirs();
        jarDir.mkdirs();
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
        final File[] configFiles = configDir.listFiles();
        ArrayList<ModuleConfig> configList = new ArrayList<>(configFiles.length);
        ModuleConfig midconfig;
        for (int i = 0; i < configFiles.length; i++) {
            final int intermediary = i;
            midconfig = gson.fromJson(fileHandler.readFile(null, configFiles[i]), ModuleConfig.class);
            if (midconfig == null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(activity, "Failed to load " + configFiles[intermediary].getName(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                configList.add(midconfig);
            }
        }
        configs = configList.toArray(new ModuleConfig[configList.size()]);
    }

    private void refreshDexed() throws IOException {
        if (dexedJar.exists()) {
            dexedJar.delete();
        }
        dexedJar.getParentFile().mkdirs();

        clearUntrackedJars();

        unpackageJars();

        if (undexedDir.list().length == 0) {
            return;
        }

        String[] args = new String[]{
                "--keep-classes",
                "--output=" + dexedJar.getAbsolutePath(),
                undexedDir.getAbsolutePath()
        };

        Main.main(args);

        classLoader = new DexClassLoader(dexedJar.getAbsolutePath(),
                optimizedDexDir.getAbsolutePath(),
                null,
                activity.getClassLoader());
    }

    private void unpackageJars() throws IOException {
        String targetDir = undexedDir.getAbsolutePath();
        JarFile jarFile;
        Enumeration<JarEntry> jarEntryEnumeration;
        JarEntry jarEntry;
        File outputFile;
        InputStream fromJar;
        FileOutputStream toFile;
        for (File jar : jarDir.listFiles()) {
            jarFile = new JarFile(jar);
            jarEntryEnumeration = jarFile.entries();
            while (jarEntryEnumeration.hasMoreElements()) {
                jarEntry = jarEntryEnumeration.nextElement();
                if (jarEntry.isDirectory() || !jarEntry.getName().endsWith(".class")) {
                    continue;
                }
                outputFile = new File(targetDir, jarEntry.getName());
                if (outputFile.exists()) {
                    outputFile.delete();
                } else {
                    outputFile.getParentFile().mkdirs();
                    System.out.println(jarEntry.getName());
                }
                fromJar = jarFile.getInputStream(jarEntry);
                toFile = new FileOutputStream(outputFile);
                while (fromJar.available() > 0) {
                    toFile.write(fromJar.read());
                }
                fromJar.close();
                toFile.close();
            }
        }
    }

    public ModuleConfig[] getConfigs() {
        return configs;
    }
}

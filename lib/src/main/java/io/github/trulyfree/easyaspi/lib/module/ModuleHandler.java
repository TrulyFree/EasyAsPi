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
import android.support.annotation.Nullable;
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
import io.github.trulyfree.easyaspi.lib.callback.EmptyCallback;
import io.github.trulyfree.easyaspi.lib.callback.StagedCallback;
import io.github.trulyfree.easyaspi.lib.disp.EAPDisplayableModule;
import io.github.trulyfree.easyaspi.lib.dl.DownloadHandler;
import io.github.trulyfree.easyaspi.lib.io.FileHandler;
import io.github.trulyfree.easyaspi.lib.module.conf.Config;
import io.github.trulyfree.easyaspi.lib.module.conf.ModuleConfig;

/**
 * Helper class for Module configuration, downloading, and establishment.
 *
 * @author vtcakavsmoace
 * @since v0.0.1-alpha
 */
public class ModuleHandler implements Module {
    /**
     * The activity which owns this ModuleHandler.
     */
    private final EAPActivity activity;

    /**
     * The configs that this handler is aware of.
     */
    private ModuleConfig[] configs;

    /**
     * The Gson instance that this ModuleHandler uses.
     */
    private Gson gson;

    /**
     * The directory containing the configs that this handler is aware of.
     */
    private File configDir;

    /**
     * The directory containing the jars that this handler will manipulate.
     */
    private File jarDir;

    /**
     * The directory containing the undexed classes that this handler will manipulate.
     */
    private File undexedDir;

    /**
     * The directory containing the dexed classes that this handler will manipulate.
     */
    private File dexedJar;

    /**
     * The directory containing the optimized dex file that this handler will load modules from.
     */
    private File optimizedDexDir;

    /**
     * The class loader used by the ModuleHandler to load up modules.
     */
    private DexClassLoader classLoader;

    /**
     * The configs that the ModuleHandler should define by default. This exists for developers to
     * test their modules without having to use an upstream repository on every build.
     */
    private ModuleConfig[] debugConfigs;

    /**
     * Standard constructor for ModuleHandler. All ModuleHandlers MUST be instantiated with a
     * reference to an EAPActivity.
     *
     * You may set the value of debugConfigs by modifying this constructor.
     *
     * @param activity The activity which owns this module handler.
     */
    public ModuleHandler(@NonNull EAPActivity activity) {
        this.activity = activity;
        this.debugConfigs = new ModuleConfig[]{
        };
    }

    /**
     * Returns the module config located at the specified URL.
     *
     * @param configUrl The URL to download from.
     * @return config The ModuleConfig located at the URL.
     * @throws IOException If the download fails.
     * @throws JsonParseException If the instantiation through Gson fails.
     */
    public ModuleConfig getModuleConfig(@NonNull String configUrl) throws IOException, JsonParseException {
        DownloadHandler downloadHandler = activity.getDownloadHandler();
        String stringConfig = downloadHandler.download(null, configUrl);
        System.out.println("Downloaded JSON: " + stringConfig);
        return gson.fromJson(stringConfig, ModuleConfig.class);
    }

    /**
     * Downloads and establishes a module given a specified module config.
     *
     * @param callback The callback to report progress to.
     * @param config The config to base off of.
     * @param alreadyDownloaded URLs which we have already downloaded from.
     * @param refreshDexed Whether or not to refresh the dex on finish.
     * @return success Whether or not the module was downloaded successfully.
     * @throws IOException If the download fails.
     * @throws JsonParseException If the ModuleConfig's config URL contains a malformed config.
     */
    public boolean getNewModule(@Nullable StagedCallback callback, @NonNull ModuleConfig config, @Nullable Stack<String> alreadyDownloaded, boolean refreshDexed) throws IOException, JsonParseException {
        DownloadHandler downloadHandler = activity.getDownloadHandler();
        FileHandler fileHandler = activity.getFileHandler();

        if (alreadyDownloaded == null) {
            alreadyDownloaded = new Stack<String>();
        }

        Stack<File> writtenFiles = new Stack<File>();

        try {
            String stringConfig = gson.toJson(config);
            writtenFiles.push(fileHandler.generateFile("config", config.getName() + ".json"));
            if (writtenFiles.peek().exists()) {
                writtenFiles.peek().delete();
            }
            fileHandler.writeFile(stringConfig, null, writtenFiles.peek());

            if (callback == null) {
                callback = EmptyCallback.EMPTY;
            }

            String[] stages = new String[config.getDependencies().length + 2];

            stages[0] = "Getting main jar (" + config.getName() + ")...";
            StringBuilder stringBuilder;
            for (int i = 1; i < stages.length - 1; i++) {
                stringBuilder = new StringBuilder("Getting dependency ");
                stringBuilder.append(config.getDependencies()[i - 1].getName());
                stringBuilder.append(" (");
                stringBuilder.append(i);
                stringBuilder.append("/");
                stringBuilder.append(stages.length - 1);
                stringBuilder.append(")...");
                stages[i] = stringBuilder.toString();
            }
            stages[stages.length - 1] = "Building modules...";
            callback.setStages(stages);

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
            } else {
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
                } else {
                    callback.onStart();
                    callback.onProgress(100);
                    callback.onFinish();
                }
            }
            refreshConfigs();
            if (refreshDexed) {
                final StagedCallback intermediary = callback;
                callback.onStart();
                refreshDexed(new StagedCallback() {
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
                        intermediary.onProgress(numerator / denominator);
                    }

                    @Override
                    public void onFinish() {
                        current++;
                    }
                });
                callback.onFinish();
            } else {
                callback.onStart();
                callback.onProgress(100);
                callback.onFinish();
            }
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

    /**
     * Deletes a Module from the directories known by this handler.
     *
     * @param callback The callback to report progress to.
     * @param config The config to delete.
     * @return success Whether the config was successfully removed.
     * @throws IOException If the deletion process fails.
     */
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

    /**
     * Clears all jars not referenced by a ModuleConfig.
     */
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

    /**
     * Redownloads and reestablishes all Modules.
     *
     * @param callback The callback to report progress to.
     * @throws IOException If the download or file processes fail.
     * @throws JsonParseException If a config downloaded is not a valid json.
     */
    public void refreshAll(StagedCallback callback) throws IOException, JsonParseException {
        refreshConfigs();
        File undexedDir = activity.getDir("undexed", Context.MODE_PRIVATE);
        File backupClassesFolder = activity.getDir("undexed_backup", Context.MODE_PRIVATE);
        if (backupClassesFolder.exists()) {
            activity.getFileHandler().deleteFile(backupClassesFolder);
        }
        undexedDir.renameTo(backupClassesFolder);
        undexedDir.mkdirs();
        try {
            String[] stages = new String[configs.length + 1];
            for (int i = 0; i < configs.length; i++) {
                stages[i] = "Getting module " + configs[i].getName();
            }
            stages[configs.length] = "Building modules...";
            if (callback == null) {
                callback = EmptyCallback.EMPTY;
            }
            callback.setStages(stages);
            Stack<String> alreadyDownloaded = new Stack<String>();
            final StagedCallback intermediary = callback;
            for (ModuleConfig config : configs) {
                callback.onStart();
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
                        intermediary.onProgress(numerator / denominator);
                    }

                    @Override
                    public void onFinish() {
                        current++;
                    }
                }, config, alreadyDownloaded, false);
                callback.onFinish();
            }
            callback.onStart();
            refreshDexed(new StagedCallback() {
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
                    intermediary.onProgress(numerator / denominator);
                }

                @Override
                public void onFinish() {
                    current++;
                }
            });
            callback.onFinish();
        } catch (IOException e) {
            e.printStackTrace();
            activity.getFileHandler().deleteFile(undexedDir);
            backupClassesFolder.renameTo(undexedDir);
            throw e;
        } catch (JsonParseException e) {
            e.printStackTrace();
            activity.getFileHandler().deleteFile(undexedDir);
            backupClassesFolder.renameTo(undexedDir);
            throw e;
        }
    }

    /**
     * Converts a String json to a ModuleConfig instance.
     *
     * @param json The json to parse.
     * @return config The ModuleConfig instantiated by this json.
     * @throws JsonParseException If the json is malformed or not of type ModuleConfig.
     */
    public ModuleConfig fromJson(String json) throws JsonParseException {
        return gson.fromJson(json, ModuleConfig.class);
    }

    /**
     * Converts a ModuleConfig instance to a String json.
     *
     * @param config The config to jsonify.
     * @return json The json generated from this config.
     */
    public String toJson(ModuleConfig config) {
        return gson.toJson(config);
    }

    /**
     * Instantiates a EAPDisplayableModule from a given config.
     *
     * @param config The ModuleConfig to base off of.
     * @return module The target module.
     * @throws ClassNotFoundException If the targetModule field of config is not a class we know of.
     * @throws IllegalAccessException If the targetModule field of config is not a class we see.
     * @throws InstantiationException If the targetModule field of config is not an instantiable class.
     */
    public EAPDisplayableModule loadModule(ModuleConfig config)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        return (EAPDisplayableModule) instantiate(config.getTargetModule());
    }

    /**
     * Instantiates an Object given a class.
     *
     * @param classname The fully qualified name of the class to instantiate.
     * @return obj The instantiation of the target class.
     * @throws ClassNotFoundException If the classname did not associate with a known class.
     * @throws IllegalAccessException If we cannot access the target class.
     * @throws InstantiationException If we cannot instantiate the target class with an empty constructor.
     */
    public Object instantiate(String classname) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        if (classLoader == null) {
            classLoader = new DexClassLoader(dexedJar.getAbsolutePath(),
                    optimizedDexDir.getAbsolutePath(),
                    null,
                    activity.getClassLoader());
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

    /**
     * Helper method to refresh the configs known by this handler.
     *
     * @throws IOException If the refresh action fails due to IO failure.
     */
    private void refreshConfigs() throws IOException {
        FileHandler fileHandler = activity.getFileHandler();
        final File[] configFiles = configDir.listFiles();
        ArrayList<ModuleConfig> configList = new ArrayList<ModuleConfig>(configFiles.length + debugConfigs.length);
        ModuleConfig midconfig;
        for (int i = 0; i < configFiles.length; i++) {
            final int intermediary = i;
            midconfig = gson.fromJson(fileHandler.readFile(null, configFiles[i]), ModuleConfig.class);
            if (midconfig == null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        activity.displayToUser("Failed to load " + configFiles[intermediary].getName(), Toast.LENGTH_SHORT);
                    }
                });
            } else {
                configList.add(midconfig);
            }
        }
        for (ModuleConfig debugConfig : debugConfigs) {
            configList.add(debugConfig);
        }
        configs = configList.toArray(new ModuleConfig[configList.size()]);
    }

    /**
     * Helper method to refresh the dex the handler loads from.
     *
     * @param callback Callback to report progress to.
     * @throws IOException If the refresh action fails due to IO failure.
     */
    private void refreshDexed(final @NonNull StagedCallback callback) throws IOException {

        callback.onStart();
        if (dexedJar.exists()) {
            dexedJar.delete();
        }
        dexedJar.getParentFile().mkdirs();

        clearUntrackedJars();

        unpackJars(new StagedCallback() {
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
                callback.onProgress(numerator / denominator);
            }

            @Override
            public void onFinish() {
                current++;
            }
        });

        if (undexedDir.list().length == 0) {
            return;
        }

        String[] args = new String[]{
                "--keep-classes",
                "--verbose",
                "--incremental",
                "--output=" + dexedJar.getAbsolutePath(),
                undexedDir.getAbsolutePath()
        };

        Main.main(args);

        classLoader = new DexClassLoader(dexedJar.getAbsolutePath(),
                optimizedDexDir.getAbsolutePath(),
                null,
                activity.getClassLoader());
        callback.onProgress(100);
        callback.onFinish();
    }

    /**
     * Unpacks the jars available to this Handler.
     *
     * @param callback Callback to report progress to.
     * @throws IOException If the upackage action fails due to IO failure.
     */
    private void unpackJars(final @NonNull StagedCallback callback) throws IOException {
        String targetDir = undexedDir.getAbsolutePath();
        JarFile jarFile;
        Enumeration<JarEntry> jarEntryEnumeration;
        JarEntry jarEntry;
        File outputFile;
        InputStream fromJar;
        FileOutputStream toFile;
        File[] jarFiles = jarDir.listFiles();
        callback.setStages(new String[jarFiles.length]);
        for (File jarFile1 : jarFiles) {
            callback.onStart();
            jarFile = new JarFile(jarFile1);
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
            callback.onProgress(100);
            callback.onFinish();
        }
    }

    /**
     * Returns the array of ModuleConfigs known by this handler.
     *
     * @return configs The array of ModuleConfigs known by this handler.
     */
    public ModuleConfig[] getConfigs() {
        return configs;
    }

    /**
     * Returns the Gson instance used by this class.
     *
     * @return gson The Gson instance used by this class.
     */
    public Gson getGson() {
        return gson;
    }

    /**
     * Returns the activity which owns this handler.
     *
     * @return activity The activity which owns this handler.
     */
    public EAPActivity getActivity() {
        return activity;
    }

}

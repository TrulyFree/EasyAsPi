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

package io.github.trulyfree.easyaspi.lib.io;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Arrays;

import io.github.trulyfree.easyaspi.lib.EAPActivity;
import io.github.trulyfree.easyaspi.lib.callback.Callback;
import io.github.trulyfree.easyaspi.lib.callback.EmptyCallback;

/**
 * Helper class which aids in File IO with relation to EasyAsPi. You do not have to use this class.
 *
 * @author vtcakavsmoace
 * @since v0.0.1-alpha
 */
public class FileHandler {

    /**
     * The read/write buffer size.
     */
    private final static int BUFFER_SIZE = 4096;

    /**
     * Characters which may not be found within filenames.
     */
    private static final char[] ILLEGAL_CHARACTERS = {'/', '\n', '\r', '\t', '\0', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"', ':'};

    /**
     * The activity which owns this FileHandler.
     */
    private final EAPActivity activity;

    /**
     * Standard constructor for FileHandler. FileHandlers MUST be instantiated with a reference to
     * an EAPActivity.
     *
     * @param activity The EAPActivity which owns this FileHandler.
     */
    public FileHandler(@NonNull EAPActivity activity) {
        this.activity = activity;
    }

    /**
     * Writes a String to a File at the specified path.
     *
     * @param content String to write to the disk.
     * @param callback Callback instance for progress updates on the write operation.
     * @param appdir The application subdirectory in which to place it.
     * @param path The subdirectories beneath the appdir, ending with the target file.
     * @throws IOException If the writing process fails.
     */
    public void writeFile(@NonNull String content,
                          @Nullable Callback callback,
                          @NonNull String appdir,
                          @NonNull String... path) throws IOException {
        File target = generateFile(appdir, path);
        if (target == null) {
            return;
        }
        writeFile(content, callback, target);
    }

    /**
     * Writes a String to a specified File.
     *
     * @param content String to write to the disk.
     * @param callback Callback instance for progress updates on the write operation.
     * @param target The target file to write to.
     * @throws IOException If the writing process fails.
     */
    public void writeFile(@NonNull String content,
                          @Nullable Callback callback,
                          @NonNull File target) throws IOException {
        target.getParentFile().mkdirs();

        PrintWriter out = new PrintWriter(target);

        if (callback == null) {
            callback = EmptyCallback.EMPTY;
        }

        callback.onStart();

        out.write(content);
        out.close();

        callback.onFinish();
    }

    /**
     * Writes content read from an InputStream to a file at a specified path.
     *
     * @param input The InputStream to read from.
     * @param callback Callback instance for progress updates on the write operation.
     * @param append Whether or not to append to the target file.
     * @param appdir The application subdirectory in which to place it.
     * @param path The subdirectories beneath the appdir, ending with the target file.
     * @throws IOException If the writing process fails.
     */
    public void writeFile(@NonNull InputStream input,
                          @Nullable Callback callback,
                          boolean append,
                          @NonNull String appdir,
                          @NonNull String... path) throws IOException {
        File target = generateFile(appdir, path);
        if (target == null) {
            return;
        }
        writeFile(input, callback, append, target);
    }

    /**
     * Writes content read from an InputStream to a specified file.
     * 
     * @param input The InputStream to read from.
     * @param callback Callback instance for progress updates on the write operation.
     * @param append Whether or not to append to the target file.
     * @param target The target file to write to.
     * @throws IOException If the writing process fails.
     */
    public void writeFile(@NonNull InputStream input,
                          @Nullable Callback callback,
                          boolean append,
                          @NonNull File target) throws IOException {
        target.getParentFile().mkdirs();

        OutputStream output = new FileOutputStream(target, append);

        byte data[] = new byte[BUFFER_SIZE];
        int current = 0, count;

        if (callback == null) {
            callback = EmptyCallback.EMPTY;
        }

        callback.onStart();

        while ((count = input.read(data)) != -1) {
            current += count;
            callback.onProgress(current);
            output.write(data, 0, count);
        }

        output.flush();
        output.close();
        input.close();

        callback.onFinish();
    }

    /**
     * Reads content from a file at the specified path and writes it to an OutputStream.
     *
     * @param output OutputStream to write the content to.
     * @param callback Callback instance for progress updates on the read operation.
     * @param appdir The application subdirectory in which to locate the target file.
     * @param path The subdirectories beneath the appdir, ending with the target file.
     * @throws IOException If the reading process fails.
     */
    public void readFile(@NonNull OutputStream output,
                         @Nullable Callback callback,
                         @NonNull String appdir,
                         @NonNull String... path) throws IOException {
        File target = generateFile(appdir, path);
        if (target == null) {
            return;
        }
        readFile(output, callback, target);
    }

    /**
     * Reads content from a specified file and writes it to an OutputStream.
     *
     * @param output OutputStream to write the content to.
     * @param callback Callback instance for progress updates on the read operation.
     * @param target The target file to read from.
     * @throws IOException If the reading process fails.
     */
    public void readFile(@NonNull OutputStream output,
                         @Nullable Callback callback,
                         @NonNull File target) throws IOException {
        InputStream input = new BufferedInputStream(new FileInputStream(target));

        if (callback == null) {
            callback = EmptyCallback.EMPTY;
        }

        callback.onStart();

        byte data[] = new byte[BUFFER_SIZE];
        long total = target.length();
        int current = 0,
                count;

        while ((count = input.read(data)) != -1) {
            current += count;
            callback.onProgress((int) ((100 * current) / total));
            output.write(data, 0, count);
        }

        output.flush();
        output.close();
        input.close();

        callback.onFinish();
    }

    /**
     * Reads content from a file at a specified path and returns it as a String.
     *
     * @param callback Callback instance for progress updates on the read operation.
     * @param appdir The application subdirectory in which to locate the target file.
     * @param path The subdirectories beneath the appdir, ending with the target file.
     * @return content Content of the file, as a String.
     * @throws IOException If the reading process fails.
     */
    public String readFile(@Nullable Callback callback,
                           @NonNull String appdir,
                           @NonNull String... path) throws IOException {
        File target = generateFile(appdir, path);
        if (target == null) {
            return null;
        }
        return readFile(callback, target);
    }

    /**
     * Reads content from a specified file and returns it as a String.
     *
     * @param callback Callback instance for progress updates on the read operation.
     * @param target The target file to read from.
     * @return content Content of the file, as a String.
     * @throws IOException If the reading process fails.
     */
    public String readFile(@Nullable Callback callback,
                           @NonNull File target) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        readFile(output, callback, target);
        return new String(output.toByteArray(), "UTF-8");
    }

    /**
     * Deletes a File instance. If that File is a folder, it will delete its submembers first.
     *
     * @param file File to delete.
     * @return success Success or failure of the deletion process.
     * @throws IOException If the reading process fails.
     */
    public boolean deleteFile(@NonNull File file) throws IOException {
        if (file.isDirectory()) {
            for (File subfile : file.listFiles()) {
                deleteFile(subfile);
            }
        }
        return file.delete();
    }

    /**
     * Generates a File instance given a specified path (with path checking).
     *
     * @param appdir The application subdirectory in which to locate the target file.
     * @param path The subdirectories beneath the appdir, ending with the target file.
     * @return target The File at the specified location.
     */
    public File generateFile(@NonNull String appdir,
                             @NonNull String... path) {
        if (!isValidName(appdir) || path.length == 0) {
            return null;
        }
        for (String pathitem : path) {
            if (!isValidName(pathitem)) {
                return null;
            }
        }
        File appdirfile = activity.getDir(appdir, Context.MODE_PRIVATE);
        StringBuilder concatpath = new StringBuilder(appdirfile.getAbsolutePath());
        for (String pathmember : path) {
            concatpath.append("/");
            concatpath.append(pathmember);
        }
        return new File(concatpath.toString());
    }

    /**
     * Validates a filename for this system.
     *
     * @param filename A String name to check.
     * @return valid The validity of this filename.
     */
    private boolean isValidName(@NonNull String filename) {
        if (filename == null || filename.length() == 0) {
            return false;
        }
        for (char character : filename.toCharArray()) {
            for (char check : ILLEGAL_CHARACTERS) {
                if (character == check) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns the buffer size defined by this class.
     *
     * @return BUFFER_SIZE Buffer size defined by this class.
     */
    public static int getBufferSize() {
        return BUFFER_SIZE;
    }

    /**
     * Returns a copy of the array that defines the illegal characters in filenames.
     *
     * @return ILLEGAL_CHARACTERS Illegal characters to find in filenames.
     */
    public static char[] getIllegalCharacters() {
        return Arrays.copyOf(ILLEGAL_CHARACTERS, ILLEGAL_CHARACTERS.length);
    }

    /**
     * Returns the activity which owns this FileHandler instance.
     *
     * @return activity The activity which owns this FileHandler.
     */
    public EAPActivity getActivity() {
        return activity;
    }

}

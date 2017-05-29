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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import io.github.trulyfree.easyaspi.lib.EAPActivity;
import io.github.trulyfree.easyaspi.lib.callback.Callback;
import io.github.trulyfree.easyaspi.lib.callback.EmptyCallback;

public class FileHandler {

    private final static int BUFFER_SIZE = 4096;
    private static final char[] ILLEGAL_CHARACTERS = {'/', '\n', '\r', '\t', '\0', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"', ':'};

    private final EAPActivity activity;

    public FileHandler(@NonNull EAPActivity activity) {
        this.activity = activity;
    }

    public void writeFile(@NonNull String content,
                          Callback callback,
                          @NonNull String appdir,
                          @NonNull String... path) throws IOException {
        File target = generateFile(appdir, path);
        if (target == null) {
            return;
        }
        writeFile(content, callback, target);
    }

    public void writeFile(@NonNull String content,
                          Callback callback,
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

    public void writeFile(@NonNull InputStream input,
                          Callback callback,
                          boolean append,
                          @NonNull String appdir,
                          @NonNull String... path) throws IOException {
        File target = generateFile(appdir, path);
        if (target == null) {
            return;
        }
        writeFile(input, callback, append, target);
    }

    public void writeFile(@NonNull InputStream input,
                          Callback callback,
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

    public void readFile(@NonNull OutputStream output,
                         Callback callback,
                         @NonNull String appdir,
                         @NonNull String... path) throws IOException {
        File target = generateFile(appdir, path);
        if (target == null) {
            return;
        }
        readFile(output, callback, target);
    }

    public void readFile(@NonNull OutputStream output,
                         Callback callback,
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

    public String readFile(Callback callback,
                           @NonNull String appdir,
                           @NonNull String... path) throws IOException {
        File target = generateFile(appdir, path);
        if (target == null) {
            return null;
        }
        return readFile(callback, target);
    }

    public String readFile(Callback callback,
                           @NonNull File target) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        readFile(output, callback, target);
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }

    public boolean deleteFile(File file) throws IOException {
        if (file.isDirectory()) {
            for (File subfile : file.listFiles()) {
                deleteFile(subfile);
            }
        }
        return file.delete();
    }

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

    private boolean isValidName(String filename) {
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

    public static int getBufferSize() {
        return BUFFER_SIZE;
    }

    public static char[] getIllegalCharacters() {
        return Arrays.copyOf(ILLEGAL_CHARACTERS, ILLEGAL_CHARACTERS.length);
    }

    public EAPActivity getActivity() {
        return activity;
    }

}

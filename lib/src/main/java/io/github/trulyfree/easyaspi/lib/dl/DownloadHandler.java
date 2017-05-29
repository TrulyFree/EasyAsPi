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

package io.github.trulyfree.easyaspi.lib.dl;

import android.support.annotation.NonNull;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import io.github.trulyfree.easyaspi.lib.EAPActivity;
import io.github.trulyfree.easyaspi.lib.callback.Callback;
import io.github.trulyfree.easyaspi.lib.callback.EmptyCallback;

public class DownloadHandler {

    private final static int BUFFER_SIZE = 4096;

    private final static int TIMEOUT = 3000;

    private final EAPActivity activity;

    public DownloadHandler(@NonNull EAPActivity activity) {
        this.activity = activity;
    }

    public String download(Callback callback,
                           @NonNull String urlString) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        download(output, callback, urlString);
        return new String(output.toByteArray());
    }

    public void download(@NonNull OutputStream output,
                         Callback callback,
                         @NonNull String urlString) throws IOException {
        broadcastDownload(urlString);
        URL url = new URL(urlString);
        URLConnection urlConnection = url.openConnection();
        urlConnection.setConnectTimeout(TIMEOUT);
        urlConnection.setReadTimeout(TIMEOUT);

        InputStream input = new BufferedInputStream(url.openStream());

        if (callback == null) {
            callback = EmptyCallback.EMPTY;
        }

        callback.onStart();

        byte data[] = new byte[BUFFER_SIZE];
        int total = urlConnection.getContentLength(),
                current = 0,
                count;

        while ((count = input.read(data)) != -1) {
            current += count;
            callback.onProgress((100 * current) / total);
            output.write(data, 0, count);
        }

        output.flush();
        output.close();
        input.close();

        callback.onFinish();
    }

    public InputStream getDownloadStream(@NonNull String urlString) throws IOException {
        broadcastDownload(urlString);
        URL url = new URL(urlString);
        URLConnection urlConnection = url.openConnection();
        urlConnection.setConnectTimeout(TIMEOUT);
        urlConnection.setReadTimeout(TIMEOUT);

        return new BufferedInputStream(url.openStream());
    }

    private void broadcastDownload(@NonNull String urlString) {
        System.out.println("Downloaded from " + urlString);
    }

    public EAPActivity getActivity() {
        return activity;
    }
}

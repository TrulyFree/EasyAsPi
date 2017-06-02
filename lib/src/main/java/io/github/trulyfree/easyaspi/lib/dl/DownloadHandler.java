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
import android.support.annotation.Nullable;

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

/**
 * Helper class for all downloading actions taken by EasyAsPi. You do not have to use this if you
 * don't want to. :P
 *
 * @author vtcakavsmoace
 * @since v0.0.1-alpha
 */
public class DownloadHandler {

    /**
     * Buffer size for all downloads.
     */
    private final static int BUFFER_SIZE = 4096;

    /**
     * Timeout for all connections.
     */
    private final static int TIMEOUT = 3000;

    /**
     * The activity which owns this download handler.
     */
    private final EAPActivity activity;

    /**
     * Standard constructor for DownloadHandler. All DownloadHandlers MUST be instantiated with a
     * reference to an EAPActivity.
     *
     * @param activity The activity which owns this download handler.
     */
    public DownloadHandler(@NonNull EAPActivity activity) {
        this.activity = activity;
    }

    /**
     * Downloads an item and returns it as a String.
     *
     * @param callback Callback for progress updates on the download.
     * @param urlString The URL of this download.
     * @return content The content of the target URL as a String.
     * @throws IOException If the download fails.
     */
    public String download(@Nullable Callback callback,
                           @NonNull String urlString) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        download(output, callback, urlString);
        return new String(output.toByteArray());
    }

    /**
     * Downloads an item and writes it to an output stream during the download.
     *
     * @param output The output stream to write to.
     * @param callback Callback for progress updates on the download.
     * @param urlString The URL of this download.
     * @throws IOException If the download fails.
     */
    public void download(@NonNull OutputStream output,
                         @Nullable Callback callback,
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

    /**
     * Returns a readable (buffered) input stream that downloads may be read from.
     *
     * @param urlString The URL of this download.
     * @return downloadStream A readable (buffered) input stream from which to read the download.
     * @throws IOException If the connection fails.
     */
    public InputStream getDownloadStream(@NonNull String urlString) throws IOException {
        broadcastDownload(urlString);
        URL url = new URL(urlString);
        URLConnection urlConnection = url.openConnection();
        urlConnection.setConnectTimeout(TIMEOUT);
        urlConnection.setReadTimeout(TIMEOUT);

        return new BufferedInputStream(url.openStream());
    }

    /**
     * Debug method for checking when downloads occur.
     *
     * @param urlString The URL of this download.
     */
    private void broadcastDownload(@NonNull String urlString) {
        System.out.println("Downloaded from " + urlString);
    }

    /**
     * Returns the activity which owns this DownloadHandler.
     *
     * @return activity The activity which owns this DownloadHandler.
     */
    public EAPActivity getActivity() {
        return activity;
    }
}

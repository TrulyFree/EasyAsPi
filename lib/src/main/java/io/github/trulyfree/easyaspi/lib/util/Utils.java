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

package io.github.trulyfree.easyaspi.lib.util;

import android.view.View;
import android.widget.Button;

import com.google.common.util.concurrent.FutureCallback;

/**
 * Helper class for a variety of miscellaneous actions.
 *
 * @author vtcakavsmoace
 * @since v0.0.2-alpha
 */
public class Utils {

    /**
     * Not to be instantiated.
     */
    private Utils() {
        throw new UnsupportedOperationException("Instantiation not permitted.");
    }

    /**
     * Generates an OnClickListener from a given FutureCallback.
     *
     * @param callback The callback to execute when the click event occurs.
     * @return listener The OnClickListener.
     */
    public static View.OnClickListener generateOnClickListener(final FutureCallback<View> callback) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (view instanceof Button) {
                    System.out.println(((Button) view).getText() + " pressed");
                }
                try {
                    callback.onSuccess(view);
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                    callback.onFailure(throwable);
                }
            }
        };
    }

}

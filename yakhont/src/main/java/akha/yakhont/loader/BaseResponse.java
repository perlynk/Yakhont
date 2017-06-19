/*
 * Copyright (C) 2015-2017 akha, a.k.a. Alexander Kharitonov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package akha.yakhont.loader;

import akha.yakhont.CoreLogger;
import akha.yakhont.CoreLogger.Level;
import akha.yakhont.Core.Utils;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.ContextWrapper;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Build;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.reflect.Type;
import java.util.Locale;

/**
 * The <code>BaseResponse</code> class represents the results of loading data.
 *
 * @param <R>
 *        The type of network response
 *
 * @param <E>
 *        The type of error (if any)
 *
 * @param <D>
 *        The type of data
 *
 * @yakhont.see BaseLoader
 
 * @author akha
 */
public class BaseResponse<R, E, D> {

    /**
     * The source of data.
     */
    public enum Source {
        /** The cache. */
        CACHE,
        /** The network. */
        NETWORK,
        /** The loading process was cancelled because of timeout. */
        TIMEOUT,
        /** The loading process was cancelled because of some unknown reason. */
        UNKNOWN
    }

    /** @exclude */
    @SuppressWarnings({"JavaDoc", "WeakerAccess"})
    public  static final    String[]              MIN_COLUMNS             = new String[] {BaseColumns._ID};
    
    /** @exclude */ @SuppressWarnings("JavaDoc")
    public  static final    Cursor                EMPTY_CURSOR            = new MatrixCursor(MIN_COLUMNS, 0);

    private final           R                     mResponse;
    private final           E                     mError;
    private final           D                     mData;
    private final           Cursor                mCursor;
    private                 ContentValues[]       mContentValues;
    private final           Source                mSource;
    private final           Throwable             mThrowable;

    /**
     * The API to convert data.
     *
     * @param <D>
     *        The type of data
     *
     * @see akha.yakhont.loader.BaseConverter
     */
    public interface Converter<D> {

        /**
         * Converts cursor to data.
         *
         * @param cursor
         *        The cursor
         *
         * @return  The data
         */
        D get(Cursor cursor);

        /**
         * Converts data to content values.
         *
         * @param src
         *        The data to convert
         *
         * @return  The content values
         */
        ContentValues[] get(D src);

        /**
         * Returns the type of data.
         *
         * @return  The type of data (or null, if type detection failed)
         */
        Type getType();

        /**
         * Sets the type of data.
         *
         * @param type
         *        The type of data
         *
         * @return  This {@code Converter} object
         */
        Converter<D> setType(Type type);
    }

    /**
     * Callback interface for a client to interact with loader manager. Simplified version of the
     * {@link android.app.LoaderManager.LoaderCallbacks}.
     */
    @SuppressWarnings("unused")
    public interface LoaderCallback<D> {

        /**
         * Called when a loader has finished its load. Please refer to
         * {@link android.app.LoaderManager.LoaderCallbacks#onLoadFinished LoaderCallbacks.onLoadFinished()} for more details.
         *
         * @param data
         *        The data generated by the Loader
         *
         * @param source
         *        The data source
         */
        @SuppressWarnings("UnusedParameters")
        void onLoadFinished(D data, Source source);
    }

    /**
     * Initialises a newly created {@code BaseResponse} object.
     *
     * @param source
     *        The source of data
     */
    public BaseResponse(@NonNull final Source source) {
        this(null, null, null, null, source, null);
    }

    /**
     * Initialises a newly created {@code BaseResponse} object.
     *
     * @param data
     *        The data
     *
     * @param response
     *        The network response
     *
     * @param cursor
     *        The cursor
     *
     * @param error
     *        The error
     *
     * @param source
     *        The source of data
     *
     * @param throwable
     *        The additional error info (normally if error is not an instance of Throwable)
     */
    public BaseResponse(final D data, final R response, final Cursor cursor, final E error,
                        @NonNull final Source source, final Throwable throwable) {
        mData               = data;
        mResponse           = response;
        mCursor             = cursor;
        mError              = error;
        mSource             = source;
        mThrowable          = throwable;
    }

    /** @exclude */ @SuppressWarnings("JavaDoc")
    public void setContentValues(final ContentValues[] values) {
        mContentValues = values;
    }

    /**
     * Returns the loaded data.
     *
     * @return  The loaded data
     */
    public D getResult() {
        return mData;
    }

    /**
     * Returns the network response.
     *
     * @return  The network response
     */
    @SuppressWarnings("unused")
    public R getResponse() {
        return mResponse;
    }

    /**
     * Returns the cursor.
     *
     * @return  The cursor
     */
    public Cursor getCursor() {
        return mCursor;
    }

    /** @exclude */ @SuppressWarnings("JavaDoc")
    public ContentValues[] getValues() {
        return mContentValues;
    }

    /**
     * Returns the error.
     *
     * @return  The error
     */
    @SuppressWarnings("unused")
    public E getError() {
        return mError;
    }

    /**
     * Returns the he additional error info (if any).
     *
     * @return  The additional error info
     */
    @SuppressWarnings("unused")
    public Throwable getThrowable() {
        return mThrowable;
    }

    /**
     * Returns the source of data.
     *
     * @return  The source of data
     */
    @NonNull
    public Source getSource() {
        return mSource;
    }

    /**
     * Please refer to the base method description.
     */
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        final Locale locale = CoreLogger.getLocale();
        final String newLine = System.getProperty("line.separator");

        builder.append(String.format(locale, "%s, class: %s, error: %s%s",
                mSource.name(), mData == null ? null: mData.getClass().getSimpleName(),
                mError, newLine));
        builder.append(String.format(locale, "more error info: %s%s", mThrowable, newLine));

        if (mData == null)
            builder.append("no data").append(newLine);
        else {
            if (mData.getClass().isArray()) {
                final Object[] array = (Object[]) mData;
                builder.append(String.format(locale, "data: length %d%s", array.length, newLine));

                for (int i = 0; i < array.length; i++)
                    builder.append(String.format(locale, "[%d] %s%s", i, array[i], newLine));
            }
            else
                builder.append("data ").append(mData).append(newLine);
        }

        final int nPos = (mCursor == null) ? -1: mCursor.getPosition();

        if (mCursor == null)
            builder.append("no cursor");
        else if (!mCursor.moveToFirst())
            builder.append("empty cursor");
        else {
            builder.append("cursor:").append(newLine);

            for (;;) {
                for (int i = 0; i < mCursor.getColumnCount(); i++)
                    builder.append(String.format(locale, "%s%s == %s", i == 0 ? "": ", ", mCursor.getColumnName(i), getString(mCursor, i)));
                builder.append(newLine);

                if (!mCursor.moveToNext()) break;
            }
            builder.delete(builder.length() - 1, builder.length());
        }

        if (mCursor != null) mCursor.moveToPosition(nPos);

        return builder.toString();
    }

    private static String getString(@NonNull final Cursor cursor, final int columnIndex) {
        final Object data = getData(cursor, columnIndex);
        return data == null ? null: data instanceof byte[] ? "BLOB": data instanceof Exception ? "error": String.valueOf(data);
    }

    /**
     * Returns the value of the requested column (from the result set returned by a database query) as an Object.
     *
     * @param cursor
     *        The cursor
     *
     * @param columnIndex
     *        The zero-based index of the target column
     *
     * @return  The value of the requested column (or Exception)
     */
    @Nullable
    @SuppressLint("ObsoleteSdkInt")
    public static Object getData(@NonNull final Cursor cursor, final int columnIndex) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                final int type = cursor.getType(columnIndex);
                switch (type) {
                    case Cursor.FIELD_TYPE_NULL:
                        return null;
                    case Cursor.FIELD_TYPE_BLOB:
                        return cursor.getBlob(columnIndex);
                    case Cursor.FIELD_TYPE_INTEGER:
                        return cursor.getLong(columnIndex);
                    case Cursor.FIELD_TYPE_FLOAT:
                        return cursor.getDouble(columnIndex);
                    default:
                        break;
                }
            }
            return cursor.getString(columnIndex);
        }
        catch (Exception e) {
            CoreLogger.log(Level.ERROR, "failed", e);
            return e;
        }
    }

    /**
     * Clears cache table.
     *
     * @param contextWrapper
     *        The {@code ContextWrapper}
     *
     * @param tableName
     *        The table name
     */
    @SuppressWarnings("unused")
    public static void clearTable(@NonNull final ContextWrapper contextWrapper, @NonNull final String tableName) {
        contextWrapper.getContentResolver().delete(Utils.getUri(tableName), null, null);
    }
}

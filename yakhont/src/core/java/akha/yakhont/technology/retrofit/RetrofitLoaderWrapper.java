/*
 * Copyright (C) 2016 akha, a.k.a. Alexander Kharitonov
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

package akha.yakhont.technology.retrofit;

import akha.yakhont.Core;
import akha.yakhont.Core.Requester;
import akha.yakhont.Core.UriResolver;
import akha.yakhont.Core.Utils;
import akha.yakhont.CoreLogger;
import akha.yakhont.CoreLogger.Level;
import akha.yakhont.loader.BaseLoader;
import akha.yakhont.loader.BaseResponse;
import akha.yakhont.loader.BaseResponse.Converter;
import akha.yakhont.loader.BaseResponse.Source;
import akha.yakhont.loader.BaseConverter;
import akha.yakhont.loader.wrapper.BaseResponseLoaderWrapper;
import akha.yakhont.technology.retrofit.Retrofit;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.Context;
import android.content.Loader;
import android.os.Build;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.util.AndroidException;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.YakhontRestAdapter.YakhontCallback;
import retrofit.client.Response;

/**
 * Extends the {@link BaseResponseLoaderWrapper} class to provide Retrofit support.
 *
 * @param <D>
 *        The type of data
 *
 * @author akha
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)                       //YakhontPreprocessor:removeInFlavor
public class RetrofitLoaderWrapper<D> extends BaseResponseLoaderWrapper<Callback<D>, Response, Exception, D> {

    private final LoaderFactory<BaseResponse<Response, Exception, D>>               mPreviousLoaderFactory;
    private final int                                                               mTimeout;

    private       Type                                                              mType;
    private final Object                                                            mTypeLock   = new Object();

    /**
     * Initialises a newly created {@code RetrofitLoaderWrapper} object.
     *
     * @param context
     *        The context
     *
     * @param fragment
     *        The fragment
     *
     * @param requester
     *        The requester
     *
     * @param tableName
     *        The name of the table in the database (to cache the loaded data)
     *
     * @param description
     *        The data description
     */
    @SuppressWarnings("unused")
    public RetrofitLoaderWrapper(@NonNull final Context context,
                                 @NonNull final Fragment fragment, @NonNull final Requester<Callback<D>> requester,
                                 @NonNull final String tableName, final String description) {
        this(context, fragment, null, requester, Core.TIMEOUT_CONNECTION, tableName, description,
                new BaseConverter<D>(), Utils.getUriResolver());
    }

    /**
     * Initialises a newly created {@code RetrofitLoaderWrapper} object.
     *
     * @param context
     *        The context
     *
     * @param fragment
     *        The fragment
     *
     * @param loaderId
     *        The loader ID
     *
     * @param requester
     *        The requester
     *
     * @param timeout
     *        The timeout (in seconds)
     *
     * @param tableName
     *        The name of the table in the database (to cache the loaded data)
     *
     * @param description
     *        The data description
     *
     * @param converter
     *        The converter
     *
     * @param uriResolver
     *        The URI resolver
     */
    @SuppressWarnings("WeakerAccess")
    public RetrofitLoaderWrapper(@NonNull final Context context,
                                 @NonNull final Fragment fragment, final Integer loaderId,
                                 @NonNull final Requester<Callback<D>> requester, @IntRange(from = 1) final int timeout,
                                 @NonNull final String tableName, final String description, @NonNull final Converter<D> converter,
                                 @NonNull final UriResolver uriResolver) {
        super(context, fragment, loaderId, requester, tableName, description, converter, uriResolver);

        mPreviousLoaderFactory      = geLoaderFactory();
        mTimeout                    = timeout;

        setLoaderFactory(new LoaderFactory<BaseResponse<Response, Exception, D>>() {
            @NonNull
            @Override
            public Loader<BaseResponse<Response, Exception, D>> getLoader(final boolean merge) {

                final Loader<BaseResponse<Response, Exception, D>> loader = mPreviousLoaderFactory.getLoader(merge);

                if (loader instanceof BaseLoader) {
                    
                    @SuppressWarnings("unchecked")
                    final BaseLoader<Callback<D>, Response, Exception, D> baseLoader =
                            (BaseLoader<Callback<D>, Response, Exception, D>) loader;

                    baseLoader.setTimeout(mTimeout).setCallback(mType != null ? new Callback<D>() {
                        @Override
                        public void success(final D result, final Response response) {
                            onSuccess(result, response, baseLoader);
                        }
                        @Override
                        public void failure(final RetrofitError error) {
                            onFailure(error, baseLoader);
                        }
                    }: new YakhontCallback<D>() {
                        @Override
                        public boolean setType(final Type type) {
                            synchronized (mTypeLock) {
                                onSetType(type);
                            }
                            return false;
                        }
                        @Override
                        public void success(final D result, final Response response) {
                            onSuccess(result, response, baseLoader);
                        }
                        @Override
                        public void failure(final RetrofitError error) {
                            onFailure(error, baseLoader);
                        }
                    });
                } else
                    CoreLogger.logWarning("callback not set");

                return loader;
            }
        });
    }

    private void onSuccess(final D result, final Response response, final BaseLoader<Callback<D>, Response, Exception, D> loader) {
        loader.callbackHelper(true, new BaseResponse<Response, Exception, D>(
                result, response, null, null, Source.NETWORK));
    }

    private void onFailure(final RetrofitError error, final BaseLoader<Callback<D>, Response, Exception, D> loader) {
        loader.callbackHelper(false, new BaseResponse<Response, Exception, D>(
                null, null, null, new RetrofitException(error), Source.NETWORK));
    }

    private void onSetType(final Type type) {
        CoreLogger.log("set type to " + type);
        mType = type;
        if (mConverter.getType() == null) mConverter.setType(mType);
    }

    /**
     * Please refer to the base method description.
     */
    @Override
    public Type getType() {
        synchronized (mTypeLock) {
            if (mType == null) onSetType(super.getType());
            if (mType == null) onSetType(getType(mRequester));
            return mType;
        }
    }

    private static <D> Type getType(@NonNull final Requester<Callback<D>> requester) {
        final Type[] typeHelper = new Type[1];
        requester.makeRequest(new YakhontCallback<D>() {
            @Override
            public boolean setType(final Type type) {
                typeHelper[0] = type;
                return true;
            }
            @Override public void success(final D result, final Response response) {}
            @Override public void failure(final RetrofitError error)               {}
        });
        return typeHelper[0];
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Builder class for {@link RetrofitLoaderWrapper} objects.
     *
     * @param <D>
     *        The type of data
     */
    public static class RetrofitLoaderBuilder<D> extends BaseResponseLoaderBuilder<Callback<D>, Response, Exception, D> {

        private Integer                                                                 mTimeout;

        /**
         * Initialises a newly created {@code RetrofitLoaderBuilder} object.
         *
         * @param fragment
         *        The fragment
         *
         * @param requester
         *        The requester
         */
        @SuppressWarnings("unused")
        public RetrofitLoaderBuilder(@NonNull final Fragment fragment, @NonNull final Requester<Callback<D>> requester) {
            super(fragment, requester);
        }

        /**
         * Initialises a newly created {@code RetrofitLoaderBuilder} object.
         *
         * @param fragment
         *        The fragment
         *
         * @param type
         *        The data type; will be used to build {@link akha.yakhont.Core.Requester} which calls
         *        the first method (from the API defined by the service interface, see {@link Retrofit#getRetrofitApi}) which handles that type
         */
        public RetrofitLoaderBuilder(@NonNull final Fragment fragment, @NonNull final Class<D> type) {
            super(fragment, getRequester(type));
            setType(type);
        }

        private static <D> Requester<Callback<D>> getRequester(@NonNull final Class<D> type) {
            final Method method = Retrofit.getYakhontRestAdapter().findMethod(type);
            CoreLogger.log(method == null ? Level.ERROR: Level.DEBUG, "for type " + type.getName() + " method == " + method);

            return new Requester<Callback<D>>() {
                @Override
                public void makeRequest(@NonNull final Callback<D> callback) {
                    try {
                        if (method != null) method.invoke(Retrofit.getYakhontRestAdapter().getHandler(), callback);
                    }
                    catch (Throwable throwable) {
                        CoreLogger.log("failed", throwable);
                        throw throwable instanceof RuntimeException ? (RuntimeException) throwable: new RuntimeException(throwable);
                    }
                }
            };
        }

        /**
         * Sets timeout (in seconds).
         *
         * @param timeout
         *        The timeout
         *
         * @return  This {@code RetrofitLoaderBuilder} object to allow for chaining of calls to set methods
         */
        @NonNull
        @SuppressWarnings("unused")
        public RetrofitLoaderBuilder<D> setTimeout(@IntRange(from = 1) final int timeout) {
            mTimeout = timeout;
            return this;
        }

        /**
         * Please refer to the base method description.
         */
        @NonNull
        @Override
        protected RetrofitLoaderWrapper<D> createLoaderWrapper() {
            return new RetrofitLoaderWrapper<>(getContext(), getFragment(), mLoaderId, mRequester,
                    mTimeout == null ? Core.TIMEOUT_CONNECTION: mTimeout,
                    getTableName(), mDescription, getConverter(), getUriResolver());
        }

        /**
         * Please refer to the base method description.
         */
        @Override
        public Type getType() {
            if (mType == null) {
                final Type type = super.getType();
                if (type == null)
                    setType(RetrofitLoaderWrapper.getType(mRequester));
                else
                    mType = type;
            }
            return mType;
        }
    }
}

class RetrofitException extends AndroidException {

    @SuppressWarnings("unused")
    RetrofitException(final RetrofitError error) {
        super(error);
    }

    @Override
    public String toString() {
        final RetrofitError error = (RetrofitError) getCause();
        if (error == null) return "null";

        try {
            return String.format("RetrofitError (%s, %s, %s)", error.getKind().name(), error.getUrl(), error.toString());
        }
        catch (Exception e) {
            return "can not handle RetrofitError";
        }
    }
}
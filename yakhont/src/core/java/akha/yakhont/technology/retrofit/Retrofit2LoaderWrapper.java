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

package akha.yakhont.technology.retrofit;

import akha.yakhont.Core;
import akha.yakhont.Core.Requester;
import akha.yakhont.Core.UriResolver;
import akha.yakhont.Core.Utils.TypeHelper;
import akha.yakhont.CoreLogger;
import akha.yakhont.CoreReflection;
import akha.yakhont.loader.BaseLoader;
import akha.yakhont.loader.BaseLoader.CoreLoadExtendedBuilder;
import akha.yakhont.loader.BaseResponse;
import akha.yakhont.loader.BaseResponse.Converter;
import akha.yakhont.loader.BaseResponse.Source;
import akha.yakhont.loader.wrapper.BaseResponseLoaderWrapper;
import akha.yakhont.loader.wrapper.BaseResponseLoaderWrapper.BaseResponseLoaderExtendedWrapper;
import akha.yakhont.technology.retrofit.Retrofit2;
import akha.yakhont.technology.retrofit.Retrofit2.Retrofit2AdapterWrapper;
import akha.yakhont.technology.rx.BaseRx.CallbackRx;
import akha.yakhont.technology.rx.BaseRx.LoaderRx;
import akha.yakhont.technology.rx.Rx;
import akha.yakhont.technology.rx.Rx.RxSubscription;
import akha.yakhont.technology.rx.Rx2;
import akha.yakhont.technology.rx.Rx2.Rx2Disposable;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.Context;
import android.os.Build;
import android.support.annotation.IntRange;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.view.View;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import okhttp3.ResponseBody;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Extends the {@link BaseResponseLoaderExtendedWrapper} class to provide Retrofit 2 support.
 *
 * @param <D>
 *        The type of data
 *
 * @author akha
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)                       //YakhontPreprocessor:removeInFlavor
public class Retrofit2LoaderWrapper<D> extends BaseResponseLoaderExtendedWrapper<Callback<D>, Response<D>, Throwable, D> {

    private           Method                                                            mMethod;

    /**
     * Initialises a newly created {@code Retrofit2LoaderWrapper} object.
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
    public Retrofit2LoaderWrapper(@NonNull final Context context,
                                  @NonNull final Fragment fragment,
                                  @NonNull final Requester<Callback<D>> requester,
                                  @NonNull final String tableName, final String description) {
        this(context, fragment, null, requester, Core.TIMEOUT_CONNECTION, tableName, description,
                BaseResponseLoaderWrapper.<D>getDefaultConverter(), getDefaultUriResolver());
    }

    /**
     * Initialises a newly created {@code Retrofit2LoaderWrapper} object.
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
    public Retrofit2LoaderWrapper(@NonNull final Context context,
                                  @NonNull final Fragment fragment, final Integer loaderId,
                                  @NonNull final Requester<Callback<D>> requester,
                                  @IntRange(from = 1) final int timeout,
                                  @NonNull final String tableName, final String description,
                                  @NonNull final Converter<D> converter,
                                  @NonNull final UriResolver uriResolver) {
        super(context, fragment, loaderId, requester, tableName, description, converter, uriResolver);

        @SuppressWarnings("unchecked")
        final BaseLoader<Callback<D>, Response<D>, Throwable, D>[] baseLoader =
                (BaseLoader<Callback<D>, Response<D>, Throwable, D>[]) new BaseLoader[1];

        setLoaderParameters(baseLoader, timeout, new Callback<D>() {
                @Override
                public void onResponse(Call<D> call, Response<D> response) {
                    onSuccess(call, response, baseLoader[0]);
                }

                @Override
                public void onFailure(Call<D> call, Throwable throwable) {
                    onError(call, null, throwable, baseLoader[0]);
                }
            });
    }

    private void onSuccess(final Call<D> call, final Response<D> response,
                           final BaseLoader<Callback<D>, Response<D>, Throwable, D> loader) {
        if (response.isSuccessful()) {
            loader.callbackHelper(true, new BaseResponse<Response<D>, Throwable, D>(
                    response.body(), response, null, null, Source.NETWORK, null));
            return;
        }

        final ResponseBody errorBody = response.errorBody();
        CoreLogger.logError("error " + errorBody);
        
        final int code = response.code();
        onError(call, response, new Exception("error code " + code), loader);
    }

    private void onError(@SuppressWarnings("UnusedParameters") final Call<D> call,
                         final Response<D> response, final Throwable error,
                         final BaseLoader<Callback<D>, Response<D>, Throwable, D> loader) {
        loader.callbackHelper(false, new BaseResponse<Response<D>, Throwable, D>(
                null, response, null, error, Source.NETWORK, null));
    }

    /** @exclude */ @SuppressWarnings("JavaDoc")
    @Override
    protected Type getTypeHelper() {
        return Retrofit2LoaderBuilder.getType(mMethod);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Builder class for {@link BaseResponseLoaderWrapper} objects. Creates the Retrofit2-based ones.
     *
     * @param <D>
     *        The type of data
     *
     * @param <T>
     *        The type of Retrofit 2 API
     */
    public static class Retrofit2LoaderBuilder<D, T> extends BaseResponseLoaderExtendedBuilder<Callback<D>, Response<D>, Throwable, D, T> {

        private final Retrofit2<T>                                                      mRetrofit;
        private final LoaderRx<Response<D>, Throwable, D>                               mRx;

        /**
         * Initialises a newly created {@code Retrofit2LoaderBuilder} object.
         *
         * @param fragment
         *        The fragment
         *
         * @param type
         *        The type of data
         *
         * @param retrofit
         *        The Retrofit2 component
         *
         * @param rx
         *        The Rx component
         */
        @SuppressWarnings("unused")
        public Retrofit2LoaderBuilder(@NonNull final Fragment fragment, @NonNull final Type type,
                                      @NonNull final Retrofit2<T>                           retrofit,
                                      final LoaderRx<Response<D>, Throwable, D>             rx) {
            super(fragment, type);

            mRetrofit = retrofit;
            mRx       = rx;
        }

        /** @exclude */ @SuppressWarnings("JavaDoc")
        @Override
        public Requester<Callback<D>> getDefaultRequester() {
            return getRequester(new RequesterHelper<Callback<D>, T>(mType) {
                @Override
                public void init() {
                    mMethod  = findMethod(mRetrofit.getService(), mType);
                    mHandler = mRetrofit.getApi();
                }

                @Override
                public void request(Callback<D> callback) throws Exception {
                    final Object result;
                    try {
                        result = CoreReflection.invoke(mHandler, mMethod);
                    }
                    catch (InvocationTargetException exception) {
                        CoreLogger.logError("please check your build.gradle: maybe " +
                                "\"compile 'com.squareup.retrofit2:adapter-rxjava:...'\" and / or " +
                                "\"compile 'com.squareup.retrofit2:adapter-rxjava2:...'\" are missing");
                        throw exception;
                    }
                    if (result == null) handleRequestError("result == null");

                    if (result instanceof Call) {
                        @SuppressWarnings("unchecked")
                        final Call<D> call = (Call<D>) result;

                        call.enqueue(callback);
                        return;
                    }

                    if (!Retrofit2CoreLoadBuilder.checkRxComponent(mRx)) return;

                    final CallbackRx<D> callbackRx = Retrofit2CoreLoadBuilder.getRxWrapper(callback);

                    Object resultRx = Rx2.handle(result, callbackRx);
                    if (resultRx != null) {
                        mRx.getRx().getRx2DisposableHandler().add(resultRx);
                        return;
                    }

                    resultRx = Rx.handle(result, callbackRx);
                    if (resultRx != null) {
                        mRx.getRx().getRxSubscriptionHandler().add(resultRx);
                        return;
                    }

                    handleRequestError("unknown " + result.getClass() + " (usually in Retrofit API)");
                }

                private void handleRequestError(@NonNull final String text) throws Exception {
                    throw new Exception(text);
                }
            });
        }

        /** @exclude */ @SuppressWarnings("JavaDoc")
        @Override
        protected Type getTypeHelper() {
            return getType(findMethod());
        }

        /** @exclude */ @SuppressWarnings("JavaDoc")
        @NonNull
        @Override
        protected Retrofit2LoaderWrapper<D> createLoaderWrapper() {
            final Retrofit2LoaderWrapper<D> loaderWrapper = new Retrofit2LoaderWrapper<>(getContext(),
                    getFragment(), mLoaderId, getRequester(), getTimeout(), getTableName(),
                    mDescription, getConverter(), getUriResolver());
            loaderWrapper.mMethod = findMethod();
            return loaderWrapper;
        }

        @SuppressWarnings("unchecked")
        private Method findMethod() {
            return findMethod(mRetrofit.getService(), mType);
        }

        private static <T> Method findMethod(@NonNull final Class<T> service,
                                             @NonNull final Type     typeResponse) {
            for (final Method method: service.getMethods())
                if (TypeHelper.checkType(typeResponse, getType(method)))
                    return method;
            return null;
        }

        /** @exclude */ @SuppressWarnings("JavaDoc")
        public static Type getType(final Method method) {
            return TypeHelper.getParameterizedType(method == null ? null: method.getGenericReturnType());
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Builder class for {@yakhont.link BaseResponseLoaderWrapper.CoreLoad} objects. Creates the Retrofit2-based ones.
     *
     * @param <D>
     *        The type of data
     *
     * @param <T>
     *        The type of Retrofit 2 API
     */
    public static class Retrofit2CoreLoadBuilder<D, T> extends CoreLoadExtendedBuilder<Callback<D>, Response<D>, Throwable, D, T> {

        private final Retrofit2<T>      mRetrofit;

        /**
         * Initialises a newly created {@code Retrofit2CoreLoadBuilder} object.
         *
         * @param fragment
         *        The fragment
         *
         * @param type
         *        The type of data
         *
         * @param retrofit
         *        The Retrofit2 component
         */
        @SuppressWarnings("unused")
        public Retrofit2CoreLoadBuilder(@NonNull final Fragment fragment, @NonNull final Class<D> type,
                                        @NonNull final Retrofit2<T> retrofit) {
            this(fragment, (Type) type, retrofit);
        }

        /**
         * Initialises a newly created {@code Retrofit2CoreLoadBuilder} object.
         *
         * @param fragment
         *        The fragment
         *
         * @param type
         *        The type of data; for generic {@link java.util.Collection} types please use {@link TypeToken}
         *
         * @param retrofit
         *        The Retrofit2 component
         */
        @SuppressWarnings("unused")
        public Retrofit2CoreLoadBuilder(@NonNull final Fragment fragment, @NonNull final Type type,
                                        @NonNull final Retrofit2<T> retrofit) {
            super(fragment, type);
            mRetrofit = retrofit;
        }

        /**
         * Initialises a newly created {@code Retrofit2CoreLoadBuilder} object.
         *
         * @param fragment
         *        The fragment
         *
         * @param type
         *        The type of data; intended to use with generic {@link java.util.Collection} types,
         *        e.g. {@code new com.google.gson.reflect.TypeToken<List<MyData>>() {}}
         *
         * @param retrofit
         *        The Retrofit2 component
         */
        @SuppressWarnings("unused")
        public Retrofit2CoreLoadBuilder(@NonNull final Fragment fragment, @NonNull final TypeToken type,
                                        @NonNull final Retrofit2<T> retrofit) {
            super(fragment, type);
            mRetrofit = retrofit;
        }
/*
        / @exclude / @SuppressWarnings({"JavaDoc", "WeakerAccess", "unused"})
        public Retrofit2CoreLoadBuilder(
                @NonNull final CoreLoadExtendedBuilder<Callback<D>, Response<D>, Throwable, D> src,
                @NonNull final Retrofit2<T> retrofit) {
            super(src);
            mRetrofit = retrofit;
        }
*/
        /** @exclude */ @SuppressWarnings("JavaDoc")
        @Override
        protected void customizeAdapterWrapper(@NonNull final CoreLoad coreLoad, @NonNull final View root,
                                               @NonNull final View list, @LayoutRes final int item) {
            setAdapterWrapper(mFrom == null ? new Retrofit2AdapterWrapper<D>(mFragment.get().getActivity(), item):
                    new Retrofit2AdapterWrapper<D>(mFragment.get().getActivity(), item, mFrom, mTo));
        }

        /**
         * Please refer to the base method description.
         */
        @Override
        public T getApi() {
            return mRetrofit.getApi();
        }

        /**
         * Returns the {@link Rx2Disposable} component.
         *
         * @return  The {@link Rx2Disposable}
         */
        @SuppressWarnings("unused")
        public Rx2Disposable getRx2DisposableHandler() {
            checkRxComponent(mRx);
            return mRx == null ? null: mRx.getRx().getRx2DisposableHandler();
        }

        /** @exclude */ @SuppressWarnings({"JavaDoc", "WeakerAccess"})
        public static <D> boolean checkRxComponent(final LoaderRx<Response<D>, Throwable, D> rx) {
            if (rx == null) CoreLogger.logError("Rx component was not defined");
            return rx != null;
        }

        /**
         * Returns the {@link RxSubscription} component.
         *
         * @return  The {@link RxSubscription}
         */
        @SuppressWarnings("unused")
        public RxSubscription getRxSubscriptionHandler() {
            checkRxComponent(mRx);
            return mRx == null ? null: mRx.getRx().getRxSubscriptionHandler();
        }

        /**
         * Creates {@link CallbackRx} from {@link Callback}.
         *
         * @param callback
         *        The {@link Callback}
         *
         * @param <D>
         *        The type of data
         *
         * @return  The {@link CallbackRx}
         */
        public static <D> CallbackRx<D> getRxWrapper(final Callback<D> callback) {
            if (callback == null) CoreLogger.logError("callback == null");

            return callback == null ? null: new CallbackRx<D>() {
                @Override
                public void onResult(D result) {
                    callback.onResponse(null, Response.success(result));
                }

                @Override
                public void onError(Throwable throwable) {
                    callback.onFailure(null, throwable);
                }
            };
        }

        /**
         * Please refer to the base method description.
         */
        @Override
        public CoreLoad create() {
            return create(new Retrofit2LoaderBuilder<>(mFragment.get(), mType, mRetrofit, mRx));
        }
    }
}

package com.duriana.cloudbox;


import android.content.Context;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.duriana.cloudbox.Request.CheckFileVersionRequest;
import com.duriana.cloudbox.Request.DownloadFileRequest;
import com.lifeofcoding.cacheutlislibrary.CacheUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;

/**
 * Created by zuhairkhj on 20/10/2016.
 */
public class CloudBox {
    private final String DEBUG_TAG = "CloudBox";
    private String metaPrefix = "/GBCloudBoxResourcesMeta/";
    private final String FILE_SYSTEM_PATH = "/files/";
    private static CloudBox instance;
    private String domain = "";
    private boolean logEnabled = true;
    public enum RESULT {UP_TO_DATE, FETCHED};

    protected CloudBox() {

    }

    public synchronized static CloudBox getInstance(Context context, String domain, String metaPrefix) {
        if (instance == null) {
            instance = new CloudBox();
            instance.domain = domain;
            instance.metaPrefix = metaPrefix;
            CacheUtils.configureCache(context);
        }
        return instance;
    }

    public static CloudBox getInstance(Context context, String domain) {
        if (instance == null) {
            instance = new CloudBox();
            instance.domain = domain;
            CacheUtils.configureCache(context);
        }
        return instance;
    }

    public Observable<RESULT> getFileFromServerRX(final Context context, final String fileName, final String fileExtensionOnStorage) {
        Observable<RESULT> observableResult = Observable.create(new Observable.OnSubscribe<RESULT>() {
            @Override
            public void call(Subscriber<? super RESULT> subscriber) {

                CheckFileVersionRequest fileVersionService = ServiceGenerator.createService(domain + metaPrefix, CheckFileVersionRequest.class);
                Call<CloudBoxFileMeta> metaCall = fileVersionService.getVersion(fileName + fileExtensionOnStorage);
                try {
                    Response<CloudBoxFileMeta> response = metaCall.execute();
                    if (response.isSuccessful()) {
                        if (logEnabled)
                            Log.d(DEBUG_TAG, String.format(Locale.ENGLISH, "%s :: Server contacted and has file %s", fileName, response.body().getUrl()));
                        final int serverVersion = response.body().getVersion();
                        final String fileUrl = response.body().getUrl();
                        final String serverMd5 = response.body().getMd5();
                        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
                        final int currentFileVersion = prefs.getInt(fileName, 0);
                        if (logEnabled)
                            Log.d(DEBUG_TAG, String.format(Locale.ENGLISH, "%s :: Version on Device %d", fileName, currentFileVersion));
                        if (serverVersion > currentFileVersion) {
                            DownloadFileRequest downloadService = ServiceGenerator.createService(domain + metaPrefix, DownloadFileRequest.class);

                            Call<ResponseBody> downloadCall = downloadService.downloadFile(fileUrl);
                            Response<ResponseBody> downloadResponse = downloadCall.execute();

                            if (response.isSuccessful()) {

                                    String content = downloadResponse.body().string();
                                    if (logEnabled)
                                        Log.d(DEBUG_TAG, String.format(Locale.ENGLISH, "%s :: File Download Success %s", fileName, content));
                                    if (downloadResponse.body().contentLength() > 2) {
                                        String MD5 = md5(content);
                                        if (!MD5.equals("")) {
                                            if (MD5.equals(serverMd5)) {
                                                CacheUtils.writeFile(fileName, content);
                                                prefs.edit().putInt(fileName, serverVersion).commit();

                                                if (logEnabled)
                                                    Log.d(DEBUG_TAG, String.format(Locale.ENGLISH, "%s :: File updated to version %d", fileName, serverVersion));

                                                subscriber.onNext(RESULT.FETCHED);
                                                subscriber.onCompleted();
                                            }
                                            else {
                                                subscriber.onError(new RuntimeException(String.format(Locale.ENGLISH, "%s :: Hash mismatch", fileName)));
                                            }
                                        } else {
                                            subscriber.onError(new RuntimeException(String.format(Locale.ENGLISH, "%s :: Empty Hash", fileName)));
                                        }
                                    } else {
                                        subscriber.onError(new RuntimeException(String.format(Locale.ENGLISH, "%s :: File too short content = %s", fileName, content)));
                                    }
                            } else {
                                String errorMessage = String.format(Locale.ENGLISH, "%s :: Server contact failed, Code: %d Msg: %s", fileName, response.code(), response.errorBody().toString());
                                if (logEnabled)
                                    Log.d(DEBUG_TAG, errorMessage);
                                downloadResponse.body().close();
                                subscriber.onError(new RuntimeException(errorMessage));
                            }
                        }

                        else {
                            Log.d(DEBUG_TAG, String.format(Locale.ENGLISH, "%s :: You already have latest version from this file V.%d", fileName, serverVersion));
                            subscriber.onNext(RESULT.UP_TO_DATE);
                            subscriber.onCompleted();
                        }

                    } else {
                        String errorMessage = String.format(Locale.ENGLISH, "%s :: Server contact failed, Code: %d Msg: %s", fileName, response.code(), response.errorBody().toString());
                        if (logEnabled)
                            Log.d(DEBUG_TAG, errorMessage);
                        subscriber.onError(new RuntimeException(errorMessage));
                    }

                } catch (IOException ioException) {
                    subscriber.onError(ioException);
                }
            }
        });
        return observableResult.subscribeOn(Schedulers.newThread());
    }

    public String getFileAsString(final Context context, final String fileName,
                                  final String fileExtension) {

        String rawFileString = null;

        if (logEnabled)
            Log.d(DEBUG_TAG, "Getting file from asset first");

        try {

            InputStream inputStream = context.getAssets().open(
                    fileName + fileExtension);

            int sizeOfJSONFile = inputStream.available();
            byte[] bytes = new byte[sizeOfJSONFile];
            inputStream.read(bytes);
            inputStream.close();

            rawFileString = new String(bytes, "UTF-8");

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        if (CacheUtils.hasCache(fileName)) {
            if (logEnabled)
                Log.d(DEBUG_TAG, "Getting file from storage");

            rawFileString = CacheUtils.readFile(fileName);
        }
        return rawFileString;
    }

    private String md5(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(input.getBytes("UTF-8"));
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < array.length; i++) {
                sb.append(String.format("%02x", array[i]));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            return "";
        }

    }

    public CloudBox setLogEnabled(boolean logEnabled) {
        this.logEnabled = logEnabled;
        return this;
    }

    public String getDomain() {
        return domain;
    }

    public CloudBox setDomain(String domain) {
        this.domain = domain;
            return this;
    }

    public String getMetaPrefix() {
        return metaPrefix;
    }

    public CloudBox setMetaPrefix(String metaPrefix) {
        this.metaPrefix = metaPrefix;
        return this;
    }
}
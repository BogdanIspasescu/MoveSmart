package eu.ase.ro.aplicatielicenta.httprequest;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import eu.ase.ro.aplicatielicenta.interfaces.Callback;

public class AsyncTaskRunner {

    private final Executor executor = Executors.newCachedThreadPool();
    private final Handler handler = new Handler(Looper.getMainLooper());

    public <R> void executeAsync(Callable<R> asyncOperation, Callback<R> mainThreadOperation) {
        try {
            executor.execute(runAsyncOperation(asyncOperation, mainThreadOperation));
        } catch (Exception e) {
            Log.e("AsyncTaskRunner", "failed call executeAsync " + e.getMessage());
        }
    }

    private <R> Runnable runAsyncOperation(Callable<R> asyncOperation, Callback<R> mainThreadOperation) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    R result = asyncOperation.call();
                    handler.post(runMainThreadOperation(result, mainThreadOperation));
                } catch (Exception e) {
                    Log.e("AsyncTaskRunner", "failed call runAsyncOperation " + e.getMessage());
                }
            }
        };
    }

    private <R> Runnable runMainThreadOperation(R result, Callback<R> mainThreadOperation) {
        return new Runnable() {
            @Override
            public void run() {
                mainThreadOperation.runResultOnUiThread(result);
            }
        };
    }
}

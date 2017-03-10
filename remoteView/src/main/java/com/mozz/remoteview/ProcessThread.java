package com.mozz.remoteview;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.mozz.remoteview.common.MainHandler;
import com.mozz.remoteview.common.WefRunnable;

import java.io.InputStream;

/**
 * @author Yang Tao, 17/3/10.
 */

final class ProcessThread {

    private static final String TAG = ProcessThread.class.getSimpleName();

    private ProcessThread() {

    }

    // for running render task
    private static HandlerThread mRenderThread = new HandlerThread("RVRenderThread");
    private static Handler mRenderHandler;

    static void init() {
        mRenderThread.start();
        mRenderHandler = new Handler(mRenderThread.getLooper());
    }

    static void quit() {
        mRenderThread.quit();
    }

    static void runRenderTask(RenderTask r) {
        mRenderHandler.post(r);
    }

    static final class RenderTask extends WefRunnable<Context> {

        private InputStream mFileSource;
        private RV.OnRViewLoaded mCallback;

        RenderTask(Context context, InputStream fileSource, RV.OnRViewLoaded callback) {
            super(context);
            mFileSource = fileSource;
            mCallback = callback;
        }

        @Override
        protected void runOverride(final Context context) {
            try {
                if (context == null)
                    return;

                final RVModule module = RVModule.load(mFileSource);

                Log.d(TAG, module.mRootTree.wholeTreeToString());

                final ViewGroup.LayoutParams layoutParams =
                        new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT);
                MainHandler.instance().post(new Runnable() {
                    @Override
                    public void run() {
                        View v = null;
                        try {
                            v = RVRenderer.get().inflate(context, module, layoutParams);
                        } catch (RVRenderer.RemoteInflateException e) {
                            e.printStackTrace();
                        }

                        if (mCallback != null)
                            mCallback.onViewLoaded(v);
                    }
                });
            } catch (final RVSyntaxError e) {
                e.printStackTrace();
                if (mCallback != null) {
                    MainHandler.instance().post(new Runnable() {
                        @Override
                        public void run() {
                            if (mCallback != null)
                                mCallback.onError(e);
                        }
                    });
                }
            }
        }
    }
}

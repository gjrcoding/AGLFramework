package com.aglframework.smzh;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import com.aglframework.smzh.filter.RenderScreenFilter;

import java.util.LinkedList;
import java.util.Queue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class AGLView extends GLSurfaceView {

    private AGLRenderer renderer;

    public AGLView(Context context) {
        super(context);
        init();
    }

    public AGLView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init();
    }

    public void init() {
        if (renderer == null) {
            renderer = new AGLRenderer();
            setEGLContextClientVersion(2);
            setEGLConfigChooser(8, 8, 8, 8, 16, 0);
            getHolder().setFormat(PixelFormat.RGBA_8888);
            setRenderer(renderer);
            setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        }
    }

    public void setRendererSource(ISource rendererSource) {
        renderer.setSource(rendererSource);
        requestRender();
    }

    public void clear() {
        renderer.clear();
        requestRender();
    }

    public void setFilter(final IFilter filter) {
        renderer.setFilter(filter);
        requestRender();
    }

    public void setDisabled(boolean disable) {
        renderer.setDisable(disable);
        requestRender();
    }

    public int getImageWidth() {
        if (renderer.getSource() != null) {
            return renderer.getSource().getWidth();
        } else {
            return 0;
        }
    }

    public int getImageHeight() {
        if (renderer.getSource() != null) {
            return renderer.getSource().getHeight();
        } else {
            return 0;
        }
    }


    private class AGLRenderer implements GLSurfaceView.Renderer {

        private ISource iSource;
        private RenderScreenFilter screenFilter;
        private final Queue<Runnable> runOnDraw;
        private final Queue<Runnable> runOnDrawEnd;
        private IFilter filter;
        private boolean disable;
        private int outWidth;
        private int outHeight;

        AGLRenderer() {
            screenFilter = new RenderScreenFilter(getContext());
            runOnDraw = new LinkedList<>();
            runOnDrawEnd = new LinkedList<>();
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            this.outWidth = width;
            this.outHeight = height;
            screenFilter.setOutSize(width, height);
        }


        @Override
        public void onDrawFrame(GL10 gl) {
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

            runAll(runOnDraw);

            if (iSource != null) {
                IFilter.Frame frame = iSource.createFrame();

                if (filter != null && !disable) {
                    frame = filter.draw(frame);
                }

                screenFilter.setVerticesCoordination(Transform.adjustVetices(frame.getTextureWidth(),frame.getTextureHeight(),outWidth,outHeight));
                screenFilter.draw(frame);
            }

            runAll(runOnDrawEnd);
        }

        void setSource(ISource iSource) {
            this.iSource = iSource;
        }

        void clear() {
            if (iSource != null) {
                iSource.destroy();
                iSource = null;
            }

            if (screenFilter != null) {
                screenFilter.destroy();
            }

        }

        void setFilter(final IFilter filter) {
            runOnDraw(new Runnable() {
                @Override
                public void run() {
                    final IFilter oldFilter = AGLRenderer.this.filter;
                    AGLRenderer.this.filter = filter;
                    if (oldFilter != null) {
                        oldFilter.destroy();
                    }
                }
            });
        }

        private void runAll(Queue<Runnable> queue) {
            synchronized (queue) {
                while (!queue.isEmpty()) {
                    queue.poll().run();
                }
            }
        }

        private void runOnDraw(final Runnable runnable) {
            synchronized (runOnDraw) {
                runOnDraw.add(runnable);
            }
        }

        protected void runOnDrawEnd(final Runnable runnable) {
            synchronized (runOnDrawEnd) {
                runOnDrawEnd.add(runnable);
            }
        }

        void setDisable(boolean disable) {
            this.disable = disable;
        }

        public ISource getSource() {
            return iSource;
        }
    }

}

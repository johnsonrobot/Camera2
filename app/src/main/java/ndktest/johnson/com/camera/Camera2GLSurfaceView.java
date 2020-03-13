package ndktest.johnson.com.camera;

import android.content.Context;
import android.opengl.GLSurfaceView;

public class Camera2GLSurfaceView extends GLSurfaceView {
    public static final String TAG = "Filter_CameraV2GLSurfaceView";
    private MyRenderer myRenderer;

    public void init(Camera2 camera, boolean isPreviewStarted, Context context) {
        setEGLContextClientVersion(2);

        myRenderer = new MyRenderer();
        myRenderer.init(this, camera, isPreviewStarted, context);

        setRenderer(myRenderer);
    }

    public Camera2GLSurfaceView(Context context) {
        super(context);
    }

}

package jp.co.cyberagent.android.gpuimage;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;

public abstract class GLTextureViewHelper extends TextureView {


    public GLTextureViewHelper(Context context) {
        super(context);
    }

    public GLTextureViewHelper(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GLTextureViewHelper(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public GLTextureViewHelper(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    protected abstract void onFinalize();

    @Override
    protected void finalize() {
        onFinalize();
    }

}

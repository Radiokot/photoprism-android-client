package ua.com.radiokot.photoprism.util.images;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;

import com.squareup.picasso.Transformation;

/**
 * Created by codezjx on 2016/5/4.
 */
public class CircleImageTransformation implements Transformation {

    /**
     * A unique key for the transformation, used for caching purposes.
     */
    private static final String KEY = "circleImageTransformation";

    public static final CircleImageTransformation INSTANCE = new CircleImageTransformation();

    @Override
    public Bitmap transform(Bitmap source) {

        int minEdge = Math.min(source.getWidth(), source.getHeight());
        int dx = (source.getWidth() - minEdge) / 2;
        int dy = (source.getHeight() - minEdge) / 2;

        // Init shader
        Shader shader = new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        Matrix matrix = new Matrix();
        matrix.setTranslate(-dx, -dy);   // Move the target area to center of the source bitmap
        shader.setLocalMatrix(matrix);

        // Init paint
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(shader);

        // Create and draw circle bitmap
        Bitmap output = Bitmap.createBitmap(minEdge, minEdge, source.getConfig());
        Canvas canvas = new Canvas(output);
        canvas.drawOval(new RectF(0, 0, minEdge, minEdge), paint);

        // Recycle the source bitmap, because we already generate a new one
        source.recycle();

        return output;
    }

    @Override
    public String key() {
        return KEY;
    }
}

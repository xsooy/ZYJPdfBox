package com.example.test;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.rendering.ImageType;
import com.tom_roush.pdfbox.rendering.PDFRenderer;
import com.tom_roush.pdfbox.rendering.PageDrawer;
import com.tom_roush.pdfbox.rendering.PageDrawerParameters;
import com.tom_roush.pdfbox.rendering.RenderDestination;

import java.io.IOException;

public class YJPDFRenderer extends PDFRenderer {
    /**
     * Creates a new PDFRenderer.
     *
     * @param document the document to render
     */
    public YJPDFRenderer(PDDocument document) {
        super(document);
    }

    public Bitmap renderImage(Bitmap bitmap, int pageIndex)
            throws IOException
    {

        PDPage page = document.getPage(pageIndex);

        PDRectangle cropbBox = page.getCropBox();
        float widthPt = cropbBox.getWidth();
        float heightPt = cropbBox.getHeight();

        float scale = bitmap.getWidth() / widthPt;

        // PDFBOX-4306 avoid single blank pixel line on the right or on the bottom
//        int widthPx = (int) Math.max(Math.floor(widthPt * scale), 1);
//        int heightPx = (int) Math.max(Math.floor(heightPt * scale), 1);

//        int rotationAngle = page.getRotation();

        // use a transparent background if the image type supports alpha
        Paint paint = new Paint();
        Canvas canvas = new Canvas(bitmap);
//        paint.setColor(Color.BLUE);
//        paint.setStyle(Paint.Style.FILL);
//        canvas.drawRect(0, 0, bitmap.getWidth(), bitmap.getHeight(), paint);
//        paint.reset();

        transform(canvas, page, scale, scale);

        // the end-user may provide a custom PageDrawer
        PageDrawerParameters parameters = new PageDrawerParameters(this, page, subsamplingAllowed,
                RenderDestination.EXPORT);
        YJPageDrawer drawer = createPageDrawer(parameters);
        drawer.setScale(scale);
        drawer.drawAnnotation(paint, canvas, page.getCropBox());

        return bitmap;
    }

    protected YJPageDrawer createPageDrawer(PageDrawerParameters parameters) throws IOException
    {
        YJPageDrawer pageDrawer = new YJPageDrawer(parameters);
        pageDrawer.setAnnotationFilter(annotationFilter);
        return pageDrawer;
    }

}

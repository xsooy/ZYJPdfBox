package com.example.test;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.tom_roush.harmony.awt.geom.AffineTransform;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import com.tom_roush.pdfbox.rendering.PageDrawer;
import com.tom_roush.pdfbox.rendering.PageDrawerParameters;

import java.io.IOException;

public class YJPageDrawer extends PageDrawer {
    /**
     * Constructor.
     *
     * @param parameters Parameters for page drawing.
     * @throws IOException If there is an error loading properties from the file.
     */
    public YJPageDrawer(PageDrawerParameters parameters) throws IOException {
        super(parameters);
    }

    public void drawAnnotation(Paint p, Canvas c, PDRectangle pageSize) throws IOException
    {
        paint = p;
        canvas = c;
        xform = new AffineTransform(canvas.getMatrix());
        this.pageSize = pageSize;

        setRenderingHints();

        canvas.translate(0, pageSize.getHeight());
        canvas.scale(1, -1);

        // adjust for non-(0,0) crop box
        canvas.translate(-pageSize.getLowerLeftX(), -pageSize.getLowerLeftY());
        canvas.save();

        initPage(getPage());
        for (PDAnnotation annotation : getPage().getAnnotations(annotationFilter))
        {
            showAnnotation(annotation);
        }
    }

}

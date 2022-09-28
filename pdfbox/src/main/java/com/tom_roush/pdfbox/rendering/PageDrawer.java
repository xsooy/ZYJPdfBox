/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tom_roush.pdfbox.rendering;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Shader;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;

import com.tom_roush.harmony.awt.geom.AffineTransform;
import com.tom_roush.pdfbox.contentstream.PDFGraphicsStreamEngine;
import com.tom_roush.pdfbox.cos.COSArray;
import com.tom_roush.pdfbox.cos.COSBase;
import com.tom_roush.pdfbox.cos.COSDictionary;
import com.tom_roush.pdfbox.cos.COSName;
import com.tom_roush.pdfbox.cos.COSNumber;
import com.tom_roush.pdfbox.pdmodel.PDResources;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.common.function.PDFunction;
import com.tom_roush.pdfbox.pdmodel.documentinterchange.markedcontent.PDPropertyList;
import com.tom_roush.pdfbox.pdmodel.font.PDCIDFontType0;
import com.tom_roush.pdfbox.pdmodel.font.PDCIDFontType2;
import com.tom_roush.pdfbox.pdmodel.font.PDFont;
import com.tom_roush.pdfbox.pdmodel.font.PDTrueTypeFont;
import com.tom_roush.pdfbox.pdmodel.font.PDType0Font;
import com.tom_roush.pdfbox.pdmodel.font.PDType1CFont;
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font;
import com.tom_roush.pdfbox.pdmodel.graphics.PDLineDashPattern;
import com.tom_roush.pdfbox.pdmodel.graphics.PDXObject;
import com.tom_roush.pdfbox.pdmodel.graphics.blend.BlendMode;
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDColor;
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDColorSpace;
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDDeviceCMYK;
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDDeviceGray;
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDPattern;
import com.tom_roush.pdfbox.pdmodel.graphics.form.PDFormXObject;
import com.tom_roush.pdfbox.pdmodel.graphics.form.PDTransparencyGroup;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImage;
import com.tom_roush.pdfbox.pdmodel.graphics.optionalcontent.PDOptionalContentGroup;
import com.tom_roush.pdfbox.pdmodel.graphics.optionalcontent.PDOptionalContentGroup.RenderState;
import com.tom_roush.pdfbox.pdmodel.graphics.pattern.PDAbstractPattern;
import com.tom_roush.pdfbox.pdmodel.graphics.pattern.PDShadingPattern;
import com.tom_roush.pdfbox.pdmodel.graphics.pattern.PDTilingPattern;
import com.tom_roush.pdfbox.pdmodel.graphics.shading.AxialShadingContext;
import com.tom_roush.pdfbox.pdmodel.graphics.shading.PDShading;
import com.tom_roush.pdfbox.pdmodel.graphics.shading.PDShadingType2;
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDGraphicsState;
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDSoftMask;
import com.tom_roush.pdfbox.pdmodel.graphics.state.RenderingMode;
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.AnnotationFilter;
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationMarkup;
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationUnknown;
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;
import com.tom_roush.pdfbox.util.Matrix;
import com.tom_roush.pdfbox.util.Vector;

/**
 * Paints a page in a PDF document to a Canvas context. May be subclassed to provide custom
 * rendering.
 *
 * <p>
 * If you want to do custom graphics processing rather than Canvas rendering, then you should
 * subclass {@link PDFGraphicsStreamEngine} instead. Subclassing PageDrawer is only suitable for
 * cases where the goal is to render onto a {@link Canvas} surface. In that case you'll also
 * have to subclass {@link PDFRenderer} and modify
 * {@link PDFRenderer#createPageDrawer(PageDrawerParameters)}.
 *
 * @author Ben Litchfield
 */
public class PageDrawer extends PDFGraphicsStreamEngine
{
    // parent document renderer - note: this is needed for not-yet-implemented resource caching
    private final PDFRenderer renderer;

    private final boolean subsamplingAllowed;

    // the graphics device to draw to, xform is the initial transform of the device (i.e. DPI)
    protected Paint paint;
    protected Canvas canvas;
    protected AffineTransform xform;

    // the page box to draw (usually the crop box but may be another)
    protected PDRectangle pageSize;

    // whether image of a transparency group must be flipped
    // needed when in a tiling pattern
    private boolean flipTG = false;

    // clipping winding rule used for the clipping path
    private Path.FillType clipWindingRule = null;
    private Path linePath = new Path();

    // last clipping path
    private Region lastClip;

    // shapes of glyphs being drawn to be used for clipping
    private List<Path> textClippings;

    // glyph cache
    private final Map<PDFont, Glyph2D> fontGlyph2D = new HashMap<PDFont, Glyph2D>();

    private PointF currentPoint = new PointF();

    private final Stack<TransparencyGroup> transparencyGroupStack = new Stack<TransparencyGroup>();

    // if greater zero the content is hidden and wil not be rendered
    private int nestedHiddenOCGCount;

    private final RenderDestination destination;

    static final int JAVA_VERSION = PageDrawer.getJavaVersion();

    /**
     * Default annotations filter, returns all annotations
     */
    protected AnnotationFilter annotationFilter = new AnnotationFilter()
    {
        @Override
        public boolean accept(PDAnnotation annotation)
        {
            return true;
        }
    };

    /**
     * Constructor.
     *
     * @param parameters Parameters for page drawing.
     * @throws IOException If there is an error loading properties from the file.
     */
    public PageDrawer(PageDrawerParameters parameters) throws IOException
    {
        super(parameters.getPage());
        this.renderer = parameters.getRenderer();
        this.subsamplingAllowed = parameters.isSubsamplingAllowed();
        this.destination = parameters.getDestination();
    }

    /**
     * Return the AnnotationFilter.
     *
     * @return the AnnotationFilter
     */
    public AnnotationFilter getAnnotationFilter()
    {
        return annotationFilter;
    }

    /**
     * Set the AnnotationFilter.
     *
     * <p>Allows to only render annotation accepted by the filter.
     *
     * @param annotationFilter the AnnotationFilter
     */
    public void setAnnotationFilter(AnnotationFilter annotationFilter)
    {
        this.annotationFilter = annotationFilter;
    }

    /**
     * Returns the parent renderer.
     */
    public final PDFRenderer getRenderer()
    {
        return renderer;
    }

    /**
     * Returns the underlying Canvas. May be null if drawPage has not yet been called.
     */
    protected final Canvas getCanvas()
    {
        return canvas;
    }

    /**
     * Returns the current line path. This is reset to empty after each fill/stroke.
     */
    protected final Path getLinePath()
    {
        return linePath;
    }

    /**
     * Sets high-quality rendering hints on the current Canvas.
     */
    protected void setRenderingHints()
    {
//        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
//            RenderingHints.VALUE_INTERPOLATION_BICUBIC);
//        graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
//            RenderingHints.VALUE_RENDER_QUALITY);
        paint.setAntiAlias(true);
    }


    /**
     * Draws the page to the requested Canvas.
     *
     * @param p The paint.
     * @param c The canvas to draw onto.
     * @param pageSize The size of the page to draw.
     * @throws IOException If there is an IO error while drawing the page.
     */
    public void drawPage(Paint p, Canvas c, PDRectangle pageSize) throws IOException
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

        processPage(getPage());

        for (PDAnnotation annotation : getPage().getAnnotations(annotationFilter))
        {
            showAnnotation(annotation);
        }
    }

//    void drawTilingPattern(Graphics2D g, PDTilingPattern pattern, PDColorSpace colorSpace,
//        PDColor color, Matrix patternMatrix) throws IOException TODO: PdfBox-Android

    private float clampColor(float color)
    {
        return color < 0 ? 0 : (color > 1 ? 1 : color);
    }

//    protected Paint getPaint(PDColor color) throws IOException TODO: PdfBox-Android

    // returns an integer for color that Android understands from the PDColor
    private int getColor(PDColor color) throws IOException {
        PDColorSpace colorSpace = color.getColorSpace();
        //Log.w("ceshi","hadingColorSpace.toRGB::"+colorSpace.getClass().getSimpleName());
        if (colorSpace instanceof PDPattern) {
//            PDAbstractPattern pattern = ((PDPattern)colorSpace).getPattern(color);
//            Log.w("ceshi","getColor:"+pattern.getClass().getSimpleName());
//            if (pattern instanceof PDTilingPattern) {
//                return Color.argb(0,0,0,0);
//            } else {
//
//            }
//            if(color.getComponents().length==4) {
//                float[] floats = color.getComponents();
//                return Color.argb(Math.round(floats[0] * 255),Math.round(floats[1] * 255),Math.round(floats[2] * 255),Math.round(floats[3] * 255));
//            }
            return Color.rgb(255,255,255);
        }

        float[] floats = colorSpace.toRGB(color.getComponents());
        int r = Math.round(floats[0] * 255)&0xff;
        int g = Math.round(floats[1] * 255)&0xff;
        int b = Math.round(floats[2] * 255)&0xff;
        //Log.w("ceshi","r:"+r+",g:"+g+",b:"+b);
        return Color.rgb(r, g, b);
    }

    // sets the clipping path using caching for performance, we track lastClip manually because
    // Graphics2D#getClip() returns a new object instead of the same one passed to setClip
    private void setClip()
    {
        Region clippingPath = getGraphicsState().getCurrentClippingPath();
        if (clippingPath != lastClip)
        {
//            Log.w("ceshi","canvas.clipPath:"+clippingPath);
            Path path = clippingPath.getBoundaryPath();
//            android.graphics.Matrix matrix = new android.graphics.Matrix();
//            matrix.setScale(1/scale,1/scale);
//            path.transform(matrix);
            canvas.clipPath(path);//TODO: PdfBox-Android
//            lastClip.setEmpty();
            lastClip = clippingPath;
        }
    }

    private void setClip2()
    {
        Region clippingPath = getGraphicsState().getCurrentClippingPath();
//        Log.w("ceshi","clippingPath:"+clippingPath);
        if (clippingPath != lastClip)
        {
//            Log.w("ceshi","canvas.clipPath:"+clippingPath);
            Path path = clippingPath.getBoundaryPath();
            android.graphics.Matrix matrix = new android.graphics.Matrix();
            matrix.setScale(1/scale,1/scale);
            path.transform(matrix);
            RectF rectF = new RectF();
            path.computeBounds(rectF,true);
//            Log.w("ceshi","setClip2====="+rectF);
            canvas.clipPath(path);//TODO: PdfBox-Android
//            lastClip.setEmpty();
            lastClip = clippingPath;
        }
    }

    @Override
    public void beginText() throws IOException
    {
        canvas.save();
//        setClip2();
        beginTextClip();
    }

    @Override
    public void endText() throws IOException
    {
        endTextClip();
        canvas.restore();
    }

    /**
     * Begin buffering the text clipping path, if any.
     */
    private void beginTextClip()
    {
        // buffer the text clippings because they represents a single clipping area
        textClippings = new ArrayList<Path>();
    }

    /**
     * End buffering the text clipping path, if any.
     */
    private void endTextClip()
    {
        PDGraphicsState state = getGraphicsState();
        RenderingMode renderingMode = state.getTextState().getRenderingMode();

        // apply the buffered clip as one area
        if (renderingMode.isClip() && !textClippings.isEmpty())
        {
            // PDFBOX-4150: this is much faster than using textClippingArea.add(new Area(glyph))
            // https://stackoverflow.com/questions/21519007/fast-union-of-shapes-in-java
            Path path = new Path();
            for (Path shape : textClippings)
            {
                path.addPath(shape);
            }
//            Log.w("ceshi","intersectClippingPath222");
            state.intersectClippingPath(path);
            textClippings = new ArrayList<Path>();

            // PDFBOX-3681: lastClip needs to be reset, because after intersection it is still the same 
            // object, thus setClip() would believe that it is cached.
            lastClip = null;
        }
    }

    @Override
    protected void showFontGlyph(Matrix textRenderingMatrix, PDFont font, int code, String unicode,
        Vector displacement) throws IOException
    {
//        Log.w("ceshi","unicode:"+unicode);
        AffineTransform at = textRenderingMatrix.createAffineTransform();
        at.concatenate(font.getFontMatrix().createAffineTransform());

        Glyph2D glyph2D = createGlyph2D(font);
//        Log.w("ceshi","glyph2D:::"+glyph2D.getClass().getSimpleName());
        drawGlyph2D(glyph2D, font, code, displacement, at);
    }

    /**
     * Render the font using the Glyph2D interface.
     *
     * @param glyph2D the Glyph2D implementation provided a Path for each glyph
     * @param font the font
     * @param code character code
     * @param displacement the glyph's displacement (advance)
     * @param at the transformation
     * @throws IOException if something went wrong
     */
    private void drawGlyph2D(Glyph2D glyph2D, PDFont font, int code, Vector displacement,
        AffineTransform at) throws IOException
    {
        PDGraphicsState state = getGraphicsState();
        RenderingMode renderingMode = state.getTextState().getRenderingMode();

//        test++;
//        if (test!=2) {
//            return;
//        }
//        Log.w("ceshi",test+"__Glyph2D::"+glyph2D.getClass().getSimpleName());
        Path path = glyph2D.getPathForCharacterCode(code);
        if (path != null)
        {
            // Stretch non-embedded glyph if it does not match the height/width contained in the PDF.
            // Vertical fonts have zero X displacement, so the following code scales to 0 if we don't skip it.
            // TODO: How should vertical fonts be handled?
            if (!font.isEmbedded() && !font.isVertical() && !font.isStandard14() && font.hasExplicitWidth(code))
            {
                float fontWidth = font.getWidthFromFont(code);
                if (fontWidth > 0 && // ignore spaces
                    Math.abs(fontWidth - displacement.getX() * 1000) > 0.0001)
                {
                    float pdfWidth = displacement.getX() * 1000;
                    at.scale(pdfWidth / fontWidth, 1);
                }
            }

            // render glyph
//            Shape glyph = at.createTransformedShape(path);
            path.transform(at.toMatrix());
            RectF rectF = new RectF();
            path.computeBounds(rectF,true);

            if (isContentRendered())
            {
                if (renderingMode.isFill())
                {
//                    Log.w("ceshi","字体路径111："+rectF);
                    paint.setColor(getNonStrokingColor());
                    setClip2();
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawPath(path, paint);
                }
                if (renderingMode.isStroke())
                {
//                    Log.w("ceshi","字体路径222："+rectF);
                    paint.setColor(getStrokingColor());
                    setStroke();
                    setClip2();
                    paint.setStyle(Paint.Style.STROKE);
                    canvas.drawPath(path, paint);
                }
            }

            if (renderingMode.isClip())
            {
                Log.w("ceshi","字体路径333：");
//                textClippings.add(glyph); TODO: PdfBox-Android
            }
        }
    }

    /**
     * Provide a Glyph2D for the given font.
     *
     * @param font the font
     * @return the implementation of the Glyph2D interface for the given font
     * @throws IOException if something went wrong
     */
    private Glyph2D createGlyph2D(PDFont font) throws IOException
    {
        Glyph2D glyph2D = fontGlyph2D.get(font);
        // Is there already a Glyph2D for the given font?
//        Log.w("ceshi","font："+font.getClass().getSimpleName());
        if (glyph2D != null)
        {
            return glyph2D;
        }

        if (font instanceof PDTrueTypeFont)
        {
            PDTrueTypeFont ttfFont = (PDTrueTypeFont)font;
            glyph2D = new TTFGlyph2D(ttfFont);  // TTF is never null
        }
        else if (font instanceof PDType1Font)
        {
            PDType1Font pdType1Font = (PDType1Font)font;
            glyph2D = new Type1Glyph2D(pdType1Font); // T1 is never null
        }
        else if (font instanceof PDType1CFont)
        {
            PDType1CFont type1CFont = (PDType1CFont)font;
            glyph2D = new Type1Glyph2D(type1CFont);
        }
        else if (font instanceof PDType0Font)
        {
            PDType0Font type0Font = (PDType0Font) font;
//            Log.w("ceshi","PDType0Font：：："+type0Font.getDescendantFont().getClass().getSimpleName());
            if (type0Font.getDescendantFont() instanceof PDCIDFontType2)
            {
                glyph2D = new TTFGlyph2D(type0Font); // TTF is never null
            }
            else if (type0Font.getDescendantFont() instanceof PDCIDFontType0)
            {
                // a Type0 CIDFont contains CFF font
                PDCIDFontType0 cidType0Font = (PDCIDFontType0)type0Font.getDescendantFont();
                glyph2D = new CIDType0Glyph2D(cidType0Font);
                // todo: could be null (need incorporate fallback)
            }
        }
        else
        {
            throw new IllegalStateException("Bad font type: " + font.getClass().getSimpleName());
        }

        // cache the Glyph2D instance
        if (glyph2D != null)
        {
            fontGlyph2D.put(font, glyph2D);
        }

        if (glyph2D == null)
        {
            // todo: make sure this never happens
            throw new UnsupportedOperationException("No font for " + font.getName());
        }

        return glyph2D;
    }

    @Override
    public void appendRectangle(PointF p0, PointF p1, PointF p2, PointF p3)
    {
        // to ensure that the path is created in the right direction, we have to create
        // it by combining single lines instead of creating a simple rectangle
        linePath.moveTo((float) p0.x, (float) p0.y);
        linePath.lineTo((float) p1.x, (float) p1.y);
        linePath.lineTo((float) p2.x, (float) p2.y);
        linePath.lineTo((float) p3.x, (float) p3.y);

        // close the subpath instead of adding the last line so that a possible set line
        // cap style isn't taken into account at the "beginning" of the rectangle
        linePath.close();
    }

//    private Paint applySoftMaskToPaint(Paint parentPaint, PDSoftMask softMask) throws IOException TODO: Pdfbox-Android

//    private void adjustRectangle(RectF r) TODO: PdfBox-Android

//    private Bitmap adjustImage(Bitmap gray) throws IOException TODO: PdfBox-Android

//    private Paint getStrokingPaint() throws IOException TODO: PdfBox-Android

    private int getStrokingColor() throws IOException
    {
        return getColor(getGraphicsState().getStrokingColor());
    }

//    private Paint getNonStrokingPaint() throws IOException TODO: PdfBox-Android

    private int getNonStrokingColor() throws IOException
    {
        return getColor(getGraphicsState().getNonStrokingColor());
    }

    // set stroke based on the current CTM and the current stroke
    private void setStroke()
    {
        PDGraphicsState state = getGraphicsState();

        // apply the CTM
        float lineWidth = transformWidth(state.getLineWidth());

        // minimum line width as used by Adobe Reader
        if (lineWidth < 0.25)
        {
            lineWidth = 0.25f;
        }

        PDLineDashPattern dashPattern = state.getLineDashPattern();
        float phaseStart = dashPattern.getPhase();
        float[] dashArray = getDashArray(dashPattern);
        phaseStart = transformWidth(phaseStart);

        // empty dash array is illegal
        // avoid also infinite and NaN values (PDFBOX-3360)
        if (dashArray.length == 0 || Float.isInfinite(phaseStart) || Float.isNaN(phaseStart))
        {
            dashArray = null;
        }
        else
        {
            for (int i = 0; i < dashArray.length; ++i)
            {
                if (Float.isInfinite(dashArray[i]) || Float.isNaN(dashArray[i]))
                {
                    dashArray = null;
                    break;
                }
            }
        }
        paint.setStrokeWidth(lineWidth);
        paint.setStrokeCap(state.getLineCap());
        paint.setStrokeJoin(state.getLineJoin());
        if (dashArray != null)
        {
            paint.setPathEffect(new DashPathEffect(dashArray, phaseStart));
        }
    }

    private float[] getDashArray(PDLineDashPattern dashPattern)
    {
        float[] dashArray = dashPattern.getDashArray();
        if (JAVA_VERSION < 10)
        {
            float scalingFactorX = new Matrix(xform).getScalingFactorX();
            for (int i = 0; i < dashArray.length; ++i)
            {
                // apply the CTM
                float w = transformWidth(dashArray[i]);
                // minimum line dash width avoids JVM crash,
                // see PDFBOX-2373, PDFBOX-2929, PDFBOX-3204, PDFBOX-3813
                // also avoid 0 in array like "[ 0 1000 ] 0 d", see PDFBOX-3724
                if (scalingFactorX < 0.5f)
                {
                    // PDFBOX-4492
                    dashArray[i] = Math.max(w, 0.2f);
                }
                else
                {
                    dashArray[i] = Math.max(w, 0.062f);
                }
            }
        }
        else
        {
            for (int i = 0; i < dashArray.length; ++i)
            {
                // apply the CTM
                dashArray[i] = transformWidth(dashArray[i]);
            }
        }
        return dashArray;
    }

    @Override
    public void strokePath() throws IOException
    {
//        graphics.setComposite(getGraphicsState().getStrokingJavaComposite());
        setStroke();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(getStrokingColor());
        canvas.save();
        setClip2();
        //TODO bbox of shading pattern should be used here? (see fillPath)
        if (isContentRendered())
        {
            canvas.drawPath(linePath, paint);
            canvas.restore();
        }
        linePath.reset();
    }

    int test = 0;

    @Override
    public void fillPath(Path.FillType windingRule) throws IOException
    {
        test++;
//        Log.w("ceshi","fillPath--------------"+test);
//        if (test != 999) {
//            return;
//        }
//        test++;

        paint.setAlpha((int)(getGraphicsState().getNonStrokeAlphaConstant()*255));
        canvas.save();

        linePath.setFillType(windingRule);

        // disable anti-aliasing for rectangular paths, this is a workaround to avoid small stripes
        // which occur when solid fills are used to simulate piecewise gradients, see PDFBOX-2302
        // note that we ignore paths with a width/height under 1 as these are fills used as strokes,
        // see PDFBOX-1658 for an example
        RectF bounds = new RectF();
        linePath.computeBounds(bounds, true);
        boolean noAntiAlias = isRectangular(linePath) && bounds.width() > 1 &&
            bounds.height() > 1;
        if (noAntiAlias)
        {
            paint.setAntiAlias(false);
        }

        if (isContentRendered())
        {
            paint.setStyle(Paint.Style.FILL);
            if (getGraphicsState().getNonStrokingColor().getColorSpace() instanceof PDPattern) {
                PDColorSpace colorSpace = getGraphicsState().getNonStrokingColor().getColorSpace();
                PDAbstractPattern pattern = ((PDPattern)colorSpace).getPattern(getGraphicsState().getNonStrokingColor());
                PDShadingPattern shadingPattern = (PDShadingPattern)pattern;
                PDShading shading = shadingPattern.getShading();
                //轴向
                if (shading instanceof PDShadingType2) {
                    getGraphicsState().intersectClippingPath(linePath,scale);
                    canvas.scale(1/scale,1/scale);
                    Rect rect = new Rect((int)(bounds.left*scale),(int)(bounds.top*scale),(int)(bounds.right*scale),(int)(bounds.bottom*scale));
                    Rect rect2 = new Rect((int)(bounds.left*scale),(int)(canvas.getHeight()-(bounds.bottom*scale)),(int)(bounds.right*scale),(int)(canvas.getHeight()-(bounds.top*scale)));

                    setClip();
                    AxialShadingContext axialShadingContext = new AxialShadingContext((PDShadingType2) shading,rect2);
                    for (int y=rect.bottom;y>rect.top;y--) {
                        int[] data = axialShadingContext.getRaster(rect.left,canvas.getHeight()-y,rect.right-rect.left,1);
                        for (int i=0;i<data.length;i++) {
                            paint.setColor(data[i]|0xff000000);
                            canvas.drawPoint(rect.left+i,y,paint);
                        }
                    }
                }
            } else {
                setClip2();
                paint.setColor(getNonStrokingColor());
                canvas.drawPath(linePath, paint);
            }
            canvas.restore();
        }

        linePath.reset();

        if (noAntiAlias)
        {
            // JDK 1.7 has a bug where rendering hints are reset by the above call to
            // the setRenderingHint method, so we re-set all hints, see PDFBOX-2302
            setRenderingHints();
        }
    }

//    private void intersectShadingBBox(PDColor color, Area area) throws IOException TODO: PdfBox-Android

    /**
     * Returns true if the given path is rectangular.
     */
    private boolean isRectangular(Path path)
    {
        RectF rect = new RectF();
        return path.isRect(rect);
    }

    /**
     * Fills and then strokes the path.
     *
     * @param windingRule The winding rule this path will use.
     * @throws IOException If there is an IO error while filling the path.
     */
    @Override
    public void fillAndStrokePath(Path.FillType windingRule) throws IOException
    {
        // TODO can we avoid cloning the path?
        Path path = new Path(linePath);
        fillPath(windingRule);
        linePath = path;
        strokePath();
    }

    @Override
    public void clip(Path.FillType windingRule)
    {
        // the clipping path will not be updated until the succeeding painting operator is called
        clipWindingRule = windingRule;
    }

    @Override
    public void moveTo(float x, float y)
    {
        currentPoint.set(x, y);
        linePath.moveTo(x, y);
    }

    @Override
    public void lineTo(float x, float y)
    {
        currentPoint.set(x, y);
        linePath.lineTo(x, y);
    }

    @Override
    public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3)
    {
        currentPoint.set(x3, y3);
        linePath.cubicTo(x1, y1, x2, y2, x3, y3); // TODO: PdfBox-Android check if this should be relative
    }

    @Override
    public PointF getCurrentPoint()
    {
        return currentPoint;
    }

    @Override
    public void closePath()
    {
        linePath.close();
    }


    @Override
    public void endPath()
    {
        if (clipWindingRule != null)
        {
            linePath.setFillType(clipWindingRule);
            //TODO: zyj 生成了有损路径
//            if (test==25) {
//                Log.w("ceshi","生成路径,scale:"+scale);
                getGraphicsState().intersectClippingPath(linePath,scale);
//            } else
//                getGraphicsState().intersectClippingPath(linePath);


            // PDFBOX-3836: lastClip needs to be reset, because after intersection it is still the same
            // object, thus setClip() would believe that it is cached.
            lastClip = null;

            clipWindingRule = null;
        } //TODO: PdfBox-Android causes rendering issues
        linePath.reset();
    }

    @Override
    public void drawImage(PDImage pdImage) throws IOException
    {
        Matrix ctm = getGraphicsState().getCurrentTransformationMatrix();
        AffineTransform at = ctm.createAffineTransform();

        if (!pdImage.getInterpolate())
        {
            boolean isScaledUp = pdImage.getWidth() < Math.round(at.getScaleX()) ||
                pdImage.getHeight() < Math.round(at.getScaleY());

            // if the image is scaled down, we use smooth interpolation, eg PDFBOX-2364
            // only when scaled up do we use nearest neighbour, eg PDFBOX-2302 / mori-cvpr01.pdf
            // stencils are excluded from this rule (see survey.pdf)
            if (isScaledUp || pdImage.isStencil())
            {
//                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
//                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            }
        }

        if (pdImage.isStencil())
        {
//            if (getGraphicsState().getNonStrokingColor().getColorSpace() instanceof PDPattern) TODO: PdfBox-Android
//            else
//            TODO: PdfBox-Android draw stenciled Bitmap
        }
        else
        {
            if (subsamplingAllowed)
            {
                int subsampling = getSubsampling(pdImage, at);
                // draw the subsampled image
                drawBitmap(pdImage.getImage(null, subsampling), at);
            }
            else
            {
                // subsampling not allowed, draw the image
                drawBitmap(pdImage.getImage(), at);
            }
        }

        if (!pdImage.getInterpolate())
        {
            // JDK 1.7 has a bug where rendering hints are reset by the above call to
            // the setRenderingHint method, so we re-set all hints, see PDFBOX-2302
            setRenderingHints();
        }
    }

    /**
     * Calculated the subsampling frequency for a given PDImage based on the current transformation
     * and its calculated transform
     *
     * @param pdImage PDImage to be drawn
     * @param at Transform that will be applied to the image when drawing
     * @return The rounded-down ratio of image pixels to drawn pixels. Returned value will always be
     * >=1.
     */
    private int getSubsampling(PDImage pdImage, AffineTransform at)
    {
        // calculate subsampling according to the resulting image size
        double scale = Math.abs(at.getDeterminant() * xform.getDeterminant());

        int subsampling = (int) Math.floor(Math.sqrt(pdImage.getWidth() * pdImage.getHeight() / scale));
        if (subsampling > 8)
        {
            subsampling = 8;
        }
        if (subsampling < 1)
        {
            subsampling = 1;
        }
        if (subsampling > pdImage.getWidth() || subsampling > pdImage.getHeight())
        {
            // For very small images it is possible that the subsampling would imply 0 size.
            // To avoid problems, the subsampling is set to no less than the smallest dimension.
            subsampling = Math.min(pdImage.getWidth(), pdImage.getHeight());
        }
        return subsampling;
    }

    private void drawBitmap(Bitmap image, AffineTransform at) throws IOException
    {
        canvas.save();
//        Log.w("ceshi","绘制图片==="+(int)(getGraphicsState().getNonStrokeAlphaConstant()*255));
        paint.setAlpha((int)(getGraphicsState().getNonStrokeAlphaConstant()*255));
        paint.setColor(getNonStrokingColor());
//        graphics.setComposite(getGraphicsState().getNonStrokingJavaComposite());
        setClip2();
        AffineTransform imageTransform = new AffineTransform(at);
        PDSoftMask softMask = getGraphicsState().getSoftMask();
        if( softMask != null )
        {
            imageTransform.scale(1, -1);
            imageTransform.translate(0, -1);
//            Paint awtPaint = new TexturePaint(image,
//                new Rectangle2D.Double(imageTransform.getTranslateX(), imageTransform.getTranslateY(),
//                    imageTransform.getScaleX(), imageTransform.getScaleY()));
//            awtPaint = applySoftMaskToPaint(awtPaint, softMask);
//            graphics.setPaint(awtPaint);
            RectF unitRect = new RectF(0, 0, 1, 1);
            if (isContentRendered())
            {
//            graphics.fill(at.createTransformedShape(unitRect));
            }
        }
        else
        {
            COSBase transfer = getGraphicsState().getTransfer();
            if (transfer instanceof COSArray || transfer instanceof COSDictionary)
            {
                image = applyTransferFunction(image, transfer);
            }

            int width = image.getWidth();
            int height = image.getHeight();
            imageTransform.scale(1.0 / width, -1.0 / height);
            imageTransform.translate(0, -height);
            if (isContentRendered())
            {
                canvas.drawBitmap(image, imageTransform.toMatrix(), paint);
            }
        }
        canvas.restore();
    }

    private Bitmap applyTransferFunction(Bitmap image, COSBase transfer) throws IOException
    {
        Bitmap bim = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
        // TODO: Pdfbox-Android - does this always need to be ARGB_8888?

        // prepare transfer functions (either one per color or one for all) 
        // and maps (actually arrays[256] to be faster) to avoid calculating values several times
        Integer rMap[], gMap[], bMap[];
        PDFunction rf, gf, bf;
        if (transfer instanceof COSArray)
        {
            COSArray ar = (COSArray) transfer;
            rf = PDFunction.create(ar.getObject(0));
            gf = PDFunction.create(ar.getObject(1));
            bf = PDFunction.create(ar.getObject(2));
            rMap = new Integer[256];
            gMap = new Integer[256];
            bMap = new Integer[256];
        }
        else
        {
            rf = PDFunction.create(transfer);
            gf = rf;
            bf = rf;
            rMap = new Integer[256];
            gMap = rMap;
            bMap = rMap;
        }

        // apply the transfer function to each color, but keep alpha
        float[] input = new float[1];
        for (int x = 0; x < image.getWidth(); ++x)
        {
            for (int y = 0; y < image.getHeight(); ++y)
            {
                int rgb = image.getPixel(x, y);
                int ri = (rgb >> 16) & 0xFF;
                int gi = (rgb >> 8) & 0xFF;
                int bi = rgb & 0xFF;
                int ro, go, bo;
                if (rMap[ri] != null)
                {
                    ro = rMap[ri];
                }
                else
                {
                    input[0] = (ri & 0xFF) / 255f;
                    ro = (int) (rf.eval(input)[0] * 255);
                    rMap[ri] = ro;
                }
                if (gMap[gi] != null)
                {
                    go = gMap[gi];
                }
                else
                {
                    input[0] = (gi & 0xFF) / 255f;
                    go = (int) (gf.eval(input)[0] * 255);
                    gMap[gi] = go;
                }
                if (bMap[bi] != null)
                {
                    bo = bMap[bi];
                }
                else
                {
                    input[0] = (bi & 0xFF) / 255f;
                    bo = (int) (bf.eval(input)[0] * 255);
                    bMap[bi] = bo;
                }
                bim.setPixel(x, y, (rgb & 0xFF000000) | (ro << 16) | (go << 8) | bo);
            }
        }
        return bim;
    }

    @Override
    public void shadingFill(COSName shadingName) throws IOException
    {
        PDShading shading = getResources().getShading(shadingName);
        if (shading == null)
        {
            Log.e("PdfBox-Android", "shading " + shadingName + " does not exist in resources dictionary");
            return;
        }
        Matrix ctm = getGraphicsState().getCurrentTransformationMatrix();
//        Paint paint = shading.toPaint(ctm);
//        paint = applySoftMaskToPaint(paint, getGraphicsState().getSoftMask());

//        graphics.setComposite(getGraphicsState().getNonStrokingJavaComposite());
//        graphics.setPaint(paint);
//        graphics.setClip(null);
//        lastClip = null;

        // get the transformed BBox and intersect with current clipping path
        // need to do it here and not in shading getRaster() because it may have been rotated
        PDRectangle bbox = shading.getBBox();
        if (bbox != null)
        {
//            Area bboxArea = new Area(bbox.transform(ctm));
//            bboxArea.intersect(getGraphicsState().getCurrentClippingPath());
//            graphics.fill(bboxArea);
        }
        else
        {
//            graphics.fill(getGraphicsState().getCurrentClippingPath());
        }
    }

    @Override
    public void showAnnotation(PDAnnotation annotation) throws IOException
    {
//        Log.w("ceshi","getAnnotationName---"+annotation.getAnnotationName());
//        Log.w("ceshi","getSubtype---"+annotation.getSubtype());
        lastClip = null;
        // Device checks shouldn't be needed
        if (annotation.isNoView())
        {
            return;
        }
        if (annotation.isHidden())
        {
            return;
        }
        if (annotation.isInvisible() && annotation instanceof PDAnnotationUnknown)
        {
            // "If set, do not display the annotation if it does not belong to one
            // of the standard annotation types and no annotation handler is available."
            return;
        }
        //TODO support NoZoom, example can be found in p5 of PDFBOX-2348

        if (isHiddenOCG(annotation.getOptionalContent()))
        {
            return;
        }

        super.showAnnotation(annotation);

        if (annotation.getAppearance() == null)
        {
            if (annotation instanceof PDAnnotationLink)
            {
                drawAnnotationLinkBorder((PDAnnotationLink) annotation);
            }

            if (annotation instanceof PDAnnotationMarkup && annotation.getSubtype().equals(PDAnnotationMarkup.SUB_TYPE_INK))
            {
                drawAnnotationInk((PDAnnotationMarkup) annotation);
            }
        }
    }

    private static class AnnotationBorder
    {
        private float[] dashArray = null;
        private boolean underline = false;
        private float width = 0;
        private PDColor color;
    }

    // return border info. BorderStyle must be provided as parameter because
    // method is not available in the base class
    private AnnotationBorder getAnnotationBorder(PDAnnotation annotation,
        PDBorderStyleDictionary borderStyle)
    {
        AnnotationBorder ab = new AnnotationBorder();
        COSArray border = annotation.getBorder();
        if (borderStyle == null)
        {
            if (border.getObject(2) instanceof COSNumber)
            {
                ab.width = ((COSNumber) border.getObject(2)).floatValue();
            }
            if (border.size() > 3)
            {
                COSBase base3 = border.getObject(3);
                if (base3 instanceof COSArray)
                {
                    ab.dashArray = ((COSArray) base3).toFloatArray();
                }
            }
        }
        else
        {
            ab.width = borderStyle.getWidth();
            if (borderStyle.getStyle().equals(PDBorderStyleDictionary.STYLE_DASHED))
            {
                ab.dashArray = borderStyle.getDashStyle().getDashArray();
            }
            if (borderStyle.getStyle().equals(PDBorderStyleDictionary.STYLE_UNDERLINE))
            {
                ab.underline = true;
            }
        }
        ab.color = annotation.getColor();
        if (ab.color == null)
        {
            // spec is unclear, but black seems to be the right thing to do
            ab.color = new PDColor(new float[] { 0 }, PDDeviceGray.INSTANCE);
        }
        if (ab.dashArray != null)
        {
            boolean allZero = true;
            for (float f : ab.dashArray)
            {
                if (f != 0)
                {
                    allZero = false;
                    break;
                }
            }
            if (allZero)
            {
                ab.dashArray = null;
            }
        }
        return ab;
    }

    private void drawAnnotationLinkBorder(PDAnnotationLink link) throws IOException
    {
        AnnotationBorder ab = getAnnotationBorder(link, link.getBorderStyle());
        if (ab.width == 0 || ab.color.getComponents().length == 0)
        {
            return;
        }
        PDRectangle rectangle = link.getRectangle();
        Paint strokePaint = new Paint(paint);
        strokePaint.setColor(getColor(ab.color));
        setStroke(strokePaint, ab.width, Paint.Cap.BUTT, Paint.Join.MITER, 10, ab.dashArray, 0);
//        canvas.restore();
        if (ab.underline)
        {
            canvas.drawLine(rectangle.getLowerLeftX(), rectangle.getLowerLeftY(),
                rectangle.getLowerLeftX() + rectangle.getWidth(), rectangle.getLowerLeftY(),
                strokePaint);
        }
        else
        {
            canvas.drawRect(rectangle.getLowerLeftX(), rectangle.getLowerLeftY(),
                rectangle.getWidth(), rectangle.getHeight(), strokePaint);
        }
    }

    private void drawAnnotationInk(PDAnnotationMarkup inkAnnotation) throws IOException
    {
        if (!inkAnnotation.getCOSObject().containsKey(COSName.INKLIST))
        {
            return;
        }
        //TODO there should be an InkAnnotation class with a getInkList method
        COSBase base = inkAnnotation.getCOSObject().getDictionaryObject(COSName.INKLIST);
        if (!(base instanceof COSArray))
        {
            return;
        }
        // PDF spec does not mention /Border for ink annotations, but it is used if /BS is not available
        AnnotationBorder ab = getAnnotationBorder(inkAnnotation, inkAnnotation.getBorderStyle());
        if (ab.width == 0 || ab.color.getComponents().length == 0)
        {
            return;
        }
        Paint strokePaint = new Paint(paint);
        strokePaint.setColor(getColor(ab.color));
        setStroke(strokePaint, ab.width, Paint.Cap.BUTT, Paint.Join.MITER, 10, ab.dashArray, 0);
        canvas.restore();
        COSArray pathsArray = (COSArray) base;
        for (COSBase baseElement : pathsArray.toList())
        {
            if (!(baseElement instanceof COSArray))
            {
                continue;
            }
            COSArray pathArray = (COSArray) baseElement;
            int nPoints = pathArray.size() / 2;

            // "When drawn, the points shall be connected by straight lines or curves 
            // in an implementation-dependent way" - we do lines.
            Path path = new Path();
            for (int i = 0; i < nPoints; ++i)
            {
                COSBase bx = pathArray.getObject(i * 2);
                COSBase by = pathArray.getObject(i * 2 + 1);
                if (bx instanceof COSNumber && by instanceof COSNumber)
                {
                    float x = ((COSNumber) bx).floatValue();
                    float y = ((COSNumber) by).floatValue();
                    if (i == 0)
                    {
                        path.moveTo(x, y);
                    }
                    else
                    {
                        path.lineTo(x, y);
                    }
                }
            }
            canvas.drawPath(path, strokePaint);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showForm(PDFormXObject form) throws IOException
    {
        if (isContentRendered())
        {
            super.showForm(form);
        }
    }

    public void setStroke(Paint p, float width, Paint.Cap cap, Paint.Join join, float miterLimit, float[] dash, float dash_phase)
    {
        p.setStrokeWidth(width);
        p.setStrokeCap(cap);
        p.setStrokeJoin(join);
        p.setStrokeMiter(miterLimit);
        if(dash != null)
        {
            p.setPathEffect(new DashPathEffect(dash, dash_phase));
        }
    }

    @Override
    public void showTransparencyGroup(PDTransparencyGroup form) throws IOException
    {
        if (!isContentRendered())
        {
            return;
        }
        TransparencyGroup group =
            new TransparencyGroup(form, false, getGraphicsState().getCurrentTransformationMatrix(), null);
//        Bitmap image = group.getImage();
//        if (image == null)
//        {
            // image is empty, don't bother
//            return;
//        }

//        graphics.setComposite(getGraphicsState().getNonStrokingJavaComposite());
        setClip2();

        // both the DPI xform and the CTM were already applied to the group, so all we do
        // here is draw it directly onto the Graphics2D device at the appropriate position
//        PDRectangle bbox = group.getBBox();
//        AffineTransform prev = graphics.getTransform();

        Matrix m = new Matrix(xform);
        float xScale = Math.abs(m.getScalingFactorX());
        float yScale = Math.abs(m.getScalingFactorY());

        AffineTransform transform = new AffineTransform(xform);
        transform.scale(1.0 / xScale, 1.0 / yScale);
//        graphics.setTransform(transform);

        // adjust bbox (x,y) position at the initial scale + cropbox
//        float x = bbox.getLowerLeftX() - pageSize.getLowerLeftX();
//        float y = pageSize.getUpperRightY() - bbox.getUpperRightY();

        if (flipTG)
        {
//            graphics.translate(0, image.getHeight());
//            graphics.scale(1, -1);
        }
        else
        {
//            graphics.translate(x * xScale, y * yScale);
        }

        PDSoftMask softMask = getGraphicsState().getSoftMask();
        if (softMask != null)
        {
//            Paint awtPaint = new TexturePaint(image,
//                new Rectangle2D.Float(0, 0, image.getWidth(), image.getHeight()));
//            awtPaint = applySoftMaskToPaint(awtPaint, softMask);
//            graphics.setPaint(awtPaint);
            if (isContentRendered())
            {
//                graphics.fill(
//                    new Rectangle2D.Float(0, 0, bbox.getWidth() * xScale, bbox.getHeight() * yScale));
            }
        }
        else
        {
            if (isContentRendered())
            {
//                graphics.drawImage(image, null, null);
            }

        }

//        graphics.setTransform(prev);
    }

    /**
     * Transparency group.
     **/
    private final class TransparencyGroup
    {
//        private final Bitmap image;
//        private final PDRectangle bbox;

//        private final int minX;
//        private final int minY;
//        private final int maxX;
//        private final int maxY;
//        private final int width;
//        private final int height;
        private final float scaleX;
        private final float scaleY;

        /**
         * Creates a buffered image for a transparency group result.
         *
         * @param form the transparency group of the form or soft mask.
         * @param isSoftMask true if this is a soft mask.
         * @param ctm the relevant current transformation matrix. For soft masks, this is the CTM at
         * the time the soft mask is set (not at the time the soft mask is used for fill/stroke!),
         * for forms, this is the CTM at the time the form is invoked.
         * @param backdropColor the color according to the /bc entry to be used for luminosity soft
         * masks.
         * @throws IOException
         */
        private TransparencyGroup(PDTransparencyGroup form, boolean isSoftMask, Matrix ctm,
            PDColor backdropColor) throws IOException
        {
//            Graphics2D g2dOriginal = graphics;
//            Area lastClipOriginal = lastClip;

            // get the CTM x Form Matrix transform
            Matrix transform = Matrix.concatenate(ctm, form.getMatrix());

            // transform the bbox
            Path transformedBox = form.getBBox().transform(transform);

            // clip the bbox to prevent giant bboxes from consuming all memory
//            Area clip = (Area)getGraphicsState().getCurrentClippingPath().clone();
//            clip.intersect(new Area(transformedBox));
//            Rectangle2D clipRect = clip.getBounds2D();
            Matrix m = new Matrix(xform);
            scaleX = Math.abs(m.getScalingFactorX());
            scaleY = Math.abs(m.getScalingFactorY());
//            if (clipRect.isEmpty())
//            {
//                image = null;
//                bbox = null;
//                minX = 0;
//                minY = 0;
//                maxX = 0;
//                maxY = 0;
//                width = 0;
//                height = 0;
//                return;
//            }
//            this.bbox = new PDRectangle((float)clipRect.getX(), (float)clipRect.getY(),
//                (float)clipRect.getWidth(), (float)clipRect.getHeight());

            // apply the underlying Graphics2D device's DPI transform
            AffineTransform dpiTransform = AffineTransform.getScaleInstance(scaleX, scaleY);
//            Rectangle2D bounds = dpiTransform.createTransformedShape(clip.getBounds2D()).getBounds2D();

//            minX = (int) Math.floor(bounds.getMinX());
//            minY = (int) Math.floor(bounds.getMinY());
//            maxX = (int) Math.floor(bounds.getMaxX()) + 1;
//            maxY = (int) Math.floor(bounds.getMaxY()) + 1;

//            width = maxX - minX;
//            height = maxY - minY;

            // FIXME - color space
            if (isGray(form.getGroup().getColorSpace()))
            {
//                image = create2ByteGrayAlphaImage(width, height);
            }
            else
            {
//                image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            }
//            Graphics2D g = image.createGraphics();

            boolean needsBackdrop = !isSoftMask && !form.getGroup().isIsolated() &&
                hasBlendMode(form, new HashSet<COSBase>());
            Bitmap backdropImage = null;
            // Position of this group in parent group's coordinates
            int backdropX = 0;
            int backdropY = 0;
            if (needsBackdrop)
            {
                if (transparencyGroupStack.isEmpty())
                {
                    // Use the current page as the parent group.
                    backdropImage = renderer.getPageImage();
                    needsBackdrop = backdropImage != null;
//                    backdropX = minX;
//                    backdropY = (backdropImage != null) ? (backdropImage.getHeight() - maxY) : 0;
                }
                else
                {
                    TransparencyGroup parentGroup = transparencyGroupStack.peek();
//                    backdropImage = parentGroup.image;
//                    backdropX = minX - parentGroup.minX;
//                    backdropY = parentGroup.maxY - maxY;
                }
            }

//            Graphics2D g = image.createGraphics();
            if (needsBackdrop)
            {
                // backdropImage must be included in group image but not in group alpha.
//                g.drawImage(backdropImage, 0, 0, width, height,
//                    backdropX, backdropY, backdropX + width, backdropY + height, null);
//                g = new GroupGraphics(image, g);
            }
            if (isSoftMask && backdropColor != null)
            {
                // "If the subtype is Luminosity, the transparency group XObject G shall be
                // composited with a fully opaque backdrop whose colour is everywhere defined
                // by the soft-mask dictionary's BC entry."
//                g.setBackground(new Color(backdropColor.toRGB()));
//                g.clearRect(0, 0, width, height);
            }

            // flip y-axis
//            g.translate(0, image.getHeight());
//            g.scale(1, -1);

            boolean oldFlipTG = flipTG;
            flipTG = false;

            // apply device transform (DPI)
            // the initial translation is ignored, because we're not writing into the initial graphics device
//            g.transform(dpiTransform);

            AffineTransform xformOriginal = xform;
            xform = AffineTransform.getScaleInstance(scaleX, scaleY);
            PDRectangle pageSizeOriginal = pageSize;
//            pageSize = new PDRectangle(minX / scaleX,
//                minY / scaleY,
//                (float) bounds.getWidth() / scaleX,
//                (float) bounds.getHeight() / scaleY);
            Path.FillType clipWindingRuleOriginal = clipWindingRule;
            clipWindingRule = null;
            Path linePathOriginal = linePath;
            linePath = new Path();

            // adjust the origin
//            g.translate(-clipRect.getX(), -clipRect.getY());

//            graphics = g;
            setRenderingHints();
            try
            {
                if (isSoftMask)
                {
                    processSoftMask(form);
                }
                else
                {
                    transparencyGroupStack.push(this);
                    processTransparencyGroup(form);
                    if (!transparencyGroupStack.isEmpty())
                    {
                        transparencyGroupStack.pop();
                    }
                }
            }
            finally
            {
                flipTG = oldFlipTG;
//                lastClip = lastClipOriginal;
//                graphics.dispose();
//                graphics = g2dOriginal;
                clipWindingRule = clipWindingRuleOriginal;
                linePath = linePathOriginal;
                pageSize = pageSizeOriginal;
                xform = xformOriginal;

                if (needsBackdrop)
                {
//                    ((GroupGraphics) g).removeBackdrop(backdropImage, backdropX, backdropY);
                }
            }
        }

        // http://stackoverflow.com/a/21181943/535646
//        private BufferedImage create2ByteGrayAlphaImage(int width, int height) TODO: PdfBox-Android

        private boolean isGray(PDColorSpace colorSpace)
        {
            if (colorSpace instanceof PDDeviceGray)
            {
                return true;
            }
//            if (colorSpace instanceof PDICCBased)
//            {
//                try
//                {
//                    return ((PDICCBased) colorSpace).getAlternateColorSpace() instanceof PDDeviceGray;
//                }
//                catch (IOException ex)
//                {
//                    return false;
//                }
//            } TODO: PdfBox-Android
            return false;
        }

//        public Bitmap getImage()
//        {
//            return image;
//        }

//        public PDRectangle getBBox()
//        {
//            return bbox;
//        }

//        public RectF getBounds()
//        {
//            PointF size = new PointF(pageSize.getWidth(), pageSize.getHeight());
//            // apply the underlying Graphics2D device's DPI transform and y-axis flip
//            AffineTransform dpiTransform = AffineTransform.getScaleInstance(scaleX, scaleY);
//            size = dpiTransform.transform(size, size);
//            // Flip y
//            return new RectF(minX - pageSize.getLowerLeftX() * scaleX,
//                size.y - minY - height + pageSize.getLowerLeftY() * scaleY,
//                width, height);
//        }
    }

    private boolean hasBlendMode(PDTransparencyGroup group, Set<COSBase> groupsDone)
    {
        if (groupsDone.contains(group.getCOSObject()))
        {
            // The group was already processed. Avoid endless recursion.
            return false;
        }
        groupsDone.add(group.getCOSObject());

        PDResources resources = group.getResources();
        if (resources == null)
        {
            return false;
        }
        for (COSName name : resources.getExtGStateNames())
        {
            PDExtendedGraphicsState extGState = resources.getExtGState(name);
            if (extGState == null)
            {
                continue;
            }
            BlendMode blendMode = extGState.getBlendMode();
            if (blendMode != BlendMode.NORMAL)
            {
                return true;
            }
        }

        // Recursively process nested transparency groups
        for (COSName name : resources.getXObjectNames())
        {
            PDXObject xObject;
            try
            {
                xObject = resources.getXObject(name);
            }
            catch (IOException ex)
            {
                continue;
            }
            if (xObject instanceof PDTransparencyGroup &&
                hasBlendMode((PDTransparencyGroup)xObject, groupsDone))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beginMarkedContentSequence(COSName tag, COSDictionary properties)
    {
        if (nestedHiddenOCGCount > 0)
        {
            nestedHiddenOCGCount++;
            return;
        }
        if (tag == null || getPage().getResources() == null)
        {
            return;
        }
        if (isHiddenOCG(getPage().getResources().getProperties(tag)))
        {
            nestedHiddenOCGCount = 1;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endMarkedContentSequence()
    {
        if (nestedHiddenOCGCount > 0)
        {
            nestedHiddenOCGCount--;
        }
    }

    private boolean isContentRendered()
    {
        return nestedHiddenOCGCount <= 0;
    }

    private boolean isHiddenOCG(PDPropertyList propertyList)
    {
        if (propertyList instanceof PDOptionalContentGroup)
        {
            PDOptionalContentGroup group = (PDOptionalContentGroup) propertyList;
            RenderState printState = group.getRenderState(destination);
            if (printState == null)
            {
                if (!getRenderer().isGroupEnabled(group))
                {
                    return true;
                }
            }
            else if (RenderState.OFF.equals(printState))
            {
                return true;
            }
        }
        return false;
    }

    private static int getJavaVersion()
    {
        // strategy from lucene-solr/lucene/core/src/java/org/apache/lucene/util/Constants.java
        String version = System.getProperty("java.specification.version");
        final StringTokenizer st = new StringTokenizer(version, ".");
        try
        {
            int major = Integer.parseInt(st.nextToken());
            int minor = 0;
            if (st.hasMoreTokens())
            {
                minor = Integer.parseInt(st.nextToken());
            }
            return major == 1 ? minor : major;
        }
        catch (NumberFormatException nfe)
        {
            // maybe some new numbering scheme in the 22nd century
            return 0;
        }
    }
}

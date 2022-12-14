/*
 * Copyright 2014 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tom_roush.pdfbox.multipdf;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.test.platform.app.InstrumentationRegistry;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.android.TestResourceGenerator;
import com.tom_roush.pdfbox.cos.COSArray;
import com.tom_roush.pdfbox.cos.COSBase;
import com.tom_roush.pdfbox.cos.COSDictionary;
import com.tom_roush.pdfbox.cos.COSName;
import com.tom_roush.pdfbox.cos.COSObject;
import com.tom_roush.pdfbox.io.MemoryUsageSetting;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDDocumentCatalog;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageTree;
import com.tom_roush.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import com.tom_roush.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitDestination;
import com.tom_roush.pdfbox.rendering.PDFRenderer;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;

/**
 * Test suite for PDFMergerUtility.
 *
 * @author Maruan Sahyoun (PDF files)
 * @author Tilman Hausherr (code)
 */
public class PDFMergerUtilityTest
{
    final String SRCDIR = "pdfbox/input/merge";
    String TARGETTESTDIR;
    private static File TARGETPDFDIR;
    final int DPI = 96;
    private Context testContext;

    @Before
    public void setUp() throws Exception
    {
        testContext = InstrumentationRegistry.getInstrumentation().getContext();
        PDFBoxResourceLoader.init(testContext);
        TARGETTESTDIR = testContext.getCacheDir() + "/pdfbox-test-output/merge/";
        TARGETPDFDIR = new File(testContext.getCacheDir(), "pdfs");
        TARGETPDFDIR.mkdirs();

        new File(TARGETTESTDIR).mkdirs();
        if (!new File(TARGETTESTDIR).exists())
        {
            throw new IOException("could not create output directory");
        }
    }



    /**
     * Tests whether the merge of two PDF files with identically named but
     * different global resources works. The two PDF files have two fonts each
     * named /TT1 and /TT0 that are Arial and Courier and vice versa in the
     * second file. Revisions before 1613017 fail this test because global
     * resources were merged which made trouble when resources of the same kind
     * had the same name.
     *
     * @throws IOException if something goes wrong.
     */
    @Test
    public void testPDFMergerUtility() throws IOException
    {
        checkMergeIdentical("PDFBox.GlobalResourceMergeTest.Doc01.decoded.pdf",
            "PDFBox.GlobalResourceMergeTest.Doc02.decoded.pdf",
            "GlobalResourceMergeTestResult.pdf",
            MemoryUsageSetting.setupMainMemoryOnly());

        // once again, with scratch file
        checkMergeIdentical("PDFBox.GlobalResourceMergeTest.Doc01.decoded.pdf",
            "PDFBox.GlobalResourceMergeTest.Doc02.decoded.pdf",
            "GlobalResourceMergeTestResult2.pdf",
            MemoryUsageSetting.setupTempFileOnly());
    }

    /**
     * Tests whether the merge of two PDF files with JPEG and CCITT works. A few revisions before
     * 1704911 this test failed because the clone utility attempted to decode and re-encode the
     * streams, see PDFBOX-2893 on 23.9.2015.
     *
     * @throws IOException if something goes wrong.
     */
    @Test
    public void testJpegCcitt() throws IOException
    {
        checkMergeIdentical("jpegrgb.pdf",
            "multitiff.pdf",
            "JpegMultiMergeTestResult.pdf",
            MemoryUsageSetting.setupMainMemoryOnly());

        // once again, with scratch file
        checkMergeIdentical("jpegrgb.pdf",
            "multitiff.pdf",
            "JpegMultiMergeTestResult.pdf",
            MemoryUsageSetting.setupTempFileOnly());
    }

    // see PDFBOX-2893
    @Test
    public void testPDFMergerUtility2() throws IOException
    {
        checkMergeIdentical("PDFBox.GlobalResourceMergeTest.Doc01.pdf",
            "PDFBox.GlobalResourceMergeTest.Doc02.pdf",
            "GlobalResourceMergeTestResult.pdf",
            MemoryUsageSetting.setupMainMemoryOnly());

        // once again, with scratch file
        checkMergeIdentical("PDFBox.GlobalResourceMergeTest.Doc01.pdf",
            "PDFBox.GlobalResourceMergeTest.Doc02.pdf",
            "GlobalResourceMergeTestResult2.pdf",
            MemoryUsageSetting.setupTempFileOnly());
    }

    /**
     * PDFBOX-3972: Test that OpenAction page destination isn't lost after merge.
     *
     * @throws IOException
     */
    @Test
    public void testPDFMergerOpenAction() throws IOException
    {
        PDDocument doc1 = new PDDocument();
        doc1.addPage(new PDPage());
        doc1.addPage(new PDPage());
        doc1.addPage(new PDPage());
        doc1.save(new File(TARGETTESTDIR,"MergerOpenActionTest1.pdf"));
        doc1.close();

        PDDocument doc2 = new PDDocument();
        doc2.addPage(new PDPage());
        doc2.addPage(new PDPage());
        doc2.addPage(new PDPage());
        PDPageDestination dest = new PDPageFitDestination();
        dest.setPage(doc2.getPage(1));
        doc2.getDocumentCatalog().setOpenAction(dest);
        doc2.save(new File(TARGETTESTDIR,"MergerOpenActionTest2.pdf"));
        doc2.close();

        PDFMergerUtility pdfMergerUtility = new PDFMergerUtility();
        pdfMergerUtility.addSource(new File(TARGETTESTDIR, "MergerOpenActionTest1.pdf"));
        pdfMergerUtility.addSource(new File(TARGETTESTDIR, "MergerOpenActionTest2.pdf"));
        pdfMergerUtility.setDestinationFileName(TARGETTESTDIR + "MergerOpenActionTestResult.pdf");
        pdfMergerUtility.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());

        PDDocument mergedDoc = PDDocument.load(new File(TARGETTESTDIR, "MergerOpenActionTestResult.pdf"));
        PDDocumentCatalog documentCatalog = mergedDoc.getDocumentCatalog();
        dest = (PDPageDestination) documentCatalog.getOpenAction();
        assertEquals(4, documentCatalog.getPages().indexOf(dest.getPage()));
        mergedDoc.close();
    }

    /**
     * PDFBOX-3999: check that page entries in the structure tree only reference pages from the page
     * tree, i.e. that no orphan pages exist.
     *
     * @throws IOException
     */
    @Test
    public void testStructureTreeMerge() throws IOException
    {
        File inputPdf = TestResourceGenerator.downloadTestResource(TARGETPDFDIR, "PDFBOX-3999-GeneralForbearance.pdf", "https://issues.apache.org/jira/secure/attachment/12896905/GeneralForbearance.pdf");
        assumeNotNull(inputPdf);

        PDFMergerUtility pdfMergerUtility = new PDFMergerUtility();
        PDDocument src = PDDocument.load(inputPdf);
        PDDocument dst = PDDocument.load(inputPdf);
        pdfMergerUtility.appendDocument(dst, src);
        src.close();
        dst.save(new File(TARGETTESTDIR, "PDFBOX-3999-GeneralForbearance-merged.pdf"));
        dst.close();

        PDDocument doc = PDDocument.load(new File(TARGETTESTDIR, "PDFBOX-3999-GeneralForbearance-merged.pdf"));
        PDPageTree pageTree = doc.getPages();

        // check for orphan pages in the StructTreeRoot/K and StructTreeRoot/ParentTree trees.
        PDStructureTreeRoot structureTreeRoot = doc.getDocumentCatalog().getStructureTreeRoot();
        checkElement(pageTree, structureTreeRoot.getParentTree().getCOSObject());
        checkElement(pageTree, structureTreeRoot.getK());
    }

    /**
     * PDFBOX-3999: check that no streams are kept from the source document by the destination
     * document, despite orphan annotations remaining in the structure tree.
     *
     * @throws IOException
     */
    @Test
    public void testStructureTreeMerge2() throws IOException
    {
        File inputPdf = TestResourceGenerator.downloadTestResource(TARGETPDFDIR, "PDFBOX-3999-GeneralForbearance.pdf", "https://issues.apache.org/jira/secure/attachment/12896905/GeneralForbearance.pdf");
        assumeNotNull(inputPdf);

        PDFMergerUtility pdfMergerUtility = new PDFMergerUtility();
        PDDocument doc = PDDocument.load(inputPdf);
        doc.getDocumentCatalog().getAcroForm().flatten();
        doc.save(new File(TARGETTESTDIR, "PDFBOX-3999-GeneralForbearance-flattened.pdf"));

        ElementCounter elementCounter = new ElementCounter();
        elementCounter.walk(doc.getDocumentCatalog().getStructureTreeRoot().getK());
        int singleCnt = elementCounter.cnt;
        int singleSetSize = elementCounter.set.size();

        doc.close();

        PDDocument src = PDDocument.load(new File(TARGETTESTDIR, "PDFBOX-3999-GeneralForbearance-flattened.pdf"));
        PDDocument dst = PDDocument.load(new File(TARGETTESTDIR, "PDFBOX-3999-GeneralForbearance-flattened.pdf"));
        pdfMergerUtility.appendDocument(dst, src);
        // before solving PDFBOX-3999, the close() below brought
        // IOException: COSStream has been closed and cannot be read.
        src.close();
        dst.save(new File(TARGETTESTDIR, "PDFBOX-3999-GeneralForbearance-flattened-merged.pdf"));
        dst.close();

        doc = PDDocument.load(new File(TARGETTESTDIR, "PDFBOX-3999-GeneralForbearance-flattened-merged.pdf"));
        PDPageTree pageTree = doc.getPages();

        // check for orphan pages in the StructTreeRoot/K and StructTreeRoot/ParentTree trees.
        PDStructureTreeRoot structureTreeRoot = doc.getDocumentCatalog().getStructureTreeRoot();
        checkElement(pageTree, structureTreeRoot.getParentTree().getCOSObject());
        checkElement(pageTree, structureTreeRoot.getK());

        // Assume that the merged tree has double element count
        elementCounter = new ElementCounter();
        elementCounter.walk(structureTreeRoot.getK());
        assertEquals(singleCnt * 2, elementCounter.cnt);
        assertEquals(singleSetSize * 2, elementCounter.set.size());

        doc.close();
    }

    /**
     * PDFBOX-4383: Test that file can be deleted after merge.
     *
     * @throws IOException
     */
    @Test
    public void testFileDeletion() throws IOException
    {
        File outFile = new File(TARGETTESTDIR, "PDFBOX-4383-result.pdf");

        File inFile1 = new File(TARGETTESTDIR, "PDFBOX-4383-src1.pdf");
        File inFile2 = new File(TARGETTESTDIR, "PDFBOX-4383-src2.pdf");

        createSimpleFile(inFile1);
        createSimpleFile(inFile2);

        OutputStream out = new FileOutputStream(outFile);
        PDFMergerUtility merger = new PDFMergerUtility();
        merger.setDestinationStream(out);
        merger.addSource(inFile1);
        merger.addSource(inFile2);
        merger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());
        out.close();

        assertTrue(inFile1.delete());
        assertTrue(inFile2.delete());
        assertTrue(outFile.delete());
    }

    private void createSimpleFile(File file) throws IOException
    {
        PDDocument doc = new PDDocument();
        doc.addPage(new PDPage());
        doc.save(file);
        doc.close();
    }

    private class ElementCounter
    {
        int cnt = 0;
        Set<COSBase> set = new HashSet<COSBase>();

        void walk(COSBase base)
        {
            if (base instanceof COSArray)
            {
                for (COSBase base2 : (COSArray) base)
                {
                    if (base2 instanceof COSObject)
                    {
                        base2 = ((COSObject) base2).getObject();
                    }
                    walk(base2);
                }
            }
            else if (base instanceof COSDictionary)
            {
                COSDictionary kdict = (COSDictionary) base;
                if (kdict.containsKey(COSName.PG))
                {
                    ++cnt;
                    set.add(kdict);
                }
                if (kdict.containsKey(COSName.K))
                {
                    walk(kdict.getDictionaryObject(COSName.K));
                }
            }
        }
    }

    // Each element can be an array, a dictionary or a number.
    // See PDF specification Table 37 - Entries in a number tree node dictionary
    // See PDF specification Table 322 - Entries in the structure tree root
    // See PDF specification Table 323 - Entries in a structure element dictionary
    // example of file with /Kids: 000153.pdf 000208.pdf 000314.pdf 000359.pdf 000671.pdf
    // from digitalcorpora site
    private void checkElement(PDPageTree pageTree, COSBase base)
    {
        if (base instanceof COSArray)
        {
            for (COSBase base2 : (COSArray) base)
            {
                if (base2 instanceof COSObject)
                {
                    base2 = ((COSObject) base2).getObject();
                }
                checkElement(pageTree, base2);
            }
        }
        else if (base instanceof COSDictionary)
        {
            COSDictionary kdict = (COSDictionary) base;
            if (kdict.containsKey(COSName.PG))
            {
                PDStructureElement structureElement = new PDStructureElement(kdict);
                checkForPage(pageTree, structureElement);
            }
            if (kdict.containsKey(COSName.K))
            {
                checkElement(pageTree, kdict.getDictionaryObject(COSName.K));
                return;
            }

            // if we're in a number tree, check /Nums and /Kids
            if (kdict.containsKey(COSName.KIDS))
            {
                checkElement(pageTree, kdict.getDictionaryObject(COSName.KIDS));
            }
            else if (kdict.containsKey(COSName.NUMS))
            {
                checkElement(pageTree, kdict.getDictionaryObject(COSName.NUMS));
            }
        }
    }

    // checks that the result file of a merge has the same rendering as the two
    // source files
    private void checkMergeIdentical(String filename1, String filename2, String mergeFilename,
        MemoryUsageSetting memUsageSetting)
        throws IOException
    {
        PDDocument srcDoc1 = PDDocument.load(testContext.getAssets().open(SRCDIR + "/" + filename1), (String)null);
        int src1PageCount = srcDoc1.getNumberOfPages();
        PDFRenderer src1PdfRenderer = new PDFRenderer(srcDoc1);
        Bitmap[] src1ImageTab = new Bitmap[src1PageCount];
        for (int page = 0; page < src1PageCount; ++page)
        {
            src1ImageTab[page] = src1PdfRenderer.renderImageWithDPI(page, DPI);
        }
        srcDoc1.close();

        PDDocument srcDoc2 = PDDocument.load(testContext.getAssets().open(SRCDIR + "/" + filename2), (String)null);
        int src2PageCount = srcDoc2.getNumberOfPages();
        PDFRenderer src2PdfRenderer = new PDFRenderer(srcDoc2);
        Bitmap[] src2ImageTab = new Bitmap[src2PageCount];
        for (int page = 0; page < src2PageCount; ++page)
        {
            src2ImageTab[page] = src2PdfRenderer.renderImageWithDPI(page, DPI);
        }
        srcDoc2.close();

        PDFMergerUtility pdfMergerUtility = new PDFMergerUtility();
        pdfMergerUtility.addSource(testContext.getAssets().open(SRCDIR + "/" + filename1));
        pdfMergerUtility.addSource(testContext.getAssets().open(SRCDIR + "/" + filename2));
        pdfMergerUtility.setDestinationFileName(TARGETTESTDIR + mergeFilename);
        pdfMergerUtility.mergeDocuments(memUsageSetting);

        PDDocument mergedDoc
            = PDDocument.load(new File(TARGETTESTDIR, mergeFilename), (String)null);
        PDFRenderer mergePdfRenderer = new PDFRenderer(mergedDoc);
        int mergePageCount = mergedDoc.getNumberOfPages();
        assertEquals(src1PageCount + src2PageCount, mergePageCount);
        for (int page = 0; page < src1PageCount; ++page)
        {
            Bitmap bim = mergePdfRenderer.renderImageWithDPI(page, DPI);
            checkImagesIdentical(bim, src1ImageTab[page]);
        }
        for (int page = 0; page < src2PageCount; ++page)
        {
            int mergePage = page + src1PageCount;
            Bitmap bim = mergePdfRenderer.renderImageWithDPI(mergePage, DPI);
            checkImagesIdentical(bim, src2ImageTab[page]);
        }
        mergedDoc.close();
    }

    private void checkImagesIdentical(Bitmap bim1, Bitmap bim2)
    {
        assertEquals(bim1.getHeight(), bim2.getHeight());
        assertEquals(bim1.getWidth(), bim2.getWidth());
        int w = bim1.getWidth();
        int h = bim1.getHeight();
        for (int i = 0; i < w; ++i)
        {
            for (int j = 0; j < h; ++j)
            {
                assertEquals(bim1.getPixel(i, j), bim2.getPixel(i, j));
            }
        }
    }

    private void checkForPage(PDPageTree pageTree, PDStructureElement structureElement)
    {
        PDPage page = structureElement.getPage();
        if (page != null)
        {
            assertTrue("Page is not in the page tree", pageTree.indexOf(page) != -1);
        }
    }
}

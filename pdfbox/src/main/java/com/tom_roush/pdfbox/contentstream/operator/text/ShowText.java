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
package com.tom_roush.pdfbox.contentstream.operator.text;

import android.util.Log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.tom_roush.pdfbox.contentstream.operator.Operator;
import com.tom_roush.pdfbox.contentstream.operator.OperatorProcessor;
import com.tom_roush.pdfbox.cos.COSBase;
import com.tom_roush.pdfbox.cos.COSString;

/**
 * Tj: Show text.
 *
 * @author Laurent Huault
 */
public class ShowText extends OperatorProcessor
{
    @Override
    public void process(Operator operator, List<COSBase> arguments) throws IOException
    {
        if (arguments.size() < 1)
        {
            // ignore ( )Tj
            return;
        }
        COSBase base = arguments.get(0);
        if (!(base instanceof COSString))
        {
            // ignore
            return;
        }
        if (context.getTextMatrix() == null)
        {
            // ignore: outside of BT...ET
            return;
        }
        COSString string = (COSString) base;
//        Log.w("ceshi","showText:"+new String(string.getBytes(),StandardCharsets.UTF_16BE));
        context.showTextString(string.getBytes());
    }

    @Override
    public String getName()
    {
        return "Tj";
    }
}

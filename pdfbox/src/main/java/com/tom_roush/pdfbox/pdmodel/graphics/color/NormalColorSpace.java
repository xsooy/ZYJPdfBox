package com.tom_roush.pdfbox.pdmodel.graphics.color;

public class NormalColorSpace {

    public static final int TYPE_XYZ = 0;

    public static final int TYPE_Lab = 1;

    public static final int TYPE_RGB = 5;

    public static final int TYPE_GRAY = 6;

    public static final int TYPE_CMYK = 9;

    public static float[] ycckToCmyk(int origin[]) {
        float[] value = new float[4];
        value[0] = origin[0] + 1.402f*origin[2]-179.456f;
        value[1] = origin[0] - 0.34414f*origin[1] - 0.71414f*origin[2] + 135.45984f;
        value[2] = origin[0] + 1.772f*origin[1] - 226.816f;
        value[0] = (255-value[0])/255;
        value[1] = (255-value[1])/255;
        value[2] = (255-value[2])/255;
        value[3] = origin[3]/255.f;
        return value;
    }

    public static float[] toLab(float[] value) {
        float[] lab = new float[3];
        for (int i=0;i<3;i++) {
            lab[i] = (getMaxValue(TYPE_Lab,i)-getMinValue(TYPE_Lab,i))*value[i]+getMinValue(TYPE_Lab,i);
        }
//        lab[0] = value[0] * 100;
//        float min = -128.0f;
//        float max = 127.0f;
//        lab[1] = (max-min)*value[1]+min;
//        lab[2] = (max-min)*value[2]+min;
        return lab;
    }

    public static float[] labToXyz(float[] value) {
        float[] xyz = new float[3];
        xyz[1] = (value[0]+16.f)/116.f;
        xyz[0] = value[1]/500.f+xyz[1];
        xyz[2] = xyz[1]-value[2]/200.f;

        for (int i = 0; i < 3; i++)
        {
            float pow = xyz[i] * xyz[i] * xyz[i];
            float ratio = (6.0f / 29.0f);
            if (xyz[i] > ratio)
            {
                xyz[i] = pow;
            }
            else
            {
                xyz[i] = (3.0f * (6.0f / 29.0f) * (6.0f / 29.0f) * (xyz[i] - (4.0f / 29.0f)));
            }
        }

        xyz[0] = xyz[0] * (95.047f);
        xyz[1] = xyz[1] * (100.0f);
        xyz[2] = xyz[2] * (108.883f);

        xyz[0] = xyz[0] / 100f;
        xyz[1] = xyz[1] / 100f;
        xyz[2] = xyz[2] / 100f;

        return xyz;
    }


    //x,y,z To r=[0],g=[1],b=[2]
    public static float[] xyzToRgb(float[] xyz) {
        float[] rgb = new float[3];
        rgb[0] = (xyz[0] * 3.240479f) + (xyz[1] * -1.537150f) + (xyz[2] * -.498535f);
        rgb[1] = (xyz[0] * -.969256f) + (xyz[1] *  1.875992f) + (xyz[2] * .041556f);
        rgb[2] = (xyz[0] * .055648f) +  (xyz[1] * -.204043f) + (xyz[2] * 1.057311f);

        for (int i = 0; i < 3; i++)
        {
            if (rgb[i] > .0031308f)
            {
                rgb[i] = (1.055f * (float)Math.pow(rgb[i], (1.0f / 2.4f))) - .055f;
            }
            else
            {
                rgb[i] = rgb[i] * 12.92f;
            }
        }
        return rgb;
    }

    public static int getIccColorType(int code) {
        switch (code) {
            case 0x47524159:
                return TYPE_GRAY;
            case 0x434D594B:
                return TYPE_CMYK;
//            case 0x52474220:
//                return TYPE_RGB;
            default:
                return TYPE_RGB;
        }
    }

    public static float getMinValue(int colorType,int index) {
        switch (colorType) {
            case TYPE_XYZ:
                return 0.0f; // X, Y, Z
//                result[1] = 1.0f + (32767.0f / 32768.0f);
//                break;
            case TYPE_Lab:
                switch (index) {
                    case 0:
                        return 0.0f;
                    case 1:
                    case 2:
                        return -128.0f;
                }
            default:
                return 0.0f;
        }
    }

    public static float getMaxValue(int colorType,int index) {
        switch (colorType) {
            case TYPE_XYZ:
//                return 0.0f; // X, Y, Z
                return 1.0f + (32767.0f / 32768.0f);
            case TYPE_Lab:
                switch (index) {
                    case 0:
                        return 100.0f;
                    case 1:
                    case 2:
                        return 127.0f;
                }
            default:
                return 1.0f;
        }
    }

}

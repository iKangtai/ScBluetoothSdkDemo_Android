package com.example.blesdkdemo.auido;

public class AudioFilter {
    public static double[] mh = {0.0d, 2.5E-4d, 6.54E-4d, 1.11E-4d, -0.002612d, -0.00777d, -0.013704d, -0.016689d, -0.011909d, 0.00466d, 0.034184d, 0.073635d, 0.11594d, 0.15175d, 0.172298d, 0.172298d, 0.15175d, 0.11594d, 0.073635d, 0.034184d, 0.00466d, -0.011909d, -0.016689d, -0.013704d, -0.00777d, -0.002612d, 1.11E-4d, 6.54E-4d, 2.5E-4d, 0.0d};
    public static double[] mh1 = {0.0d, 6.60172E-6d, 2.24135E-5d, 2.58951E-5d, -1.31774E-5d, -1.10552E-4d, -2.45884E-4d, -3.56538E-4d, -3.55262E-4d, -1.69334E-4d, 2.12976E-4d, 7.10198E-4d, 0.00114998d, 0.00131519d, 0.00102542d, 2.27702E-4d, -9.40352E-4d, -0.00214879d, -0.00295136d, -0.002928d, -0.00185342d, 1.67554E-4d, 0.002657d, 0.00484964d, 0.00590319d, 0.00518245d, 0.00252986d, -0.00157637d, -0.00605399d, -0.00946331d, -0.0104246d, -0.00809584d, -0.00256215d, 0.00500784d, 0.0125303d, 0.0175013d, 0.0177175d, 0.0120358d, 9.4827E-4d, -0.0132281d, -0.0266568d, -0.0347623d, -0.0333272d, -0.0196421d, 0.0065973d, 0.042905d, 0.0842762d, 0.124093d, 0.155475d, 0.172761d, 0.172761d, 0.155475d, 0.124093d, 0.0842762d, 0.042905d, 0.0065973d, -0.0196421d, -0.0333272d, -0.0347623d, -0.0266568d, -0.0132281d, 9.4827E-4d, 0.0120358d, 0.0177175d, 0.0175013d, 0.0125303d, 0.00500784d, -0.00256215d, -0.00809584d, -0.0104246d, -0.00946331d, -0.00605399d, -0.00157637d, 0.00252986d, 0.00518245d, 0.00590319d, 0.00484964d, 0.002657d, 1.67554E-4d, -0.00185342d, -0.002928d, -0.00295136d, -0.00214879d, -9.40352E-4d, 2.27702E-4d, 0.00102542d, 0.00131519d, 0.00114998d, 7.10198E-4d, 2.12976E-4d, -1.69334E-4d, -3.55262E-4d, -3.56538E-4d, -2.45884E-4d, -1.10552E-4d, -1.31774E-5d, 2.58951E-5d, 2.24135E-5d, 6.60172E-6d, 0.0d};
    public static double[] mh3 = {2.66E-5d, 8.74579E-5d, -6.31443E-4d, -6.00684E-4d, 3.70135E-4d, 5.15019E-4d, -4.74407E-4d, -4.06744E-4d, 0.00117669d, 0.00148687d, -8.69804E-5d, 5.86156E-5d, 0.00270446d, 0.00319677d, 5.52097E-4d, 7.04005E-4d, 0.00482445d, 0.00537445d, 9.14909E-4d, 8.38354E-4d, 0.00680722d, 0.00714598d, -1.50754E-4d, -8.13798E-4d, 0.007464d, 0.00727833d, -0.00411837d, -0.00572049d, 0.0056824d, 0.00479456d, -0.0122007d, -0.0149346d, 0.00119149d, -2.44549E-4d, -0.0248214d, -0.0286532d, -0.00468912d, -0.00601215d, -0.0416775d, -0.0465563d, -0.00822262d, -0.00789404d, -0.0636123d, -0.070718d, 3.1372E-4d, 0.00784319d, -0.10628d, -0.133326d, 0.10517d, 0.405102d, 0.405102d, 0.10517d, -0.133326d, -0.10628d, 0.00784319d, 3.1372E-4d, -0.070718d, -0.0636123d, -0.00789404d, -0.00822262d, -0.0465563d, -0.0416775d, -0.00601215d, -0.00468912d, -0.0286532d, -0.0248214d, -2.44549E-4d, 0.00119149d, -0.0149346d, -0.0122007d, 0.00479456d, 0.0056824d, -0.00572049d, -0.00411837d, 0.00727833d, 0.007464d, -8.13798E-4d, -1.50754E-4d, 0.00714598d, 0.00680722d, 8.38354E-4d, 9.14909E-4d, 0.00537445d, 0.00482445d, 7.04005E-4d, 5.52097E-4d, 0.00319677d, 0.00270446d, 5.86156E-5d, -8.69804E-5d, 0.00148687d, 0.00117669d, -4.06744E-4d, -4.74407E-4d, 5.15019E-4d, 3.70135E-4d, -6.00684E-4d, -6.31443E-4d, 8.74579E-5d, 2.66E-5d};

    public static void Filter(short[] sArr, short[] sArr2, int i) {
        double[] dArr = mh1;
        short[] sArr3 = new short[100];
        int i2 = i + 100;
        int[] iArr = new int[i2];
        for (int i3 = 0; i3 < 100; i3++) {
            iArr[i3] = (int) (((double) sArr3[i3]) * 0.8d);
            if (iArr[i3] > 32767) {
                iArr[i3] = 32767;
            } else if (iArr[i3] < -32767) {
                iArr[i3] = -32767;
            }
        }
        for (int i4 = 100; i4 < i2; i4++) {
            iArr[i4] = (int) (((double) sArr[i4 - 100]) * 0.8d);
            if (iArr[i4] > 32767) {
                iArr[i4] = 32767;
            } else if (iArr[i4] < -32767) {
                iArr[i4] = -32767;
            }
        }
        if (i >= 100) {
            for (int i5 = 0; i5 < 100; i5++) {
                sArr3[i5] = sArr[(i - 100) + i5];
            }
        } else {
            int i6 = 100 - i;
            int i7 = 0;
            while (i7 < i6) {
                sArr3[i7] = sArr3[i7 + i];
                i7++;
            }
            for (int i8 = 0; i8 < i; i8++) {
                sArr3[i7] = sArr[i8];
                i7++;
            }
        }
        for (int i9 = 0; i9 < i; i9++) {
            double d = 0.0d;
            for (int i10 = 0; i10 < 100; i10++) {
                d += dArr[i10] * ((double) iArr[(i9 - i10) + 100]);
            }
            double d2 = 32767.0d;
            if (d <= 32767.0d) {
                d2 = d < -32767.0d ? -32767.0d : d;
            }
            sArr2[i9] = (short) ((int) d2);
        }
    }
}
package com.example.blesdkdemo.auido;

import android.util.Log;

import com.ikangtai.bluetoothsdk.util.BleUtils;

public class AudioCompress {
    private State decodeState = new State();
    private int[] indexTable = {-1, -1, -1, -1, 2, 4, 6, 8, -1, -1, -1, -1, 2, 4, 6, 8};
    private int[] stepsizeTable = {7, 8, 9, 10, 11, 12, 13, 14, 16, 17, 19, 21, 23, 25, 28, 31, 34, 37, 41, 45, 50, 55, 60, 66, 73, 80, 88, 97, 107, 118, 130, 143, 157, 173, 190, 209, 230, 253, 279, 307, 337, 371, 408, 449, 494, 544, 598, 658, 724, 796, 876, 963, 1060, 1166, 1282, 1411, 1552, 1707, 1878, 2066, 2272, 2499, 2749, 3024, 3327, 3660, 4026, 4428, 4871, 5358, 5894, 6484, 7132, 7845, 8630, 9493, 10442, 11487, 12635, 13899, 15289, 16818, 18500, 20350, 22385, 24623, 27086, 29794, 32767};

    public static class State {
        int index;
        short valprev;

        public State() {
        }
    }

    public AudioCompress() {
        this.decodeState.index = 0;
        this.decodeState.valprev = 0;
    }

    public int decoder0(byte[] indata, short[] outdata, int len, State state) {
        int delta;
        int step;
        int valpred;
        int vpdiff;
        int index;
        int inputbuffer = 0;
        int count = 0;

        valpred = state.valprev;
        index = state.index;
        step = stepsizeTable[index];
        int i10 = 0;
        while (len-- > 0) {

            inputbuffer = indata[i10] & 255;
            i10++;
            delta = (inputbuffer >> 4);

            index += indexTable[delta];
            if (index < 0) index = 0;
            else if (index > 88) index = 88;


            vpdiff = step >> 3;
            if ((delta & 4) != 0) vpdiff += step;
            if ((delta & 2) != 0) vpdiff += step >> 1;
            if ((delta & 1) != 0) vpdiff += step >> 2;

            if ((delta & 8) != 0) {
                valpred -= vpdiff;
                if (valpred < -32768)
                    valpred = -32768;
            } else {
                valpred += vpdiff;
                if (valpred > 32767)
                    valpred = 32767;
            }

            step = stepsizeTable[index];

            outdata[i10] = (short) valpred;
            i10++;
            delta = inputbuffer & 0xf;

            index += indexTable[delta];
            if (index < 0) index = 0;
            else if (index > 88) index = 88;

            vpdiff = step >> 3;
            if ((delta & 4) != 0) vpdiff += step;
            if ((delta & 2) != 0) vpdiff += step >> 1;
            if ((delta & 1) != 0) vpdiff += step >> 2;

            if ((delta & 8) != 0) {
                valpred -= vpdiff;
                if (valpred < -32768)
                    valpred = -32768;
            } else {
                valpred += vpdiff;
                if (valpred > 32767)
                    valpred = 32767;
            }

            step = stepsizeTable[index];

            outdata[i10] = (short) valpred;
            i10++;
            count += 2;
        }

        state.valprev = (short) valpred;
        state.index = (char) index;

        return count;
    }

    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r7v0, resolved type: short} */
    /* JADX WARNING: Multi-variable type inference failed */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int decoder(byte[] bArr, short[] sArr, int i, State state) {
        int i2;
        int i3;
        short s = state.valprev;
        int i4 = state.index;
        short s2 = s;
        int i5 = this.stepsizeTable[i4];
        int i6 = 0;
        int i7 = 0;
        int i8 = i;
        int i9 = i4;
        int i10 = 0;
        while (true) {
            int i11 = i8 - 1;
            if (i8 > 0) {
                int i12 = bArr[i10] & 255;
                i10++;
                int i13 = i12 >> 4;
                int i14 = i9 + this.indexTable[i13];
                if (i14 < 0) {
                    i14 = 0;
                } else if (i14 > 88) {
                    i14 = 88;
                }
                int i15 = i5 >> 3;
                if ((i13 & 4) != 0) {
                    i15 += i5;
                }
                if ((i13 & 2) != 0) {
                    i15 += i5 >> 1;
                }
                if ((i13 & 1) != 0) {
                    i15 += i5 >> 2;
                }
                if ((i13 & 8) != 0) {
                    i2 = s2 - i15;
                    if (i2 < -32768) {
                        i2 = -32768;
                    }
                } else {
                    i2 = s2 + i15;
                    if (i2 > 32767) {
                        i2 = 32767;
                    }
                }
                int i16 = this.stepsizeTable[i14];
                sArr[i7] = (short) i2;
                int i17 = i7 + 1;
                int i18 = i12 & 15;
                i9 = i14 + this.indexTable[i18];
                if (i9 < 0) {
                    i9 = 0;
                } else if (i9 > 88) {
                    i9 = 88;
                }
                int i19 = i16 >> 3;
                if ((i18 & 4) != 0) {
                    i19 += i16;
                }
                if ((i18 & 2) != 0) {
                    i19 += i16 >> 1;
                }
                if ((i18 & 1) != 0) {
                    i19 += i16 >> 2;
                }
                if ((i18 & 8) != 0) {
                    i3 = i2 - i19;
                    if (i3 < -32768) {
                        i3 = -32768;
                    }
                } else {
                    i3 = i2 + i19;
                    if (i3 > 32767) {
                        i3 = 32767;
                    }
                }
                i5 = this.stepsizeTable[i9];
                sArr[i17] = (short) i3;
                i7 = i17 + 1;
                i6 += 2;
                i8 = i11;
                s2 = (short) i3;
            } else {
                state.valprev = s2 == 1 ? (short) 1 : 0;
                state.index = (byte) i9;
                return i6;
            }
        }
    }

    public int decode_frame(byte[] indata, short[] outdata, int len) {
        int ret = -1;
        byte sum;
        int i;
        int count;
        int i2;
        byte[] bArr2 = new byte[len - 8];
        State state = new State();
        if ((indata[0] == (byte)0xFF) && (indata[1] == (byte)0xAA) && (indata[len - 1] == (byte)0x55)) {
            sum = indata[0];
            for (i = 1; i < len - 2; i++) {
                sum += indata[i];
            }

            if (sum == indata[len - 2]) {//是正确的一帧数据，解析胎心率和胎心音
                ret = indata[5] & 255;
                state.index = indata[4] & 255;
                state.valprev = (short) ((indata[2] & 255) | ((indata[3] & 255) << 8));
                for (int i6 = 6; i6 < len - 2; i6++) {
                    bArr2[i6 - 6] = indata[i6];
                }
                decoder(bArr2, outdata, len - 8, state);
            }
        }
        return ret;
    }

    public int decode_frame0(byte[] bArr, short[] sArr, int i) {
        int i2;
        int i3 = i - 8;
        byte[] bArr2 = new byte[i3];
        State state = new State();
        if ((bArr[0] & 255) == 255) {
            int i4 = 1;
            if ((bArr[1] & 255) == 170 && (bArr[i - 1] & 255) == 85) {
                byte b = bArr[0];
                i2 = i - 2;
                for (i = 0; i4 < i2; i4++) {
                    b = (byte) ((b == 1 ? 1 : 0) + (bArr[i4] & 255));
                }
                if (((b == 1 ? 1 : 0) & 255) == (bArr[i2] & 255)) {
                    int i5 = bArr[5] & 255;
                    for (int i6 = 6; i6 < i2; i6++) {
                        bArr2[i6 - 6] = bArr[i6];
                    }
                    state.index = bArr[4] & 255;
                    state.valprev = (short) ((bArr[2] & 255) | ((bArr[3] & 255) << 8));
                    decoder(bArr2, sArr, i3, state);
                    return i5;
                }
            }
        }
        return -1;
    }

}
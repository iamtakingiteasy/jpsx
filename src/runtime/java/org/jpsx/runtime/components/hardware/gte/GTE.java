/*
Copyright (C) 2007 graham sanderson

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package org.jpsx.runtime.components.hardware.gte;

import org.apache.bcel.generic.*;
import org.apache.log4j.Logger;
import org.jpsx.api.components.core.addressspace.AddressSpace;
import org.jpsx.api.components.core.cpu.*;
import org.jpsx.runtime.JPSXComponent;
import org.jpsx.runtime.components.core.CoreComponentConnections;
import org.jpsx.runtime.util.ClassUtil;

// TODO should vz be 16 bits only when read?

public final class GTE extends JPSXComponent implements InstructionProvider {
    private static final Logger log = Logger.getLogger("GTE");
    private static final String CLASS = GTE.class.getName();
    private static final String VECTOR_CLASS = Vector.class.getName();
    private static final String VECTOR_SIGNATURE = ClassUtil.signatureOfClass(VECTOR_CLASS);
    private static final String MATRIX_CLASS = Matrix.class.getName();
    private static final String MATRIX_SIGNATURE = ClassUtil.signatureOfClass(MATRIX_CLASS);

    private static final int R_VXY0 = 0;
    private static final int R_VZ0 = 1;
    private static final int R_VXY1 = 2;
    private static final int R_VZ1 = 3;
    private static final int R_VXY2 = 4;
    private static final int R_VZ2 = 5;
    private static final int R_RGB = 6;
    private static final int R_OTZ = 7;
    private static final int R_IR0 = 8;
    private static final int R_IR1 = 9;
    private static final int R_IR2 = 10;
    private static final int R_IR3 = 11;
    private static final int R_SXY0 = 12;
    private static final int R_SXY1 = 13;
    private static final int R_SXY2 = 14;
    private static final int R_SXYP = 15;
    private static final int R_SZX = 16;
    private static final int R_SZ0 = 17;
    private static final int R_SZ1 = 18;
    private static final int R_SZ2 = 19;
    private static final int R_RGB0 = 20;
    private static final int R_RGB1 = 21;
    private static final int R_RGB2 = 22;
    private static final int R_RES1 = 23;
    private static final int R_MAC0 = 24;
    private static final int R_MAC1 = 25;
    private static final int R_MAC2 = 26;
    private static final int R_MAC3 = 27;
    private static final int R_IRGB = 28;
    private static final int R_ORGB = 29;
    private static final int R_LZCS = 30;
    private static final int R_LZCR = 31;
    private static final int R_R11R12 = 32;
    private static final int R_R13R21 = 33;
    private static final int R_R22R23 = 34;
    private static final int R_R31R32 = 35;
    private static final int R_R33 = 36;
    private static final int R_TRX = 37;
    private static final int R_TRY = 38;
    private static final int R_TRZ = 39;
    private static final int R_L11L12 = 40;
    private static final int R_L13L21 = 41;
    private static final int R_L22L23 = 42;
    private static final int R_L31L32 = 43;
    private static final int R_L33 = 44;
    private static final int R_RBK = 45;
    private static final int R_GBK = 46;
    private static final int R_BBK = 47;
    private static final int R_LR1LR2 = 48;
    private static final int R_LR3LG1 = 49;
    private static final int R_LG2LG3 = 50;
    private static final int R_LB1LB2 = 51;
    private static final int R_LB3 = 52;
    private static final int R_RFC = 53;
    private static final int R_GFC = 54;
    private static final int R_BFC = 55;
    private static final int R_OFX = 56;
    private static final int R_OFY = 57;
    private static final int R_H = 58;
    private static final int R_DQA = 59;
    private static final int R_DQB = 60;
    private static final int R_ZSF3 = 61;
    private static final int R_ZSF4 = 62;
    private static final int R_FLAG = 63;

    private static final int GTE_SF_MASK = 0x80000;

    private static final int GTE_MX_MASK = 0x60000;
    private static final int GTE_MX_ROTATION = 0x00000;
    private static final int GTE_MX_LIGHT = 0x20000;
    private static final int GTE_MX_COLOR = 0x40000;

    private static final int GTE_V_MASK = 0x18000;
    private static final int GTE_V_V0 = 0x00000;
    private static final int GTE_V_V1 = 0x08000;
    private static final int GTE_V_V2 = 0x10000;
    private static final int GTE_V_IR = 0x18000;

    private static final int GTE_CV_MASK = 0x06000;
    private static final int GTE_CV_TR = 0x00000;
    private static final int GTE_CV_BK = 0x02000;
    private static final int GTE_CV_FC = 0x04000;
    private static final int GTE_CV_NONE = 0x06000;

    private static final int GTE_LM_MASK = 0x00400;

    private static final int GTE_ALL_MASKS = (GTE_SF_MASK | GTE_MX_MASK | GTE_V_MASK | GTE_CV_MASK | GTE_LM_MASK);

//    public static void setFlag( int bits)
//    {
//        reg_flag |= bits;
//
//        // TODO check these flags!
//        // CHK is set if any flag in 0x7FC7E000 is set
//        //      (A1-A3 B1-B3 D E FP FN G1 G2)
//
//        if (0!=(bits & 0x7fc7e000)) {
//            reg_flag|=FLAG_CHK;
//        }
//    }

// removed above, and rolled FLAG_CHK into them automatically

    private static final int FLAG_CHK = 0x80000000;
    private static final int FLAG_A1P = 0x40000000 | FLAG_CHK;
    private static final int FLAG_A2P = 0x20000000 | FLAG_CHK;
    private static final int FLAG_A3P = 0x10000000 | FLAG_CHK;
    private static final int FLAG_A1N = 0x08000000 | FLAG_CHK;
    private static final int FLAG_A2N = 0x04000000 | FLAG_CHK;
    private static final int FLAG_A3N = 0x02000000 | FLAG_CHK;
    private static final int FLAG_B1 = 0x01000000 | FLAG_CHK;
    private static final int FLAG_B2 = 0x00800000 | FLAG_CHK;
    private static final int FLAG_B3 = 0x00400000 | FLAG_CHK;
    private static final int FLAG_C1 = 0x00200000;
    private static final int FLAG_C2 = 0x00100000;
    private static final int FLAG_C3 = 0x00080000;
    private static final int FLAG_D = 0x00040000 | FLAG_CHK;
    private static final int FLAG_E = 0x00020000 | FLAG_CHK;
    private static final int FLAG_FP = 0x00010000 | FLAG_CHK;
    private static final int FLAG_FN = 0x00008000 | FLAG_CHK;
    private static final int FLAG_G1 = 0x00004000 | FLAG_CHK;
    private static final int FLAG_G2 = 0x00002000 | FLAG_CHK;
    private static final int FLAG_H = 0x00001000;

    private static final long BIT44 = 0x100000000000L;
    private static final long BIT31 = 0x80000000L;
    private static final long BIT47 = 0x800000000000L;

    public static class Vector {
        public int x, y, z;
    }

    public static class Matrix {
        public int m11, m12, m13, m21, m22, m23, m31, m32, m33;
    }

    public static Vector reg_v0 = new Vector();
    public static Vector reg_v1 = new Vector();
    public static Vector reg_v2 = new Vector();

    public static int reg_rgb;
    public static int reg_otz;

    public static int reg_ir0;
    public static int reg_ir1;
    public static int reg_ir2;
    public static int reg_ir3;

    public static int reg_sx0;
    public static int reg_sy0;
    public static int reg_sx1;
    public static int reg_sy1;
    public static int reg_sx2;
    public static int reg_sy2;
    public static int reg_sxp;
    public static int reg_syp;

    public static int reg_szx;
    public static int reg_sz0;
    public static int reg_sz1;
    public static int reg_sz2;
    public static int reg_rgb0;
    public static int reg_rgb1;
    public static int reg_rgb2;
    public static int reg_res1;
    public static int reg_mac0;
    public static int reg_mac1;
    public static int reg_mac2;
    public static int reg_mac3;
    public static int reg_irgb;
    public static int reg_orgb;
    public static int reg_lzcr;
    public static int reg_lzcs;

    public static Matrix reg_rot = new Matrix();

    public static int reg_trx;
    public static int reg_try;
    public static int reg_trz;

    public static Matrix reg_ls = new Matrix();
    public static int reg_rbk;
    public static int reg_gbk;
    public static int reg_bbk;

    public static Matrix reg_lc = new Matrix();
    public static int reg_rfc;
    public static int reg_gfc;
    public static int reg_bfc;

    public static int reg_ofx;
    public static int reg_ofy;
    public static int reg_h;
    public static int reg_dqa;
    public static int reg_dqb;

    public static int reg_zsf3;
    public static int reg_zsf4;
    public static int reg_flag;

    public GTE() {
        super("JPSX Geometry Transform Engine");
    }

    private static AddressSpace addressSpace;
    private static R3000 r3000;
    private static int[] r3000regs;

    private static void emitReadReg(InstructionList il, CompilationContext context, int reg) {
        ConstantPoolGen cp = context.getConstantPoolGen();

        if (reg < 32) {
            switch (reg) {
                case R_VXY0:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_v0", VECTOR_SIGNATURE)));
                    il.append(new GETFIELD(context.getConstantPoolGen().addFieldref(VECTOR_CLASS, "x", "I")));
                    il.append(new PUSH(cp, 0xffff));
                    il.append(new IAND());
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_v0", VECTOR_SIGNATURE)));
                    il.append(new GETFIELD(context.getConstantPoolGen().addFieldref(VECTOR_CLASS, "y", "I")));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHL());
                    il.append(new IOR());
                    break;
                case R_VZ0:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_v0", VECTOR_SIGNATURE)));
                    il.append(new GETFIELD(context.getConstantPoolGen().addFieldref(VECTOR_CLASS, "z", "I")));
                    break;
                case R_VXY1:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_v1", VECTOR_SIGNATURE)));
                    il.append(new GETFIELD(context.getConstantPoolGen().addFieldref(VECTOR_CLASS, "x", "I")));
                    il.append(new PUSH(cp, 0xffff));
                    il.append(new IAND());
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_v1", VECTOR_SIGNATURE)));
                    il.append(new GETFIELD(context.getConstantPoolGen().addFieldref(VECTOR_CLASS, "y", "I")));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHL());
                    il.append(new IOR());
                    break;
                case R_VZ1:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_v1", VECTOR_SIGNATURE)));
                    il.append(new GETFIELD(context.getConstantPoolGen().addFieldref(VECTOR_CLASS, "z", "I")));
                    break;
                case R_VXY2:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_v2", VECTOR_SIGNATURE)));
                    il.append(new GETFIELD(context.getConstantPoolGen().addFieldref(VECTOR_CLASS, "x", "I")));
                    il.append(new PUSH(cp, 0xffff));
                    il.append(new IAND());
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_v2", VECTOR_SIGNATURE)));
                    il.append(new GETFIELD(context.getConstantPoolGen().addFieldref(VECTOR_CLASS, "y", "I")));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHL());
                    il.append(new IOR());
                    break;
                case R_VZ2:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_v2", VECTOR_SIGNATURE)));
                    il.append(new GETFIELD(context.getConstantPoolGen().addFieldref(VECTOR_CLASS, "z", "I")));
                    break;
                case R_RGB:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_rgb", "I")));
                    break;
                case R_OTZ:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_otz", "I")));
                    break;
                case R_IR0:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_ir0", "I")));
                    break;
                case R_IR1:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_ir1", "I")));
                    break;
                case R_IR2:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_ir2", "I")));
                    break;
                case R_IR3:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_ir3", "I")));
                    break;
                case R_SXY0:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_sx0", "I")));
                    il.append(new PUSH(cp, 0xffff));
                    il.append(new IAND());
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_sy0", "I")));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHL());
                    il.append(new IOR());
                    break;
                case R_SXY1:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_sx1", "I")));
                    il.append(new PUSH(cp, 0xffff));
                    il.append(new IAND());
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_sy1", "I")));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHL());
                    il.append(new IOR());
                    break;
                case R_SXY2:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_sx2", "I")));
                    il.append(new PUSH(cp, 0xffff));
                    il.append(new IAND());
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_sy2", "I")));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHL());
                    il.append(new IOR());
                    break;
                case R_SXYP:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_sxp", "I")));
                    il.append(new PUSH(cp, 0xffff));
                    il.append(new IAND());
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_syp", "I")));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHL());
                    il.append(new IOR());
                    break;
                case R_SZX:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_szx", "I")));
                    break;
                case R_SZ0:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_sz0", "I")));
                    break;
                case R_SZ1:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_sz1", "I")));
                    break;
                case R_SZ2:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_sz2", "I")));
                    break;
                case R_RGB0:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_rgb0", "I")));
                    break;
                case R_RGB1:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_rgb1", "I")));
                    break;
                case R_RGB2:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_rgb2", "I")));
                    break;
                case R_RES1:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_res1", "I")));
                    break;
                case R_MAC0:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_mac0", "I")));
                    break;
                case R_MAC1:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_mac1", "I")));
                    break;
                case R_MAC2:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_mac2", "I")));
                    break;
                case R_MAC3:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_mac3", "I")));
                    break;
                case R_IRGB:
                    // todo check this
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_irgb", "I")));
                    break;
                case R_ORGB:
                    // todo check this
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_orgb", "I")));
                    break;
                case R_LZCS:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_lzcs", "I")));
                    break;
                case R_LZCR:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_lzcr", "I")));
                    break;
            }
        } else {
            switch (reg) {
                case R_R11R12:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_rot", MATRIX_SIGNATURE)));
                    il.append(new GETFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m11", "I")));
                    il.append(new PUSH(cp, 0xffff));
                    il.append(new IAND());
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_rot", MATRIX_SIGNATURE)));
                    il.append(new GETFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m12", "I")));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHL());
                    il.append(new IOR());
                    break;
                case R_R13R21:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_rot", MATRIX_SIGNATURE)));
                    il.append(new GETFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m13", "I")));
                    il.append(new PUSH(cp, 0xffff));
                    il.append(new IAND());
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_rot", MATRIX_SIGNATURE)));
                    il.append(new GETFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m21", "I")));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHL());
                    il.append(new IOR());
                    break;
                case R_R22R23:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_rot", MATRIX_SIGNATURE)));
                    il.append(new GETFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m22", "I")));
                    il.append(new PUSH(cp, 0xffff));
                    il.append(new IAND());
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_rot", MATRIX_SIGNATURE)));
                    il.append(new GETFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m23", "I")));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHL());
                    il.append(new IOR());
                    break;
                case R_R31R32:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_rot", MATRIX_SIGNATURE)));
                    il.append(new GETFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m31", "I")));
                    il.append(new PUSH(cp, 0xffff));
                    il.append(new IAND());
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_rot", MATRIX_SIGNATURE)));
                    il.append(new GETFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m32", "I")));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHL());
                    il.append(new IOR());
                    break;
                case R_R33:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_rot", MATRIX_SIGNATURE)));
                    il.append(new GETFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m33", "I")));
                    il.append(new PUSH(cp, 0xffff));
                    il.append(new IAND());
                    break;
                case R_TRX:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_trx", "I")));
                    break;
                case R_TRY:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_try", "I")));
                    break;
                case R_TRZ:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_trz", "I")));
                    break;
                case R_L11L12:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_ls", MATRIX_SIGNATURE)));
                    il.append(new GETFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m11", "I")));
                    il.append(new PUSH(cp, 0xffff));
                    il.append(new IAND());
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_ls", MATRIX_SIGNATURE)));
                    il.append(new GETFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m12", "I")));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHL());
                    il.append(new IOR());
                    break;
                case R_L13L21:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_ls", MATRIX_SIGNATURE)));
                    il.append(new GETFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m13", "I")));
                    il.append(new PUSH(cp, 0xffff));
                    il.append(new IAND());
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_ls", MATRIX_SIGNATURE)));
                    il.append(new GETFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m21", "I")));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHL());
                    il.append(new IOR());
                    break;
                case R_L22L23:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_ls", MATRIX_SIGNATURE)));
                    il.append(new GETFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m22", "I")));
                    il.append(new PUSH(cp, 0xffff));
                    il.append(new IAND());
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_ls", MATRIX_SIGNATURE)));
                    il.append(new GETFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m23", "I")));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHL());
                    il.append(new IOR());
                    break;
                case R_L31L32:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_ls", MATRIX_SIGNATURE)));
                    il.append(new GETFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m31", "I")));
                    il.append(new PUSH(cp, 0xffff));
                    il.append(new IAND());
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_ls", MATRIX_SIGNATURE)));
                    il.append(new GETFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m32", "I")));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHL());
                    il.append(new IOR());
                    break;
                case R_L33:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_ls", MATRIX_SIGNATURE)));
                    il.append(new GETFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m13", "I")));
                    il.append(new PUSH(cp, 0xffff));
                    il.append(new IAND());
                    break;
                case R_RBK:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_rbk", "I")));
                    break;
                case R_GBK:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_gbk", "I")));
                    break;
                case R_BBK:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_bbk", "I")));
                    break;
                case R_LR1LR2:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_lc", MATRIX_SIGNATURE)));
                    il.append(new GETFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m11", "I")));
                    il.append(new PUSH(cp, 0xffff));
                    il.append(new IAND());
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_lc", MATRIX_SIGNATURE)));
                    il.append(new GETFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m12", "I")));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHL());
                    il.append(new IOR());
                    break;
                case R_LR3LG1:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_lc", MATRIX_SIGNATURE)));
                    il.append(new GETFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m13", "I")));
                    il.append(new PUSH(cp, 0xffff));
                    il.append(new IAND());
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_lc", MATRIX_SIGNATURE)));
                    il.append(new GETFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m21", "I")));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHL());
                    il.append(new IOR());
                    break;
                case R_LG2LG3:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_lc", MATRIX_SIGNATURE)));
                    il.append(new GETFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m22", "I")));
                    il.append(new PUSH(cp, 0xffff));
                    il.append(new IAND());
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_lc", MATRIX_SIGNATURE)));
                    il.append(new GETFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m23", "I")));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHL());
                    il.append(new IOR());
                    break;
                case R_LB1LB2:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_lc", MATRIX_SIGNATURE)));
                    il.append(new GETFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m31", "I")));
                    il.append(new PUSH(cp, 0xffff));
                    il.append(new IAND());
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_lc", MATRIX_SIGNATURE)));
                    il.append(new GETFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m32", "I")));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHL());
                    il.append(new IOR());
                    break;
                case R_LB3:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_lc", MATRIX_SIGNATURE)));
                    il.append(new GETFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m33", "I")));
                    il.append(new PUSH(cp, 0xffff));
                    il.append(new IAND());
                    break;
                case R_RFC:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_rfc", "I")));
                    break;
                case R_GFC:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_gfc", "I")));
                    break;
                case R_BFC:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_bfc", "I")));
                    break;
                case R_OFX:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_ofx", "I")));
                    break;
                case R_OFY:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_ofy", "I")));
                    break;
                case R_H:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_h", "I")));
                    break;
                case R_DQA:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_dqa", "I")));
                    break;
                case R_DQB:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_dqb", "I")));
                    break;
                case R_ZSF3:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_zsf3", "I")));
                    break;
                case R_ZSF4:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_zsf4", "I")));
                    break;
                case R_FLAG:
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_flag", "I")));
                    break;
            }
        }
    }

    private static void emitWriteReg(InstructionList il, CompilationContext context, int reg) {
        ConstantPoolGen cp = context.getConstantPoolGen();
        int temp = context.getTempLocal(0);

        if (reg < 32) {
            switch (reg) {
                case R_VXY0:
                    il.append(new ISTORE(temp));
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_v0", VECTOR_SIGNATURE)));
                    il.append(new ILOAD(temp));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHL());
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHR());
                    il.append(new PUTFIELD(context.getConstantPoolGen().addFieldref(VECTOR_CLASS, "x", "I")));
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_v0", VECTOR_SIGNATURE)));
                    il.append(new ILOAD(temp));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHR());
                    il.append(new PUTFIELD(context.getConstantPoolGen().addFieldref(VECTOR_CLASS, "y", "I")));
                    break;
                case R_VZ0:
                    il.append(new ISTORE(temp));
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_v0", VECTOR_SIGNATURE)));
                    il.append(new ILOAD(temp));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHL());
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHR());
                    il.append(new PUTFIELD(context.getConstantPoolGen().addFieldref(VECTOR_CLASS, "z", "I")));
                    break;
                case R_VXY1:
                    il.append(new ISTORE(temp));
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_v1", VECTOR_SIGNATURE)));
                    il.append(new ILOAD(temp));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHL());
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHR());
                    il.append(new PUTFIELD(context.getConstantPoolGen().addFieldref(VECTOR_CLASS, "x", "I")));
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_v1", VECTOR_SIGNATURE)));
                    il.append(new ILOAD(temp));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHR());
                    il.append(new PUTFIELD(context.getConstantPoolGen().addFieldref(VECTOR_CLASS, "y", "I")));
                    break;
                case R_VZ1:
                    il.append(new ISTORE(temp));
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_v1", VECTOR_SIGNATURE)));
                    il.append(new ILOAD(temp));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHL());
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHR());
                    il.append(new PUTFIELD(context.getConstantPoolGen().addFieldref(VECTOR_CLASS, "z", "I")));
                    break;
                case R_VXY2:
                    il.append(new ISTORE(temp));
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_v2", VECTOR_SIGNATURE)));
                    il.append(new ILOAD(temp));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHL());
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHR());
                    il.append(new PUTFIELD(context.getConstantPoolGen().addFieldref(VECTOR_CLASS, "x", "I")));
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_v2", VECTOR_SIGNATURE)));
                    il.append(new ILOAD(temp));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHR());
                    il.append(new PUTFIELD(context.getConstantPoolGen().addFieldref(VECTOR_CLASS, "y", "I")));
                    break;
                case R_VZ2:
                    il.append(new ISTORE(temp));
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_v2", VECTOR_SIGNATURE)));
                    il.append(new ILOAD(temp));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHL());
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHR());
                    il.append(new PUTFIELD(context.getConstantPoolGen().addFieldref(VECTOR_CLASS, "z", "I")));
                    break;
                case R_RGB:
                    il.append(new PUTSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_rgb", "I")));
                    break;
                case R_OTZ:
                    il.append(new PUTSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_otz", "I")));
                    break;
                case R_IR0:
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHL());
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHR());
                    il.append(new PUTSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_ir0", "I")));
                    break;
                case R_IR1:
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHL());
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHR());
                    il.append(new PUTSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_ir1", "I")));
                    break;
                case R_IR2:
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHL());
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHR());
                    il.append(new PUTSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_ir2", "I")));
                    break;
                case R_IR3:
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHL());
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHR());
                    il.append(new PUTSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_ir3", "I")));
                    break;
                case R_SXY0:
                    il.append(new ISTORE(temp));
                    il.append(new ILOAD(temp));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHL());
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHR());
                    il.append(new PUTSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_sx0", "I")));
                    il.append(new ILOAD(temp));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHR());
                    il.append(new PUTSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_sy0", "I")));
                    break;
                case R_SXY1:
                    il.append(new ISTORE(temp));
                    il.append(new ILOAD(temp));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHL());
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHR());
                    il.append(new PUTSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_sx1", "I")));
                    il.append(new ILOAD(temp));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHR());
                    il.append(new PUTSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_sy1", "I")));
                    break;
                case R_SXY2:
                    il.append(new ISTORE(temp));
                    il.append(new ILOAD(temp));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHL());
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHR());
                    il.append(new PUTSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_sx2", "I")));
                    il.append(new ILOAD(temp));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHR());
                    il.append(new PUTSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_sy2", "I")));
                    break;
                    //        case R_SXYP:
                    //            //TODO: not certain this is 100% correct - but seems to work for Tony Hawk's (which is the only thing I've seen so far which uses it...)
                    //           reg_sx0 = reg_sx1; reg_sx1 = reg_sx2; reg_sx2 = (value<<16)>>16;
                    //          reg_sy0 = reg_sy1; reg_sy1 = reg_sy2; reg_sy2 = value>>16;
                    //         break;
                case R_SZX:
                    il.append(new PUSH(cp, 0xffff));
                    il.append(new IAND());
                    il.append(new PUTSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_szx", "I")));
                    break;
                case R_SZ0:
                    il.append(new PUSH(cp, 0xffff));
                    il.append(new IAND());
                    il.append(new PUTSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_sz0", "I")));
                    break;
                case R_SZ1:
                    il.append(new PUSH(cp, 0xffff));
                    il.append(new IAND());
                    il.append(new PUTSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_sz1", "I")));
                    break;
                case R_SZ2:
                    il.append(new PUSH(cp, 0xffff));
                    il.append(new IAND());
                    il.append(new PUTSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_sz2", "I")));
                    break;
                case R_RGB0:
                    il.append(new PUTSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_rgb0", "I")));
                    break;
                case R_RGB1:
                    il.append(new PUTSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_rgb1", "I")));
                    break;
                case R_RGB2:
                    il.append(new PUTSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_rgb2", "I")));
                    break;
                case R_RES1:
                    il.append(new PUTSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_res1", "I")));
                    break;
                case R_MAC0:
                    il.append(new PUTSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_mac0", "I")));
                    break;
                case R_MAC1:
                    il.append(new PUTSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_mac1", "I")));
                    break;
                case R_MAC2:
                    il.append(new PUTSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_mac2", "I")));
                    break;
                case R_MAC3:
                    il.append(new PUTSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_mac3", "I")));
                    break;
                case R_IRGB:
                    // todo check this
                    il.append(new PUTSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_irgb", "I")));
                    break;
                case R_ORGB:
                    // todo check this
                    il.append(new PUTSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_orgb", "I")));
                    break;
                    //            case R_LZCS:
                    //                {
                    //                    reg_lzcs = value;
                    //                   int mask = 0x80000000;
                    //                  int comp = value&0x80000000;
                    //                 int bits;
                    //
                    //              for (bits=0;bits<32;bits++) {
                    //                 if ((value&mask)!=comp)
                    //                      break;
                    //                 mask >>= 1;
                    //                comp >>= 1;
                    //           }
                    //          reg_lzcr = bits;
                    //         //Console.println("LZCS "+MiscUtil.toHex( reg_lzcs, 8)+" "+bits);
                    //         break;
                    //     }
                case R_LZCR:
                    // todo check this
                    il.append(new PUTSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_lzcr", "I")));
                    break;
                default:
                    il.append(new ISTORE(temp));
                    il.append(new PUSH(cp, reg));
                    il.append(new ILOAD(temp));
                    il.append(new INVOKESTATIC(cp.addMethodref(CLASS, "writeRegister", "(II)V")));
                    break;
            }
        } else {
            switch (reg) {
                case R_R11R12:
                    il.append(new ISTORE(temp));
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_rot", MATRIX_SIGNATURE)));
                    il.append(new ILOAD(temp));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHL());
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHR());
                    il.append(new PUTFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m11", "I")));
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_rot", MATRIX_SIGNATURE)));
                    il.append(new ILOAD(temp));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHR());
                    il.append(new PUTFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m12", "I")));
                    break;
                case R_R13R21:
                    il.append(new ISTORE(temp));
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_rot", MATRIX_SIGNATURE)));
                    il.append(new ILOAD(temp));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHL());
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHR());
                    il.append(new PUTFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m13", "I")));
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_rot", MATRIX_SIGNATURE)));
                    il.append(new ILOAD(temp));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHR());
                    il.append(new PUTFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m21", "I")));
                    break;
                case R_R22R23:
                    il.append(new ISTORE(temp));
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_rot", MATRIX_SIGNATURE)));
                    il.append(new ILOAD(temp));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHL());
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHR());
                    il.append(new PUTFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m22", "I")));
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_rot", MATRIX_SIGNATURE)));
                    il.append(new ILOAD(temp));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHR());
                    il.append(new PUTFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m23", "I")));
                    break;
                case R_R31R32:
                    il.append(new ISTORE(temp));
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_rot", MATRIX_SIGNATURE)));
                    il.append(new ILOAD(temp));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHL());
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHR());
                    il.append(new PUTFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m31", "I")));
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_rot", MATRIX_SIGNATURE)));
                    il.append(new ILOAD(temp));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHR());
                    il.append(new PUTFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m32", "I")));
                    break;
                case R_R33:
                    il.append(new ISTORE(temp));
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_rot", MATRIX_SIGNATURE)));
                    il.append(new ILOAD(temp));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHL());
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHR());
                    il.append(new PUTFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m33", "I")));
                    break;
                case R_TRX:
                    il.append(new PUTSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_trx", "I")));
                    break;
                case R_TRY:
                    il.append(new PUTSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_try", "I")));
                    break;
                case R_TRZ:
                    il.append(new PUTSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_trz", "I")));
                    break;
                case R_L11L12:
                    il.append(new ISTORE(temp));
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_ls", MATRIX_SIGNATURE)));
                    il.append(new ILOAD(temp));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHL());
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHR());
                    il.append(new PUTFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m11", "I")));
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_ls", MATRIX_SIGNATURE)));
                    il.append(new ILOAD(temp));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHR());
                    il.append(new PUTFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m12", "I")));
                    break;
                case R_L13L21:
                    il.append(new ISTORE(temp));
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_ls", MATRIX_SIGNATURE)));
                    il.append(new ILOAD(temp));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHL());
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHR());
                    il.append(new PUTFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m13", "I")));
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_ls", MATRIX_SIGNATURE)));
                    il.append(new ILOAD(temp));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHR());
                    il.append(new PUTFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m21", "I")));
                    break;
                case R_L22L23:
                    il.append(new ISTORE(temp));
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_ls", MATRIX_SIGNATURE)));
                    il.append(new ILOAD(temp));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHL());
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHR());
                    il.append(new PUTFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m22", "I")));
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_ls", MATRIX_SIGNATURE)));
                    il.append(new ILOAD(temp));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHR());
                    il.append(new PUTFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m23", "I")));
                    break;
                case R_L31L32:
                    il.append(new ISTORE(temp));
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_ls", MATRIX_SIGNATURE)));
                    il.append(new ILOAD(temp));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHL());
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHR());
                    il.append(new PUTFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m31", "I")));
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_ls", MATRIX_SIGNATURE)));
                    il.append(new ILOAD(temp));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHR());
                    il.append(new PUTFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m32", "I")));
                    break;
                case R_L33:
                    il.append(new ISTORE(temp));
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_ls", MATRIX_SIGNATURE)));
                    il.append(new ILOAD(temp));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHL());
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHR());
                    il.append(new PUTFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m33", "I")));
                    break;
                case R_RBK:
                    il.append(new PUTSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_rbk", "I")));
                    break;
                case R_GBK:
                    il.append(new PUTSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_gbk", "I")));
                    break;
                case R_BBK:
                    il.append(new PUTSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_bbk", "I")));
                    break;
                case R_LR1LR2:
                    il.append(new ISTORE(temp));
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_lc", MATRIX_SIGNATURE)));
                    il.append(new ILOAD(temp));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHL());
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHR());
                    il.append(new PUTFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m11", "I")));
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_lc", MATRIX_SIGNATURE)));
                    il.append(new ILOAD(temp));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHR());
                    il.append(new PUTFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m12", "I")));
                    break;
                case R_LR3LG1:
                    il.append(new ISTORE(temp));
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_lc", MATRIX_SIGNATURE)));
                    il.append(new ILOAD(temp));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHL());
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHR());
                    il.append(new PUTFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m13", "I")));
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_lc", MATRIX_SIGNATURE)));
                    il.append(new ILOAD(temp));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHR());
                    il.append(new PUTFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m21", "I")));
                    break;
                case R_LG2LG3:
                    il.append(new ISTORE(temp));
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_lc", MATRIX_SIGNATURE)));
                    il.append(new ILOAD(temp));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHL());
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHR());
                    il.append(new PUTFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m22", "I")));
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_lc", MATRIX_SIGNATURE)));
                    il.append(new ILOAD(temp));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHR());
                    il.append(new PUTFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m23", "I")));
                    break;
                case R_LB1LB2:
                    il.append(new ISTORE(temp));
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_lc", MATRIX_SIGNATURE)));
                    il.append(new ILOAD(temp));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHL());
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHR());
                    il.append(new PUTFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m31", "I")));
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_lc", MATRIX_SIGNATURE)));
                    il.append(new ILOAD(temp));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHR());
                    il.append(new PUTFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m32", "I")));
                    break;
                case R_LB3:
                    il.append(new ISTORE(temp));
                    il.append(new GETSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_lc", MATRIX_SIGNATURE)));
                    il.append(new ILOAD(temp));
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHL());
                    il.append(new PUSH(cp, 16));
                    il.append(new ISHR());
                    il.append(new PUTFIELD(context.getConstantPoolGen().addFieldref(MATRIX_CLASS, "m33", "I")));
                    break;
                case R_RFC:
                    il.append(new PUTSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_rfc", "I")));
                    break;
                case R_GFC:
                    il.append(new PUTSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_gfc", "I")));
                    break;
                case R_BFC:
                    il.append(new PUTSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_bfc", "I")));
                    break;
                case R_OFX:
                    il.append(new PUTSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_ofx", "I")));
                    break;
                case R_OFY:
                    il.append(new PUTSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_ofy", "I")));
                    break;
                case R_H:
                    il.append(new PUTSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_h", "I")));
                    break;
                case R_DQA:
                    il.append(new PUTSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_dqa", "I")));
                    break;
                case R_DQB:
                    il.append(new PUTSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_dqb", "I")));
                    break;
                case R_ZSF3:
                    il.append(new PUTSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_zsf3", "I")));
                    break;
                case R_ZSF4:
                    il.append(new PUTSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_zsf4", "I")));
                    break;
                case R_FLAG:
                    il.append(new PUTSTATIC(context.getConstantPoolGen().addFieldref(CLASS, "reg_flag", "I")));
                    break;
                default:
                    il.append(new ISTORE(temp));
                    il.append(new PUSH(cp, reg));
                    il.append(new ILOAD(temp));
                    il.append(new INVOKESTATIC(cp.addMethodref(CLASS, "writeRegister", "(II)V")));
                    break;
            }
        }
    }

    public void init() {
        super.init();
        CoreComponentConnections.INSTRUCTION_PROVIDERS.add(this);
    }

    @Override
    public void resolveConnections() {
        super.resolveConnections();
        addressSpace = CoreComponentConnections.ADDRESS_SPACE.resolve();
        r3000 = CoreComponentConnections.R3000.resolve();
        r3000regs = r3000.getInterpreterRegs();
    }

    public void addInstructions(InstructionRegistrar registrar) {
        log.info("Adding COP2 instructions...");
        i_mfc2 = new CPUInstruction("mfc2", GTE.class, 0, CPUInstruction.FLAG_WRITES_RT) {
            public void compile(CompilationContext context, int address, int ci, InstructionList il) {
                int rd = R3000.Util.bits_rd(ci);
                int rt = R3000.Util.bits_rt(ci);

                if (rt != 0) {
                    emitReadReg(il, context, rd);
                    context.emitSetReg(il, rt);
                }
            }
        };
        i_cfc2 = new CPUInstruction("cfc2", GTE.class, 0, CPUInstruction.FLAG_WRITES_RT) {
            public void compile(CompilationContext context, int address, int ci, InstructionList il) {
                int rd = R3000.Util.bits_rd(ci);
                int rt = R3000.Util.bits_rt(ci);

                if (rt != 0) {
                    emitReadReg(il, context, rd + 32);
                    context.emitSetReg(il, rt);
                }
            }
        };

        i_mtc2 = new CPUInstruction("mtc2", GTE.class, 0, CPUInstruction.FLAG_READS_RT) {
            public void compile(CompilationContext context, int address, int ci, InstructionList il) {
                int rt = R3000.Util.bits_rt(ci);
                int rd = R3000.Util.bits_rd(ci);

                ConstantPoolGen cp = context.getConstantPoolGen();

                if (0 != (context.getConstantRegs() & (1 << rt))) {
                    il.append(new PUSH(cp, context.getRegValue(rt)));
                } else {
                    context.emitGetReg(il, rt);
                }

                emitWriteReg(il, context, rd);
            }
        };

        i_ctc2 = new CPUInstruction("ctc2", GTE.class, 0, CPUInstruction.FLAG_READS_RT) {
            public void compile(CompilationContext context, int address, int ci, InstructionList il) {
                int rt = R3000.Util.bits_rt(ci);
                int rd = R3000.Util.bits_rd(ci);

                ConstantPoolGen cp = context.getConstantPoolGen();

                if (0 != (context.getConstantRegs() & (1 << rt))) {
                    il.append(new PUSH(cp, context.getRegValue(rt)));
                } else {
                    context.emitGetReg(il, rt);
                }
                emitWriteReg(il, context, rd + 32);
            }
        };
        i_lwc2 = new CPUInstruction("lwc2", GTE.class, 0, CPUInstruction.FLAG_READS_RS | CPUInstruction.FLAG_MEM32) {
            public void compile(CompilationContext context, int address, int ci, InstructionList il) {
                int base = R3000.Util.bits_rs(ci);
                int rt = R3000.Util.bits_rt(ci);
                int offset = R3000.Util.sign_extend(ci);

                if (0 != (context.getConstantRegs() & (1 << base))) {
                    context.emitReadMem32(il, context.getRegValue(base) + offset, false);
                } else {
                    context.emitReadMem32(il, base, offset, false);
                }
                emitWriteReg(il, context, rt);
            }
        };
        i_swc2 = new CPUInstruction("swc2", GTE.class, 0, CPUInstruction.FLAG_READS_RS | CPUInstruction.FLAG_MEM32) {
            public void compile(CompilationContext context, int address, int ci, InstructionList il) {
                int base = R3000.Util.bits_rs(ci);
                int rt = R3000.Util.bits_rt(ci);
                int offset = R3000.Util.sign_extend(ci);

                InstructionList il2 = new InstructionList();
                emitReadReg(il2, context, rt);
                if (0 != (context.getConstantRegs() & (1 << base))) {
                    context.emitWriteMem32(il, context.getRegValue(base) + offset, il2, false);
                } else {
                    context.emitWriteMem32(il, base, offset, il2, false);
                }
                il2.dispose();
            }
        };
        i_rtpt = new CPUInstruction("rtpt", GTE.class, 0, 0);
        i_rtps = new CPUInstruction("rtps", GTE.class, 0, 0);
        i_mvmva = new CPUInstruction("mvmva", GTE.class, 0, 0);
        i_op = new CPUInstruction("op", GTE.class, 0, 0);
        i_avsz3 = new CPUInstruction("avsz3", GTE.class, 0, 0);
        i_avsz4 = new CPUInstruction("avsz4", GTE.class, 0, 0);
        i_nclip = new CPUInstruction("nclip", GTE.class, 0, 0);
        i_ncct = new CPUInstruction("ncct", GTE.class, 0, 0);
        i_gpf = new CPUInstruction("gpf", GTE.class, 0, 0);
        i_dcpl = new CPUInstruction("dcpl", GTE.class, 0, 0);
        i_dpcs = new CPUInstruction("dpcs", GTE.class, 0, 0);
        i_intpl = new CPUInstruction("intpl", GTE.class, 0, 0);
        i_sqr = new CPUInstruction("sqr", GTE.class, 0, 0);
        i_ncs = new CPUInstruction("ncs", GTE.class, 0, 0);
        i_nct = new CPUInstruction("nct", GTE.class, 0, 0);
        i_ncds = new CPUInstruction("ncds", GTE.class, 0, 0);
        i_ncdt = new CPUInstruction("ncdt", GTE.class, 0, 0);
        i_dpct = new CPUInstruction("dpct", GTE.class, 0, 0);
        i_nccs = new CPUInstruction("nccs", GTE.class, 0, 0);
        i_cdp = new CPUInstruction("cdp", GTE.class, 0, 0);
        i_cc = new CPUInstruction("cc", GTE.class, 0, 0);
        i_gpl = new CPUInstruction("gpl", GTE.class, 0, 0);
        CPUInstruction i_cop2 = new CPUInstruction("cop2", GTE.class, 0, 0) {
            public CPUInstruction subDecode(int ci) {
                switch (R3000.Util.bits_rs(ci)) {
                    case 0:
                        return i_mfc2;
                    case 2:
                        return i_cfc2;
                    case 4:
                        return i_mtc2;
                    case 6:
                        return i_ctc2;
                    case 1:
                    case 3:
                    case 5:
                    case 7:
                        return r3000.getInvalidInstruction();
                }
                switch (ci & 0x3f) {
                    case 0x01:
                        return i_rtps;
                    case 0x06:
                        return i_nclip;
                    case 0x0c:
                        return i_op;
                    case 0x10:
                        return i_dpcs;
                    case 0x11:
                        return i_intpl;
                    case 0x12:
                        return i_mvmva;
                    case 0x13:
                        return i_ncds;
                    case 0x14:
                        return i_cdp;
                    case 0x16:
                        return i_ncdt;
                    case 0x1b:
                        return i_nccs;
                    case 0x1c:
                        return i_cc;
                    case 0x1e:
                        return i_ncs;
                    case 0x20:
                        return i_nct;
                    case 0x28:
                        return i_sqr;
                    case 0x29:
                        return i_dcpl;
                    case 0x2a:
                        return i_dpct;
                    case 0x2d:
                        return i_avsz3;
                    case 0x2e:
                        return i_avsz4;
                    case 0x30:
                        return i_rtpt;
                    case 0x3d:
                        return i_gpf;
                    case 0x3e:
                        return i_gpl;
                    case 0x3f:
                        return i_ncct;
                }
                return r3000.getInvalidInstruction();
            }
        };

        registrar.setInstruction(18, i_cop2);
        registrar.setInstruction(50, i_lwc2);
        registrar.setInstruction(58, i_swc2);
    }

    private static CPUInstruction i_mfc2;
    private static CPUInstruction i_cfc2;
    private static CPUInstruction i_mtc2;
    private static CPUInstruction i_ctc2;
    private static CPUInstruction i_lwc2;
    private static CPUInstruction i_swc2;
    private static CPUInstruction i_rtpt;
    private static CPUInstruction i_rtps;
    private static CPUInstruction i_mvmva;
    private static CPUInstruction i_op;
    private static CPUInstruction i_avsz3;
    private static CPUInstruction i_avsz4;
    private static CPUInstruction i_nclip;
    private static CPUInstruction i_ncct;
    private static CPUInstruction i_gpf;
    private static CPUInstruction i_dcpl;
    private static CPUInstruction i_dpcs;
    private static CPUInstruction i_intpl;
    private static CPUInstruction i_sqr;
    private static CPUInstruction i_ncs;
    private static CPUInstruction i_nct;
    private static CPUInstruction i_ncds;
    private static CPUInstruction i_ncdt;
    private static CPUInstruction i_dpct;
    private static CPUInstruction i_nccs;
    private static CPUInstruction i_cdp;
    private static CPUInstruction i_cc;
    private static CPUInstruction i_gpl;

    public static void interpret_mfc2(final int ci) {
        int rt = R3000.Util.bits_rt(ci);
        int rd = R3000.Util.bits_rd(ci);
        int value = readRegister(rd);
        if (rt != 0)
            r3000regs[rt] = value;
    }

    public static int readRegister(final int reg) {
        int value;
        switch (reg) {
            case R_VXY0:
                value = (reg_v0.x & 0xffff) | ((reg_v0.y << 16) & 0xffff0000);
                break;
            case R_VZ0:
                value = reg_v0.z;
                break;
            case R_VXY1:
                value = (reg_v1.x & 0xffff) | ((reg_v1.y << 16) & 0xffff0000);
                break;
            case R_VZ1:
                value = reg_v1.z;
                break;
            case R_VXY2:
                value = (reg_v2.x & 0xffff) | ((reg_v2.y << 16) & 0xffff0000);
                break;
            case R_VZ2:
                value = reg_v2.z;
                break;
            case R_RGB:
                value = reg_rgb;
                break;
            case R_OTZ:
                value = reg_otz;
                break;
            case R_IR0:
                value = reg_ir0;
                break;
            case R_IR1:
                value = reg_ir1;
                break;
            case R_IR2:
                value = reg_ir2;
                break;
            case R_IR3:
                value = reg_ir3;
                break;
            case R_SXY0:
                value = (reg_sx0 & 0xffff) | ((reg_sy0 << 16) & 0xffff0000);
                break;
            case R_SXY1:
                value = (reg_sx1 & 0xffff) | ((reg_sy1 << 16) & 0xffff0000);
                break;
            case R_SXY2:
                value = (reg_sx2 & 0xffff) | ((reg_sy2 << 16) & 0xffff0000);
                break;
            case R_SXYP:
                value = (reg_sxp & 0xffff) | ((reg_syp << 16) & 0xffff0000);
                break;
            case R_SZX:
                value = reg_szx;
                break;
            case R_SZ0:
                value = reg_sz0;
                break;
            case R_SZ1:
                value = reg_sz1;
                break;
            case R_SZ2:
                value = reg_sz2;
                break;
            case R_RGB0:
                value = reg_rgb0;
                break;
            case R_RGB1:
                value = reg_rgb1;
                break;
            case R_RGB2:
                value = reg_rgb2;
                break;
            case R_RES1:
                value = reg_res1;
                break;
            case R_MAC0:
                value = reg_mac0;
                break;
            case R_MAC1:
                value = reg_mac1;
                break;
            case R_MAC2:
                value = reg_mac2;
                break;
            case R_MAC3:
                value = reg_mac3;
                break;
            case R_IRGB:
                // todo check this
                value = reg_irgb;
                break;
            case R_ORGB:
                // todo check this
                value = reg_orgb;
                break;
            case R_LZCS:
                value = reg_lzcs;
                break;
            case R_LZCR:
                value = reg_lzcr;
                break;
            case R_R11R12:
                value = (reg_rot.m11 & 0xffff) | ((reg_rot.m12 << 16) & 0xffff0000);
                break;
            case R_R13R21:
                value = (reg_rot.m13 & 0xffff) | ((reg_rot.m21 << 16) & 0xffff0000);
                break;
            case R_R22R23:
                value = (reg_rot.m22 & 0xffff) | ((reg_rot.m23 << 16) & 0xffff0000);
                break;
            case R_R31R32:
                value = (reg_rot.m31 & 0xffff) | ((reg_rot.m32 << 16) & 0xffff0000);
                break;
            case R_R33:
                value = (reg_rot.m33 & 0xffff);
                break;
            case R_TRX:
                value = reg_trx;
                break;
            case R_TRY:
                value = reg_try;
                break;
            case R_TRZ:
                value = reg_trz;
                break;
            case R_L11L12:
                value = (reg_ls.m11 & 0xffff) | ((reg_ls.m12 << 16) & 0xffff0000);
                break;
            case R_L13L21:
                value = (reg_ls.m13 & 0xffff) | ((reg_ls.m21 << 16) & 0xffff0000);
                break;
            case R_L22L23:
                value = (reg_ls.m21 & 0xffff) | ((reg_ls.m23 << 16) & 0xffff0000);
                break;
            case R_L31L32:
                value = (reg_ls.m31 & 0xffff) | ((reg_ls.m32 << 16) & 0xffff0000);
                break;
            case R_L33:
                value = (reg_ls.m33 & 0xffff);
                break;
            case R_RBK:
                value = reg_rbk;
                break;
            case R_GBK:
                value = reg_gbk;
                break;
            case R_BBK:
                value = reg_bbk;
                break;
            case R_LR1LR2:
                value = (reg_lc.m11 & 0xffff) | ((reg_lc.m12 << 16) & 0xffff0000);
                break;
            case R_LR3LG1:
                value = (reg_lc.m13 & 0xffff) | ((reg_lc.m21 << 16) & 0xffff0000);
                break;
            case R_LG2LG3:
                value = (reg_lc.m22 & 0xffff) | ((reg_lc.m23 << 16) & 0xffff0000);
                break;
            case R_LB1LB2:
                value = (reg_lc.m31 & 0xffff) | ((reg_lc.m32 << 16) & 0xffff0000);
                break;
            case R_LB3:
                value = (reg_lc.m33 & 0xffff);
                break;
            case R_RFC:
                value = reg_rfc;
                break;
            case R_GFC:
                value = reg_gfc;
                break;
            case R_BFC:
                value = reg_bfc;
                break;
            case R_OFX:
                value = reg_ofx;
                break;
            case R_OFY:
                value = reg_ofy;
                break;
            case R_H:
                value = reg_h;
                break;
            case R_DQA:
                value = reg_dqa;
                break;
            case R_DQB:
                value = reg_dqb;
                break;
            case R_ZSF3:
                value = reg_zsf3;
                break;
            case R_ZSF4:
                value = reg_zsf4;
                break;
            case R_FLAG:
                value = reg_flag;
                break;
            default:
                value = 0;
        }
        return value;
    }

    public static void interpret_cfc2(final int ci) {
        int rt = R3000.Util.bits_rt(ci);
        int rd = R3000.Util.bits_rd(ci);
        int value = readRegister(rd + 32);
        if (rt != 0)
            r3000regs[rt] = value;
    }

    public static void writeRegister(int reg, int value) {
        switch (reg) {
            case R_VXY0:
                reg_v0.x = (value << 16) >> 16;
                reg_v0.y = value >> 16;
                break;
            case R_VZ0:
                reg_v0.z = (value << 16) >> 16;
                break;
            case R_VXY1:
                reg_v1.x = (value << 16) >> 16;
                reg_v1.y = value >> 16;
                break;
            case R_VZ1:
                reg_v1.z = (value << 16) >> 16;
                break;
            case R_VXY2:
                reg_v2.x = (value << 16) >> 16;
                reg_v2.y = value >> 16;
                break;
            case R_VZ2:
                reg_v2.z = (value << 16) >> 16;
                break;
            case R_RGB:
                reg_rgb = value;
                break;
            case R_OTZ:
                reg_otz = value;
                break;
            case R_IR0:
                reg_ir0 = (value << 16) >> 16;
                break;
            case R_IR1:
                reg_ir1 = (value << 16) >> 16;
                break;
            case R_IR2:
                reg_ir2 = (value << 16) >> 16;
                break;
            case R_IR3:
                reg_ir3 = (value << 16) >> 16;
                break;
            case R_SXY0:
                reg_sx0 = (value << 16) >> 16;
                reg_sy0 = value >> 16;
                break;
            case R_SXY1:
                reg_sx1 = (value << 16) >> 16;
                reg_sy1 = value >> 16;
                break;
            case R_SXY2:
                reg_sx2 = (value << 16) >> 16;
                reg_sy2 = value >> 16;
                break;
            case R_SXYP:
                //TODO: not certain this is 100% correct - but seems to work for Tony Hawk's (which is the only thing I've seen so far which uses it...)
                reg_sx0 = reg_sx1;
                reg_sx1 = reg_sx2;
                reg_sx2 = (value << 16) >> 16;
                reg_sy0 = reg_sy1;
                reg_sy1 = reg_sy2;
                reg_sy2 = value >> 16;
                break;
            case R_SZX:
                reg_szx = value & 0xffff;
                break;
            case R_SZ0:
                reg_sz0 = value & 0xffff;
                break;
            case R_SZ1:
                reg_sz1 = value & 0xffff;
                break;
            case R_SZ2:
                reg_sz2 = value & 0xffff;
                break;
            case R_RGB0:
                reg_rgb0 = value;
                break;
            case R_RGB1:
                reg_rgb1 = value;
                break;
            case R_RGB2:
                reg_rgb2 = value;
                break;
            case R_RES1:
                reg_res1 = value;
                break;
            case R_MAC0:
                reg_mac0 = value;
                break;
            case R_MAC1:
                reg_mac1 = value;
                break;
            case R_MAC2:
                reg_mac2 = value;
                break;
            case R_MAC3:
                reg_mac3 = value;
                break;
            case R_IRGB:
                // todo check this
                reg_irgb = value;
                break;
            case R_ORGB:
                // todo check this
                reg_orgb = value;
                break;
            case R_LZCS: {
                reg_lzcs = value;
                int mask = 0x80000000;
                int comp = value & 0x80000000;
                int bits;

                for (bits = 0; bits < 32; bits++) {
                    if ((value & mask) != comp)
                        break;
                    mask >>= 1;
                    comp >>= 1;
                }
                reg_lzcr = bits;
                //Console.println("LZCS "+MiscUtil.toHex( reg_lzcs, 8)+" "+bits);
                break;
            }
            case R_LZCR:
                // todo check this
                reg_lzcr = value;
                break;
            case R_R11R12:
                reg_rot.m11 = (value << 16) >> 16;
                reg_rot.m12 = value >> 16;
                break;
            case R_R13R21:
                reg_rot.m13 = (value << 16) >> 16;
                reg_rot.m21 = value >> 16;
                break;
            case R_R22R23:
                reg_rot.m22 = (value << 16) >> 16;
                reg_rot.m23 = value >> 16;
                break;
            case R_R31R32:
                reg_rot.m31 = (value << 16) >> 16;
                reg_rot.m32 = value >> 16;
                break;
            case R_R33:
                reg_rot.m33 = (value << 16) >> 16;
                break;
            case R_TRX:
                reg_trx = value;
                break;
            case R_TRY:
                reg_try = value;
                break;
            case R_TRZ:
                reg_trz = value;
                break;
            case R_L11L12:
                reg_ls.m11 = (value << 16) >> 16;
                reg_ls.m12 = value >> 16;
                break;
            case R_L13L21:
                reg_ls.m13 = (value << 16) >> 16;
                reg_ls.m21 = value >> 16;
                break;
            case R_L22L23:
                reg_ls.m22 = (value << 16) >> 16;
                reg_ls.m23 = value >> 16;
                break;
            case R_L31L32:
                reg_ls.m31 = (value << 16) >> 16;
                reg_ls.m32 = value >> 16;
                break;
            case R_L33:
                reg_ls.m33 = (value << 16) >> 16;
                break;
            case R_RBK:
                reg_rbk = value;
                break;
            case R_GBK:
                reg_gbk = value;
                break;
            case R_BBK:
                reg_bbk = value;
                break;
            case R_LR1LR2:
                reg_lc.m11 = (value << 16) >> 16;
                reg_lc.m12 = value >> 16;
                break;
            case R_LR3LG1:
                reg_lc.m13 = (value << 16) >> 16;
                reg_lc.m21 = value >> 16;
                break;
            case R_LG2LG3:
                reg_lc.m22 = (value << 16) >> 16;
                reg_lc.m23 = value >> 16;
                break;
            case R_LB1LB2:
                reg_lc.m31 = (value << 16) >> 16;
                reg_lc.m32 = value >> 16;
                break;
            case R_LB3:
                reg_lc.m33 = (value << 16) >> 16;
                break;
            case R_RFC:
                reg_rfc = value;
                break;
            case R_GFC:
                reg_gfc = value;
                break;
            case R_BFC:
                reg_bfc = value;
                break;
            case R_OFX:
                reg_ofx = value;
                break;
            case R_OFY:
                reg_ofy = value;
                break;
            case R_H:
                reg_h = value;
                break;
            case R_DQA:
                reg_dqa = value;
                break;
            case R_DQB:
                reg_dqb = value;
                break;
            case R_ZSF3:
                reg_zsf3 = value;
                break;
            case R_ZSF4:
                reg_zsf4 = value;
                break;
            case R_FLAG:
                reg_flag = value;
                break;
        }
    }

    public static void interpret_mtc2(final int ci) {
        int rt = R3000.Util.bits_rt(ci);
        int rd = R3000.Util.bits_rd(ci);
        int value = r3000regs[rt];

        writeRegister(rd, value);
    }

    public static void interpret_ctc2(final int ci) {
        int rt = R3000.Util.bits_rt(ci);
        int rd = R3000.Util.bits_rd(ci);
        int value = r3000regs[rt];

        writeRegister(rd + 32, value);
    }

    public static void interpret_cop2(final int ci) {
        switch (R3000.Util.bits_rs(ci)) {
            case 0:
                GTE.interpret_mfc2(ci);
                return;
            case 2:
                GTE.interpret_cfc2(ci);
                return;
            case 4:
                GTE.interpret_mtc2(ci);
                return;
            case 6:
                GTE.interpret_ctc2(ci);
                return;
            case 1:
            case 3:
            case 5:
            case 7:
                break;
            default:
                switch (ci & 0x3f) {
                    case 0x01:
                        GTE.interpret_rtps(ci);
                        return;
                    case 0x06:
                        GTE.interpret_nclip(ci);
                        return;
                    case 0x0c:
                        GTE.interpret_op(ci);
                        return;
                    case 0x10:
                        GTE.interpret_dpcs(ci);
                        return;
                    case 0x11:
                        GTE.interpret_intpl(ci);
                        return;
                    case 0x12:
                        GTE.interpret_mvmva(ci);
                        return;
                    case 0x13:
                        GTE.interpret_ncds(ci);
                        return;
                    case 0x14:
                        GTE.interpret_cdp(ci);
                        return;
                    case 0x16:
                        GTE.interpret_ncdt(ci);
                        return;
                    case 0x1b:
                        GTE.interpret_nccs(ci);
                        return;
                    case 0x1c:
                        GTE.interpret_cc(ci);
                        return;
                    case 0x1e:
                        GTE.interpret_ncs(ci);
                        return;
                    case 0x20:
                        GTE.interpret_nct(ci);
                        return;
                    case 0x28:
                        GTE.interpret_sqr(ci);
                        return;
                    case 0x29:
                        GTE.interpret_dcpl(ci);
                        return;
                    case 0x2a:
                        GTE.interpret_dpct(ci);
                        return;
                    case 0x2d:
                        GTE.interpret_avsz3(ci);
                        return;
                    case 0x2e:
                        GTE.interpret_avsz4(ci);
                        return;
                    case 0x30:
                        GTE.interpret_rtpt(ci);
                        return;
                    case 0x3d:
                        GTE.interpret_gpf(ci);
                        return;
                    case 0x3e:
                        GTE.interpret_gpl(ci);
                        return;
                    case 0x3f:
                        GTE.interpret_ncct(ci);
                        return;
                }
        }
        CoreComponentConnections.SCP.resolve().signalReservedInstructionException();
    }

    public static void interpret_lwc2(final int ci) {
        int base = R3000.Util.bits_rs(ci);
        int rt = R3000.Util.bits_rt(ci);
        int offset = (ci << 16) >> 16;
        int addr = r3000regs[base] + offset;
        addressSpace.tagAddressAccessRead32(r3000.getPC(), addr);
        writeRegister(rt, addressSpace.read32(addr));
    }

    public static void interpret_swc2(final int ci) {
        int base = R3000.Util.bits_rs(ci);
        int rt = R3000.Util.bits_rt(ci);
        int offset = (ci << 16) >> 16;
        int addr = r3000regs[base] + offset;
        addressSpace.tagAddressAccessWrite(r3000.getPC(), addr);
        addressSpace.write32(addr, readRegister(rt));
    }

    public static void interpret_rtpt(final int ci) {
        // UNTESTED
        reg_flag = 0;

        reg_szx = reg_sz2;

        long vx = reg_v0.x;
        long vy = reg_v0.y;
        long vz = reg_v0.z;

        long ssx = reg_rot.m11 * vx + reg_rot.m12 * vy + reg_rot.m13 * vz + (((long) reg_trx) << 12);
        long ssy = reg_rot.m21 * vx + reg_rot.m22 * vy + reg_rot.m23 * vz + (((long) reg_try) << 12);
        long ssz = reg_rot.m31 * vx + reg_rot.m32 * vy + reg_rot.m33 * vz + (((long) reg_trz) << 12);

        reg_mac1 = A1(ssx);
        reg_mac2 = A2(ssy);
        reg_mac3 = A3(ssz);

        reg_ir1 = LiB1_0(reg_mac1);
        reg_ir2 = LiB2_0(reg_mac2);

        reg_sz0 = LiD(reg_mac3);

        if (reg_sz0 != 0) {
            long hsz = ((long) (reg_h & 0xffff)) << 16;
            hsz /= reg_sz0;
            hsz = LiE((int) hsz);
            // [1,15,0] SX2= LG1[F[OFX + IR1*(H/SZ)]]                       [1,27,16]
            reg_sx0 = LiG1(LiF(reg_ofx + reg_ir1 * hsz));
            // [1,15,0] SY2= LG2[F[OFY + IR2*(H/SZ)]]                       [1,27,16]
            reg_sy0 = LiG2(LiF(reg_ofy + reg_ir2 * hsz));
            // [1,31,0] MAC0= F[DQB + DQA * (H/SZ)]                           [1,19,24]
            reg_mac0 = LiF(reg_dqb + ((reg_dqa * hsz) >> 16));
        } else {
            LiE(0x7fffffff);
            reg_sx0 = LiG1(LiF(reg_ofx + SIGNED_BIG(reg_ir1)));
            reg_sy0 = LiG2(LiF(reg_ofy + SIGNED_BIG(reg_ir2)));
            reg_mac0 = LiF(reg_dqb + SIGNED_BIG(reg_dqa));
        }

        // [1,15,0] IR0= LH[MAC0]                                       [1,31,0]
        reg_ir0 = LiH(reg_mac0);

        // ---------------------------------------------------

        vx = reg_v1.x;
        vy = reg_v1.y;
        vz = reg_v1.z;

        ssx = reg_rot.m11 * vx + reg_rot.m12 * vy + reg_rot.m13 * vz + (((long) reg_trx) << 12);
        ssy = reg_rot.m21 * vx + reg_rot.m22 * vy + reg_rot.m23 * vz + (((long) reg_try) << 12);
        ssz = reg_rot.m31 * vx + reg_rot.m32 * vy + reg_rot.m33 * vz + (((long) reg_trz) << 12);

        reg_mac1 = A1(ssx);
        reg_mac2 = A2(ssy);
        reg_mac3 = A3(ssz);

        reg_ir1 = LiB1_0(reg_mac1);
        reg_ir2 = LiB2_0(reg_mac2);

        reg_sz1 = LiD(reg_mac3);

        if (reg_sz1 != 0) {
            long hsz = ((long) (reg_h & 0xffff)) << 16;
            hsz /= reg_sz1;
            hsz = LiE((int) hsz);
            // [1,15,0] SX2= LG1[F[OFX + IR1*(H/SZ)]]                       [1,27,16]
            reg_sx1 = LiG1(LiF(reg_ofx + reg_ir1 * hsz));
            // [1,15,0] SY2= LG2[F[OFY + IR2*(H/SZ)]]                       [1,27,16]
            reg_sy1 = LiG2(LiF(reg_ofy + reg_ir2 * hsz));
            // [1,31,0] MAC0= F[DQB + DQA * (H/SZ)]                           [1,19,24]
            reg_mac0 = LiF(reg_dqb + ((reg_dqa * hsz) >> 16));
        } else {
            LiE(0x7fffffff);
            reg_sx1 = LiG1(LiF(reg_ofx + SIGNED_BIG(reg_ir1)));
            reg_sy1 = LiG2(LiF(reg_ofy + SIGNED_BIG(reg_ir2)));
            reg_mac0 = LiF(reg_dqb + SIGNED_BIG(reg_dqa));
        }

        // [1,15,0] IR0= LH[MAC0]                                       [1,31,0]
        reg_ir0 = LiH(reg_mac0);

        // ---------------------------------------------------

        vx = reg_v2.x;
        vy = reg_v2.y;
        vz = reg_v2.z;

        ssx = reg_rot.m11 * vx + reg_rot.m12 * vy + reg_rot.m13 * vz + (((long) reg_trx) << 12);
        ssy = reg_rot.m21 * vx + reg_rot.m22 * vy + reg_rot.m23 * vz + (((long) reg_try) << 12);
        ssz = reg_rot.m31 * vx + reg_rot.m32 * vy + reg_rot.m33 * vz + (((long) reg_trz) << 12);

        reg_mac1 = A1(ssx);
        reg_mac2 = A2(ssy);
        reg_mac3 = A3(ssz);

        reg_ir1 = LiB1_0(reg_mac1);
        reg_ir2 = LiB2_0(reg_mac2);
        reg_ir3 = LiB3_0(reg_mac3);

        reg_sz2 = LiD(reg_mac3);

        if (reg_sz2 != 0) {
            long hsz = ((long) (reg_h & 0xffff)) << 16;
            hsz /= reg_sz2;
            hsz = LiE((int) hsz);
            // [1,15,0] SX2= LG1[F[OFX + IR1*(H/SZ)]]                       [1,27,16]
            reg_sx2 = LiG1(LiF(reg_ofx + reg_ir1 * hsz));
            // [1,15,0] SY2= LG2[F[OFY + IR2*(H/SZ)]]                       [1,27,16]
            reg_sy2 = LiG2(LiF(reg_ofy + reg_ir2 * hsz));
            // [1,31,0] MAC0= F[DQB + DQA * (H/SZ)]                           [1,19,24]
            reg_mac0 = LiF(reg_dqb + ((reg_dqa * hsz) >> 16));
        } else {
            LiE(0x7fffffff);
            reg_sx2 = LiG1(LiF(reg_ofx + SIGNED_BIG(reg_ir1)));
            reg_sy2 = LiG2(LiF(reg_ofy + SIGNED_BIG(reg_ir2)));
            reg_mac0 = LiF(reg_dqb + SIGNED_BIG(reg_dqa));
        }

        // [1,15,0] IR0= LH[MAC0]                                       [1,31,0]
        reg_ir0 = LiH(reg_mac0);
        //if (true) throw new IllegalStateException("pah");
    }

    public static void interpret_rtps(final int ci) {
        reg_flag = 0;

        long vx = reg_v0.x;
        long vy = reg_v0.y;
        long vz = reg_v0.z;

        long ssx = reg_rot.m11 * vx + reg_rot.m12 * vy + reg_rot.m13 * vz + (((long) reg_trx) << 12);
        long ssy = reg_rot.m21 * vx + reg_rot.m22 * vy + reg_rot.m23 * vz + (((long) reg_try) << 12);
        long ssz = reg_rot.m31 * vx + reg_rot.m32 * vy + reg_rot.m33 * vz + (((long) reg_trz) << 12);

        reg_mac1 = A1(ssx);
        reg_mac2 = A2(ssy);
        reg_mac3 = A3(ssz);

        reg_ir1 = LiB1_0(reg_mac1);
        reg_ir2 = LiB2_0(reg_mac2);
        reg_ir3 = LiB3_0(reg_mac3);

        reg_szx = reg_sz0;
        reg_sz0 = reg_sz1;
        reg_sz1 = reg_sz2;

        reg_sz2 = LiD(reg_mac3);

        reg_sx0 = reg_sx1;
        reg_sy0 = reg_sy1;
        reg_sx1 = reg_sx2;
        reg_sy1 = reg_sy2;

        if (reg_sz2 != 0) {
            long hsz = ((long) (reg_h & 0xffff)) << 16;
            hsz /= reg_sz2;
            hsz = LiE((int) hsz);
            // [1,15,0] SX2= LG1[F[OFX + IR1*(H/SZ)]]                       [1,27,16]
            reg_sx2 = LiG1(LiF(reg_ofx + reg_ir1 * hsz));
            // [1,15,0] SY2= LG2[F[OFY + IR2*(H/SZ)]]                       [1,27,16]
            reg_sy2 = LiG2(LiF(reg_ofy + reg_ir2 * hsz));
            // [1,31,0] MAC0= F[DQB + DQA * (H/SZ)]                           [1,19,24]
            reg_mac0 = LiF(reg_dqb + ((reg_dqa * hsz) >> 16));
        } else {
            LiE(0x7fffffff);
            reg_sx2 = LiG1(LiF(reg_ofx + SIGNED_BIG(reg_ir1)));
            reg_sy2 = LiG2(LiF(reg_ofy + SIGNED_BIG(reg_ir2)));
            reg_mac0 = LiF(reg_dqb + SIGNED_BIG(reg_dqa));
        }

        // [1,15,0] IR0= LH[MAC0]                                       [1,31,0]
        reg_ir0 = LiH(reg_mac0);
    }

    public static long SIGNED_BIG(int src) {
        if (src == 0)
            return 0;
        if (src > 0)
            return 0x10000000000000L;
        return -0x10000000000000L;
    }

    public static void interpret_mvmva(final int ci) {
// NOTE: int64/A1,A2,A3 can only happen with IR I think

        reg_flag = 0;

        Matrix matrix;
        switch (ci & GTE_MX_MASK) {
            case GTE_MX_LIGHT:
                matrix = reg_ls;
                break;
            case GTE_MX_COLOR:
                matrix = reg_lc;
                break;
            default:
                matrix = reg_rot;
                break;
        }

        long vx;
        long vy;
        long vz;
        switch (ci & GTE_V_MASK) {
            case GTE_V_IR:
                // is this sign correct?
                vx = reg_ir1;
                vy = reg_ir2;
                vz = reg_ir3;
                //System.out.println("Unsure mvma with IR input!");
                break;
            case GTE_V_V2:
                vx = reg_v2.x;
                vy = reg_v2.y;
                vz = reg_v2.z;
                break;
            case GTE_V_V1:
                vx = reg_v1.x;
                vy = reg_v1.y;
                vz = reg_v1.z;
                break;
            default:
                vx = reg_v0.x;
                vy = reg_v0.y;
                vz = reg_v0.z;
                break;
        }

        // v values s15.0 or s31.0 (s19.12 in SF case?)

        long ssx = matrix.m11 * vx + matrix.m12 * vy + matrix.m13 * vz;
        long ssy = matrix.m21 * vx + matrix.m22 * vy + matrix.m23 * vz;
        long ssz = matrix.m31 * vx + matrix.m32 * vy + matrix.m33 * vz;

        if (0 != (ci & GTE_SF_MASK)) {
            ssx >>= 12;
            ssy >>= 12;
            ssz >>= 12;
        }

        // ss values are up to about s36.12
        switch (ci & GTE_CV_MASK) {
            case GTE_CV_TR:
                ssx += reg_trx;
                ssy += reg_try;
                ssz += reg_trz;
                break;
            case GTE_CV_BK:
                ssx += reg_rbk;
                ssy += reg_gbk;
                ssz += reg_bbk;
                break;
            case GTE_CV_FC:
                ssx += reg_rfc;
                ssy += reg_gfc;
                ssz += reg_bfc;
                break;
            default:
                break;
        }

        reg_mac1 = A1(ssx << 12);
        reg_mac2 = A2(ssy << 12);
        reg_mac3 = A3(ssz << 12);

        if (0 != (ci & GTE_LM_MASK)) {
            reg_ir1 = LiB1_1(reg_mac1);
            reg_ir2 = LiB2_1(reg_mac2);
            reg_ir3 = LiB3_1(reg_mac3);
        } else {
            reg_ir1 = LiB1_0(reg_mac1);
            reg_ir2 = LiB2_0(reg_mac2);
            reg_ir3 = LiB3_0(reg_mac3);
        }
    }

    // is this correct?
    public static int LiB1_0(int src) {
        if (src >= 0x8000) {
            reg_flag |= FLAG_B1;
            return 0x7fff;
        } else if (src < -0x8000) {
            reg_flag |= FLAG_B1;
            return -0x8000;
        }
        return src;
    }


    // is this correct?
    public static int LiB1_1(int src) {
        if (src >= 0x8000) {
            reg_flag |= FLAG_B1;
            return 0x7fff;
        } else if (src < 0) {
            reg_flag |= FLAG_B1;
            return 0;
        }
        return src;
    }

    public static int LiB2_0(int src) {
        if (src >= 0x8000) {
            reg_flag |= FLAG_B2;
            return 0x7fff;
        } else if (src < -0x8000) {
            reg_flag |= FLAG_B2;
            return -0x8000;
        }
        return src;
    }

    public static int LiB2_1(int src) {
        if (src >= 0x8000) {
            reg_flag |= FLAG_B2;
            return 0x7fff;
        } else if (src < 0) {
            reg_flag |= FLAG_B2;
            return 0;
        }
        return src;
    }

    public static int LiB3_0(int src) {
        if (src >= 0x8000) {
            reg_flag |= FLAG_B3;
            return 0x7fff;
        } else if (src < -0x8000) {
            reg_flag |= FLAG_B3;
            return -0x8000;
        }
        return src;
    }

    public static int LiB3_1(int src) {
        if (src >= 0x8000) {
            reg_flag |= FLAG_B3;
            return 0x7fff;
        } else if (src < 0) {
            reg_flag |= FLAG_B3;
            return 0;
        }
        return src;
    }

    public static int LiC1(int src) {
        if (src < 0) {
            reg_flag |= FLAG_C1;
            return 0;
        } else if (src > 0xff) {
            reg_flag |= FLAG_C1;
            return 0xff;
        }
        return src;
    }

    public static int LiC2(int src) {
        if (src < 0) {
            reg_flag |= FLAG_C2;
            return 0;
        } else if (src > 0xff) {
            reg_flag |= FLAG_C2;
            return 0xff;
        }
        return src;
    }

    public static int LiC3(int src) {
        if (src < 0) {
            reg_flag |= FLAG_C3;
            return 0;
        } else if (src > 0xff) {
            reg_flag |= FLAG_C3;
            return 0xff;
        }
        return src;
    }

    public static int LiD(int src) {
        if (src < 0) {
            reg_flag |= FLAG_D;
            return 0;
        } else if (src >= 0x8000) {
            reg_flag |= FLAG_D;
            return 0x7fff;
        }
        return src;
    }

    private static int LiE(int src) {
        if (src >= 0x20000) {
            reg_flag |= FLAG_E;
        }
        return src;
    }

    private static int LiF(long src) {
        if (src >= BIT47) {
            reg_flag |= FLAG_FP;
            return 0x7fffffff;
        } else if (src <= -BIT47) {
            reg_flag |= FLAG_FN;
            return 0x80000000;
        }
        return (int) (src >> 16);
    }

    // not sure if this is 15 bit or 9 bit limit
    public static int LiG1(int src) {
        if (src >= 0x800) {
            reg_flag |= FLAG_G1;
            return 0x7ff;
        } else if (src < -0x800) {
            reg_flag |= FLAG_G1;
            return -0x800;
        }
        return src;
    }

    public static int LiG2(int src) {
        if (src >= 0x800) {
            reg_flag |= FLAG_G2;
            return 0x7ff;
        } else if (src < -0x800) {
            reg_flag |= FLAG_G2;
            return -0x800;
        }
        return src;
    }

    public static int LiH(int src) {
        if (src >= 0x1000) {
            reg_flag |= FLAG_H;
            return 0xfff;
        } else if (src < 0) {
            reg_flag |= FLAG_H;
            return 0;
        }
        return src;
    }

    private static int A1(long val) {
        if (val >= BIT44) {
            reg_flag |= FLAG_A1P;
        } else if (val <= -BIT44) {
            reg_flag |= FLAG_A1N;
        }
        return (int) (val >> 12);
    }

    private static int A2(long val) {
        if (val >= BIT44) {
            reg_flag |= FLAG_A2P;
        } else if (val <= -BIT44) {
            reg_flag |= FLAG_A2N;
        }
        return (int) (val >> 12);
    }

    private static int A3(long val) {
        if (val >= BIT44) {
            reg_flag |= FLAG_A3P;
        } else if (val <= -BIT44) {
            reg_flag |= FLAG_A3N;
        }
        return (int) (val >> 12);
    }

    public static void interpret_op(final int ci) {
        reg_flag = 0;

        long a1 = reg_rot.m11;
        long a2 = reg_rot.m22;
        long a3 = reg_rot.m33;

        long ss1 = a2 * reg_ir3 - a3 * reg_ir2;
        long ss2 = a3 * reg_ir1 - a1 * reg_ir3;
        long ss3 = a1 * reg_ir2 - a2 * reg_ir1;

        if (0 == (ci & GTE_SF_MASK)) {
            ss1 <<= 12;
            ss2 <<= 12;
            ss3 <<= 12;
        }

        reg_mac1 = A1(ss1);
        reg_mac2 = A2(ss2);
        reg_mac3 = A3(ss3);
        reg_ir1 = LiB1_0(reg_mac1);
        reg_ir2 = LiB2_0(reg_mac2);
        reg_ir3 = LiB3_0(reg_mac3);
    }

    public static void interpret_avsz3(final int ci) {
        reg_flag = 0;
        // UNTESTED

        // [1,31,0] MAC0=F[ZSF3*SZ0 + ZSF3*SZ1 + ZSF3*SZ2]     [1,31,12]
        // [0,16,0] OTZ=LD[MAC0]                                        [1,31,0]
        // skipping F for now
        // TODO figure out why LiF is 16 for now when it should be twelve

        reg_mac0 = (reg_zsf3 * (reg_sz0 + reg_sz1 + reg_sz2)) >> 12;
        reg_otz = LiD(reg_mac0);
    }

    public static void interpret_avsz4(final int ci) {
        reg_flag = 0;
        // UNTESTED

        // [1,31,0] MAC0=F[ZSF4*SZ0 + ZSF4*SZ1 + ZSF4*SZ2 + ZSF4*SZ3]     [1,31,12]
        // [0,16,0] OTZ=LD[MAC0]                                        [1,31,0]

        // skipping F for now
        // TODO figure out why LiF is 16 for now when it should be twelve

        reg_mac0 = (reg_zsf4 * (reg_szx + reg_sz0 + reg_sz1 + reg_sz2)) >> 12;
        reg_otz = LiD(reg_mac0);
    }

    public static void interpret_nclip(final int ci) {
        /*
              NOTE: I don't think nclip should clear the FLAG register.

              In Tomb Raider, there is code which looks something like this:
                  RTPT
                  NCLIP
                  f = gte->R_FLAG
                  if (f & 0x7fc7e000) goto skip_polygon;

               Since the RTPT can only set SX0,SX1,SX2,SY0,SY1,SY2 to values in the
              range -0x800 to 0x800, there is no way NCLIP's calculation can overflow.
               Hence, if NCLIP clears the FLAG register there is no way the branch
              in the above code can ever be taken.
               Whereas, if NCLIP _doesn't_ clear the FLAG, the above code actually
              makes sense.
          */
        //	reg_flag = 0;

        // [1,31,0] MAC0 = F[SX0*SY1+SX1*SY2+SX2*SY0-SX0*SY2-SX1*SY0-SX2*SY1] [1,43,0]
        // @@ not too worried about liF() here...
        reg_mac0 = reg_sx0 * reg_sy1 + reg_sx1 * reg_sy2 + reg_sx2 * reg_sy0 -
                reg_sx0 * reg_sy2 - reg_sx1 * reg_sy0 - reg_sx2 * reg_sy1;
    }

    public static void interpret_ncct(final int ci) {
        // untested
        reg_flag = 0;

        int chi = reg_rgb & 0xff000000;
        int r = (reg_rgb & 0xff) << 4;
        int g = (reg_rgb & 0xff00) >> 4;
        int b = (reg_rgb & 0xff0000) >> 12;

        long m11 = reg_ls.m11;
        long m12 = reg_ls.m12;
        long m13 = reg_ls.m13;
        long m21 = reg_ls.m21;
        long m22 = reg_ls.m22;
        long m23 = reg_ls.m23;
        long m31 = reg_ls.m31;
        long m32 = reg_ls.m32;
        long m33 = reg_ls.m33;

        long ss1 = m11 * reg_v0.x + m12 * reg_v0.y + m13 * reg_v0.z;
        long ss2 = m21 * reg_v0.x + m22 * reg_v0.y + m23 * reg_v0.z;
        long ss3 = m31 * reg_v0.x + m32 * reg_v0.y + m33 * reg_v0.z;

        int mac1 = A1(ss1);
        int mac2 = A2(ss2);
        int mac3 = A3(ss3);

        int ir1 = LiB1_1(mac1);
        int ir2 = LiB2_1(mac2);
        int ir3 = LiB3_1(mac3);

        long c11 = reg_lc.m11;
        long c12 = reg_lc.m12;
        long c13 = reg_lc.m13;
        long c21 = reg_lc.m21;
        long c22 = reg_lc.m22;
        long c23 = reg_lc.m23;
        long c31 = reg_lc.m31;
        long c32 = reg_lc.m32;
        long c33 = reg_lc.m33;

        long bkr = reg_rbk;
        long bkg = reg_gbk;
        long bkb = reg_bbk;

        ss1 = c11 * ir1 + c12 * ir2 + c13 * ir3 + (bkr << 12);
        ss2 = c21 * ir1 + c22 * ir2 + c23 * ir3 + (bkg << 12);
        ss3 = c31 * ir1 + c32 * ir2 + c33 * ir3 + (bkb << 12);

        mac1 = A1(ss1);
        mac2 = A2(ss2);
        mac3 = A3(ss3);

        ir1 = LiB1_1(mac1);
        ir2 = LiB2_1(mac2);
        ir3 = LiB3_1(mac3);

        mac1 = A1(r * ir1);
        mac2 = A2(g * ir2);
        mac3 = A3(b * ir3);

        int rr = LiC1(mac1 >> 4);
        int gg = LiC2(mac2 >> 4);
        int bb = LiC3(mac3 >> 4);
        reg_rgb0 = rr | (gg << 8) | (bb << 16) | chi;

        // 2
        ss1 = m11 * reg_v1.x + m12 * reg_v1.y + m13 * reg_v1.z;
        ss2 = m21 * reg_v1.x + m22 * reg_v1.y + m23 * reg_v1.z;
        ss3 = m31 * reg_v1.x + m32 * reg_v1.y + m33 * reg_v1.z;

        mac1 = A1(ss1);
        mac2 = A2(ss2);
        mac3 = A3(ss3);

        ir1 = LiB1_1(mac1);
        ir2 = LiB2_1(mac2);
        ir3 = LiB3_1(mac3);

        ss1 = c11 * ir1 + c12 * ir2 + c13 * ir3 + (bkr << 12);
        ss2 = c21 * ir1 + c22 * ir2 + c23 * ir3 + (bkg << 12);
        ss3 = c31 * ir1 + c32 * ir2 + c33 * ir3 + (bkb << 12);

        mac1 = A1(ss1);
        mac2 = A2(ss2);
        mac3 = A3(ss3);

        ir1 = LiB1_1(mac1);
        ir2 = LiB2_1(mac2);
        ir3 = LiB3_1(mac3);

        mac1 = A1(r * ir1);
        mac2 = A2(g * ir2);
        mac3 = A3(b * ir3);

        // gcs 011802 added >>4
        rr = LiC1(mac1 >> 4);
        gg = LiC2(mac2 >> 4);
        bb = LiC3(mac3 >> 4);
        reg_rgb1 = rr | (gg << 8) | (bb << 16) | chi;

        // 3
        ss1 = m11 * reg_v2.x + m12 * reg_v2.y + m13 * reg_v2.z;
        ss2 = m21 * reg_v2.x + m22 * reg_v2.y + m23 * reg_v2.z;
        ss3 = m31 * reg_v2.x + m32 * reg_v2.y + m33 * reg_v2.z;

        mac1 = A1(ss1);
        mac2 = A2(ss2);
        mac3 = A3(ss3);

        ir1 = LiB1_1(mac1);
        ir2 = LiB2_1(mac2);
        ir3 = LiB3_1(mac3);

        ss1 = c11 * ir1 + c12 * ir2 + c13 * ir3 + (bkr << 12);
        ss2 = c21 * ir1 + c22 * ir2 + c23 * ir3 + (bkg << 12);
        ss3 = c31 * ir1 + c32 * ir2 + c33 * ir3 + (bkb << 12);

        mac1 = A1(ss1);
        mac2 = A2(ss2);
        mac3 = A3(ss3);

        ir1 = LiB1_1(mac1);
        ir2 = LiB2_1(mac2);
        ir3 = LiB3_1(mac3);

        reg_mac1 = A1(r * ir1);
        reg_mac2 = A2(g * ir2);
        reg_mac3 = A3(b * ir3);
        reg_ir1 = LiB1_1(reg_mac1);
        reg_ir2 = LiB2_1(reg_mac2);
        reg_ir3 = LiB3_1(reg_mac3);

        // gcs 011802 added >>4
        rr = LiC1(reg_mac1 >> 4);
        gg = LiC2(reg_mac2 >> 4);
        bb = LiC3(reg_mac3 >> 4);
        reg_rgb2 = rr | (gg << 8) | (bb << 16) | chi;
    }

    public static void interpret_gpf(final int ci) {
        reg_flag = 0;
        //   MAC1=A1[IR0 * IR1]
        //   MAC2=A2[IR0 * IR2]
        //   MAC3=A3[IR0 * IR3]
        //   IR1=LB1[MAC1]
        //   IR2=LB2[MAC2]
        //   IR3=LB3[MAC3]
        //[0,8,0]   Cd0<-Cd1<-Cd2<- CODE
        //[0,8,0]   R0<-R1<-R2<- LC1[MAC1]
        //[0,8,0]   G0<-G1<-G2<- LC2[MAC2]
        //[0,8,0]   B0<-B1<-B2<- LC3[MAC3]

        long m = reg_ir0;
        if (0 != (ci & GTE_SF_MASK)) {
            reg_mac1 = A1(m * reg_ir1);
            reg_mac2 = A2(m * reg_ir2);
            reg_mac3 = A3(m * reg_ir3);
        } else {
            reg_mac1 = A1((m * reg_ir1) << 12);
            reg_mac2 = A2((m * reg_ir2) << 12);
            reg_mac3 = A3((m * reg_ir3) << 12);
        }
        reg_ir1 = LiB1_0(reg_mac1);
        reg_ir2 = LiB2_0(reg_mac2);
        reg_ir3 = LiB3_0(reg_mac3);
        int rr = LiC1(reg_mac1 >> 4);
        int gg = LiC2(reg_mac2 >> 4);
        int bb = LiC3(reg_mac3 >> 4);
        reg_rgb0 = reg_rgb1;
        reg_rgb1 = reg_rgb2;
        reg_rgb2 = (reg_rgb & 0xff000000) | rr | (gg << 8) | (bb << 16);
    }

    public static void interpret_dcpl(final int ci) {
        if (true) throw new IllegalStateException("GTE UNIMPLEMENTED: DCPL");
    }

    public static void interpret_dpcs(final int ci) {
        reg_flag = 0;
        int chi = reg_rgb & 0xff000000;
        int r = (reg_rgb & 0xff) << 4;
        int g = (reg_rgb & 0xff00) >> 4;
        int b = (reg_rgb & 0xff0000) >> 12;

        // TODO - is this B1 all the way correct?
        reg_mac1 = A1((r << 12) + reg_ir0 * LiB1_0(reg_rfc - r));
        reg_mac2 = A2((g << 12) + reg_ir0 * LiB1_0(reg_gfc - g));
        reg_mac3 = A3((b << 12) + reg_ir0 * LiB1_0(reg_bfc - b));

        reg_ir1 = LiB1_0(reg_mac1);
        reg_ir2 = LiB2_0(reg_mac2);
        reg_ir3 = LiB3_0(reg_mac3);

        int rr = LiC1(reg_mac1 >> 4);
        int gg = LiC2(reg_mac2 >> 4);
        int bb = LiC3(reg_mac3 >> 4);
        reg_rgb0 = reg_rgb1;
        reg_rgb1 = reg_rgb2;
        reg_rgb2 = rr | (gg << 8) | (bb << 16) | chi;
    }

    public static void interpret_intpl(final int ci) {
        reg_flag = 0;
        int chi = reg_rgb & 0xff000000;

        long ir0 = reg_ir0;
        reg_mac1 = A1((reg_ir1 << 12) + ir0 * LiB1_0(reg_rfc - reg_ir1));
        reg_mac2 = A2((reg_ir2 << 12) + ir0 * LiB2_0(reg_gfc - reg_ir2));
        reg_mac3 = A3((reg_ir3 << 12) + ir0 * LiB3_0(reg_bfc - reg_ir3));
        reg_ir1 = LiB1_0(reg_mac1);
        reg_ir2 = LiB2_0(reg_mac2);
        reg_ir3 = LiB3_0(reg_mac3);

        int rr = LiC1(reg_mac1 >> 4);
        int gg = LiC2(reg_mac2 >> 4);
        int bb = LiC3(reg_mac3 >> 4);
        reg_rgb0 = reg_rgb1;
        reg_rgb1 = reg_rgb2;
        reg_rgb2 = rr | (gg << 8) | (bb << 16) | chi;
    }

    public static void interpret_sqr(final int ci) {
        reg_flag = 0;

        // [1,31,0] MAC1=A1[IR1*IR1]                     [1,43,0]
        // [1,31,0] MAC2=A2[IR2*IR2]                     [1,43,0]
        // [1,31,0] MAC3=A3[IR3*IR3]                     [1,43,0]
        // [1,15,0] IR1=LB1[MAC1]                      [1,31,0][lm=1]
        // [1,15,0] IR2=LB2[MAC2]                      [1,31,0][lm=1]
        // [1,15,0] IR3=LB3[MAC3]                      [1,31,0][lm=1]

        // A1,A2,A3 not possible... if IR inputs are 16 bit (well except for -32768!)
        int i1 = reg_ir1 * reg_ir1;
        int i2 = reg_ir2 * reg_ir2;
        int i3 = reg_ir3 * reg_ir3;
        if (0 != (ci & GTE_SF_MASK)) {
            i1 >>= 12;
            i2 >>= 12;
            i3 >>= 12;
        }
        reg_mac1 = i1;
        reg_mac2 = i2;
        reg_mac3 = i3;
        reg_ir1 = LiB1_1(i1);
        reg_ir2 = LiB1_1(i2);
        reg_ir3 = LiB1_1(i3);
    }

    public static void interpret_ncs(final int ci) {
        // test with ridge racer
        reg_flag = 0;
        int chi = reg_rgb & 0xff000000;

        long m11 = reg_ls.m11;
        long m12 = reg_ls.m12;
        long m13 = reg_ls.m13;
        long m21 = reg_ls.m21;
        long m22 = reg_ls.m22;
        long m23 = reg_ls.m23;
        long m31 = reg_ls.m31;
        long m32 = reg_ls.m32;
        long m33 = reg_ls.m33;

        int mac1 = A1(m11 * reg_v0.x + m12 * reg_v0.y + m13 * reg_v0.z);
        int mac2 = A2(m21 * reg_v0.x + m22 * reg_v0.y + m23 * reg_v0.z);
        int mac3 = A3(m31 * reg_v0.x + m32 * reg_v0.y + m33 * reg_v0.z);

        int ir1 = LiB1_1(mac1);
        int ir2 = LiB2_1(mac2);
        int ir3 = LiB3_1(mac3);

        long c11 = reg_lc.m11;
        long c12 = reg_lc.m12;
        long c13 = reg_lc.m13;
        long c21 = reg_lc.m21;
        long c22 = reg_lc.m22;
        long c23 = reg_lc.m23;
        long c31 = reg_lc.m31;
        long c32 = reg_lc.m32;
        long c33 = reg_lc.m33;

        long bkr = reg_rbk;
        long bkg = reg_gbk;
        long bkb = reg_bbk;

        reg_mac1 = A1(c11 * ir1 + c12 * ir2 + c13 * ir3 + (bkr << 12));
        reg_mac2 = A2(c21 * ir1 + c22 * ir2 + c23 * ir3 + (bkg << 12));
        reg_mac3 = A3(c31 * ir1 + c32 * ir2 + c33 * ir3 + (bkb << 12));

        reg_ir1 = LiB1_1(reg_mac1);
        reg_ir2 = LiB2_1(reg_mac2);
        reg_ir3 = LiB3_1(reg_mac3);

        int rr = LiC1(reg_mac1 >> 4);
        int gg = LiC2(reg_mac2 >> 4);
        int bb = LiC3(reg_mac3 >> 4);
        reg_rgb0 = reg_rgb1;
        reg_rgb1 = reg_rgb2;
        reg_rgb2 = rr | (gg << 8) | (bb << 16) | chi;
    }

    public static void interpret_nct(final int ci) {
        // test with ridge racer
        reg_flag = 0;
        int chi = reg_rgb & 0xff000000;

        long m11 = reg_ls.m11;
        long m12 = reg_ls.m12;
        long m13 = reg_ls.m13;
        long m21 = reg_ls.m21;
        long m22 = reg_ls.m22;
        long m23 = reg_ls.m23;
        long m31 = reg_ls.m31;
        long m32 = reg_ls.m32;
        long m33 = reg_ls.m33;

        int mac1 = A1(m11 * reg_v0.x + m12 * reg_v0.y + m13 * reg_v0.z);
        int mac2 = A2(m21 * reg_v0.x + m22 * reg_v0.y + m23 * reg_v0.z);
        int mac3 = A3(m31 * reg_v0.x + m32 * reg_v0.y + m33 * reg_v0.z);

        int ir1 = LiB1_1(mac1);
        int ir2 = LiB2_1(mac2);
        int ir3 = LiB3_1(mac3);

        long c11 = reg_lc.m11;
        long c12 = reg_lc.m12;
        long c13 = reg_lc.m13;
        long c21 = reg_lc.m21;
        long c22 = reg_lc.m22;
        long c23 = reg_lc.m23;
        long c31 = reg_lc.m31;
        long c32 = reg_lc.m32;
        long c33 = reg_lc.m33;

        long bkr = reg_rbk;
        long bkg = reg_gbk;
        long bkb = reg_bbk;

        mac1 = A1(c11 * ir1 + c12 * ir2 + c13 * ir3 + (bkr << 12));
        mac2 = A2(c21 * ir1 + c22 * ir2 + c23 * ir3 + (bkg << 12));
        mac3 = A3(c31 * ir1 + c32 * ir2 + c33 * ir3 + (bkb << 12));

        int rr = LiC1(mac1 >> 4);
        int gg = LiC2(mac2 >> 4);
        int bb = LiC3(mac3 >> 4);
        reg_rgb0 = rr | (gg << 8) | (bb << 16) | chi;

        // 2

        mac1 = A1(m11 * reg_v1.x + m12 * reg_v1.y + m13 * reg_v1.z);
        mac2 = A2(m21 * reg_v1.x + m22 * reg_v1.y + m23 * reg_v1.z);
        mac3 = A3(m31 * reg_v1.x + m32 * reg_v1.y + m33 * reg_v1.z);

        ir1 = LiB1_1(mac1);
        ir2 = LiB2_1(mac2);
        ir3 = LiB3_1(mac3);

        mac1 = A1(c11 * ir1 + c12 * ir2 + c13 * ir3 + (bkr << 12));
        mac2 = A2(c21 * ir1 + c22 * ir2 + c23 * ir3 + (bkg << 12));
        mac3 = A3(c31 * ir1 + c32 * ir2 + c33 * ir3 + (bkb << 12));

        rr = LiC1(mac1 >> 4);
        gg = LiC2(mac2 >> 4);
        bb = LiC3(mac3 >> 4);
        reg_rgb1 = rr | (gg << 8) | (bb << 16) | chi;

        // 3

        mac1 = A1(m11 * reg_v2.x + m12 * reg_v2.y + m13 * reg_v2.z);
        mac2 = A2(m21 * reg_v2.x + m22 * reg_v2.y + m23 * reg_v2.z);
        mac3 = A3(m31 * reg_v2.x + m32 * reg_v2.y + m33 * reg_v2.z);

        ir1 = LiB1_1(mac1);
        ir2 = LiB2_1(mac2);
        ir3 = LiB3_1(mac3);

        reg_mac1 = A1(c11 * ir1 + c12 * ir2 + c13 * ir3 + (bkr << 12));
        reg_mac2 = A2(c21 * ir1 + c22 * ir2 + c23 * ir3 + (bkg << 12));
        reg_mac3 = A3(c31 * ir1 + c32 * ir2 + c33 * ir3 + (bkb << 12));

        reg_ir1 = LiB1_1(reg_mac1);
        reg_ir2 = LiB2_1(reg_mac2);
        reg_ir3 = LiB3_1(reg_mac3);

        rr = LiC1(reg_mac1 >> 4);
        gg = LiC2(reg_mac2 >> 4);
        bb = LiC3(reg_mac3 >> 4);
        reg_rgb2 = rr | (gg << 8) | (bb << 16) | chi;
    }

    public static void interpret_ncds(final int ci) {
        reg_flag = 0;

        int chi = reg_rgb & 0xff000000;
        int r = (reg_rgb & 0xff) << 4;
        int g = (reg_rgb & 0xff00) >> 4;
        int b = (reg_rgb & 0xff0000) >> 12;

        // [1,19,12] MAC1=A1[L11*VX0 + L12*VY0 + L13*VZ0]                 [1,19,24]
        // [1,19,12] MAC2=A1[L21*VX0 + L22*VY0 + L23*VZ0]                 [1,19,24]
        // [1,19,12] MAC3=A1[L31*VX0 + L32*VY0 + L33*VZ0]                 [1,19,24]
        // [1,3,12]  IR1= LB1[MAC1]                                     [1,19,12][lm=1]
        // [1,3,12]  IR2= LB2[MAC2]                                     [1,19,12][lm=1]
        // [1,3,12]  IR3= LB3[MAC3]                                     [1,19,12][lm=1]

        long m11 = reg_ls.m11;
        long m12 = reg_ls.m12;
        long m13 = reg_ls.m13;
        long m21 = reg_ls.m21;
        long m22 = reg_ls.m22;
        long m23 = reg_ls.m23;
        long m31 = reg_ls.m31;
        long m32 = reg_ls.m32;
        long m33 = reg_ls.m33;

        int mac1 = A1(m11 * reg_v0.x + m12 * reg_v0.y + m13 * reg_v0.z);
        int mac2 = A2(m21 * reg_v0.x + m22 * reg_v0.y + m23 * reg_v0.z);
        int mac3 = A3(m31 * reg_v0.x + m32 * reg_v0.y + m33 * reg_v0.z);
        int ir1 = LiB1_1(mac1);
        int ir2 = LiB2_1(mac2);
        int ir3 = LiB3_1(mac3);

        // [1,19,12] MAC1=A1[RBK + LR1*IR1 + LR2*IR2 + LR3*IR3]           [1,19,24]
        // [1,19,12] MAC2=A1[GBK + LG1*IR1 + LG2*IR2 + LG3*IR3]           [1,19,24]
        // [1,19,12] MAC3=A1[BBK + LB1*IR1 + LB2*IR2 + LB3*IR3]           [1,19,24]
        // [1,3,12]  IR1= LB1[MAC1]                                     [1,19,12][lm=1]
        // [1,3,12]  IR2= LB2[MAC2]                                     [1,19,12][lm=1]
        // [1,3,12]  IR3= LB3[MAC3]                                     [1,19,12][lm=1]

        long c11 = reg_lc.m11;
        long c12 = reg_lc.m12;
        long c13 = reg_lc.m13;
        long c21 = reg_lc.m21;
        long c22 = reg_lc.m22;
        long c23 = reg_lc.m23;
        long c31 = reg_lc.m31;
        long c32 = reg_lc.m32;
        long c33 = reg_lc.m33;

        long bkr = reg_rbk;
        long bkg = reg_gbk;
        long bkb = reg_bbk;

        mac1 = A1(c11 * ir1 + c12 * ir2 + c13 * ir3 + (bkr << 12));
        mac2 = A2(c21 * ir1 + c22 * ir2 + c23 * ir3 + (bkg << 12));
        mac3 = A3(c31 * ir1 + c32 * ir2 + c33 * ir3 + (bkb << 12));

        ir1 = LiB1_1(mac1);
        ir2 = LiB2_1(mac2);
        ir3 = LiB3_1(mac3);

        // [1,27,4]  MAC1=A1[R*IR1 + IR0*(LB1[RFC-R*IR1])]              [1,27,16][lm=0]
        // [1,27,4]  MAC2=A1[G*IR2 + IR0*(LB2[GFC-G*IR2])]              [1,27,16][lm=0]
        // [1,27,4]  MAC3=A1[B*IR3 + IR0*(LB3[BFC-B*IR3])]              [1,27,16][lm=0]
        // [1,3,4]  IR1= LB1[MAC1]                                     [1,27,4][lm=1]
        // [1,3,4]  IR2= LB2[MAC2]                                     [1,27,4][lm=1]
        // [1,3,4]  IR3= LB3[MAC3]                                     [1,27,4][lm=1]
        long ir0 = reg_ir0;
        reg_mac1 = A1(r * ir1 + ((ir0 * LiB1_0((reg_rfc << 12) - r * ir1)) >> 12));
        reg_mac2 = A2(g * ir2 + ((ir0 * LiB2_0((reg_gfc << 12) - g * ir2)) >> 12));
        reg_mac3 = A3(b * ir3 + ((ir0 * LiB3_0((reg_bfc << 12) - b * ir3)) >> 12));

        reg_ir1 = LiB1_1(reg_mac1);
        reg_ir2 = LiB2_1(reg_mac2);
        reg_ir3 = LiB3_1(reg_mac3);

        // [0,8,0]   Cd0<-Cd1<-Cd2<- CODE
        // [0,8,0]   R0<-R1<-R2<- LC1[MAC1]                             [1,27,4]
        // [0,8,0]   G0<-G1<-G2<- LC2[MAC2]                             [1,27,4]
        // [0,8,0]   B0<-B1<-B2<- LC3[MAC3]                             [1,27,4]
        int rr = LiC1(reg_mac1 >> 4);
        int gg = LiC2(reg_mac2 >> 4);
        int bb = LiC3(reg_mac3 >> 4);
        reg_rgb0 = reg_rgb1;
        reg_rgb1 = reg_rgb2;
        reg_rgb2 = rr | (gg << 8) | (bb << 16) | chi;
    }

    public static void interpret_ncdt(final int ci) {
        reg_flag = 0;

        int chi = reg_rgb & 0xff000000;
        int r = (reg_rgb & 0xff) << 4;
        int g = (reg_rgb & 0xff00) >> 4;
        int b = (reg_rgb & 0xff0000) >> 12;

        // [1,19,12] MAC1=A1[L11*VX0 + L12*VY0 + L13*VZ0]                 [1,19,24]
        // [1,19,12] MAC2=A1[L21*VX0 + L22*VY0 + L23*VZ0]                 [1,19,24]
        // [1,19,12] MAC3=A1[L31*VX0 + L32*VY0 + L33*VZ0]                 [1,19,24]
        // [1,3,12]  IR1= LB1[MAC1]                                     [1,19,12][lm=1]
        // [1,3,12]  IR2= LB2[MAC2]                                     [1,19,12][lm=1]
        // [1,3,12]  IR3= LB3[MAC3]                                     [1,19,12][lm=1]

        long m11 = reg_ls.m11;
        long m12 = reg_ls.m12;
        long m13 = reg_ls.m13;
        long m21 = reg_ls.m21;
        long m22 = reg_ls.m22;
        long m23 = reg_ls.m23;
        long m31 = reg_ls.m31;
        long m32 = reg_ls.m32;
        long m33 = reg_ls.m33;

        int mac1 = A1(m11 * reg_v0.x + m12 * reg_v0.y + m13 * reg_v0.z);
        int mac2 = A2(m21 * reg_v0.x + m22 * reg_v0.y + m23 * reg_v0.z);
        int mac3 = A3(m31 * reg_v0.x + m32 * reg_v0.y + m33 * reg_v0.z);
        int ir1 = LiB1_1(mac1);
        int ir2 = LiB2_1(mac2);
        int ir3 = LiB3_1(mac3);

        // [1,19,12] MAC1=A1[RBK + LR1*IR1 + LR2*IR2 + LR3*IR3]           [1,19,24]
        // [1,19,12] MAC2=A1[GBK + LG1*IR1 + LG2*IR2 + LG3*IR3]           [1,19,24]
        // [1,19,12] MAC3=A1[BBK + LB1*IR1 + LB2*IR2 + LB3*IR3]           [1,19,24]
        // [1,3,12]  IR1= LB1[MAC1]                                     [1,19,12][lm=1]
        // [1,3,12]  IR2= LB2[MAC2]                                     [1,19,12][lm=1]
        // [1,3,12]  IR3= LB3[MAC3]                                     [1,19,12][lm=1]

        long c11 = reg_lc.m11;
        long c12 = reg_lc.m12;
        long c13 = reg_lc.m13;
        long c21 = reg_lc.m21;
        long c22 = reg_lc.m22;
        long c23 = reg_lc.m23;
        long c31 = reg_lc.m31;
        long c32 = reg_lc.m32;
        long c33 = reg_lc.m33;

        long bkr = reg_rbk;
        long bkg = reg_gbk;
        long bkb = reg_bbk;

        mac1 = A1(c11 * ir1 + c12 * ir2 + c13 * ir3 + (bkr << 12));
        mac2 = A2(c21 * ir1 + c22 * ir2 + c23 * ir3 + (bkg << 12));
        mac3 = A3(c31 * ir1 + c32 * ir2 + c33 * ir3 + (bkb << 12));

        ir1 = LiB1_1(mac1);
        ir2 = LiB2_1(mac2);
        ir3 = LiB3_1(mac3);

        // [1,27,4]  MAC1=A1[R*IR1 + IR0*(LB1[RFC-R*IR1])]              [1,27,16][lm=0]
        // [1,27,4]  MAC2=A1[G*IR2 + IR0*(LB2[GFC-G*IR2])]              [1,27,16][lm=0]
        // [1,27,4]  MAC3=A1[B*IR3 + IR0*(LB3[BFC-B*IR3])]              [1,27,16][lm=0]
        // [1,3,4]  IR1= LB1[MAC1]                                     [1,27,4][lm=1]
        // [1,3,4]  IR2= LB2[MAC2]                                     [1,27,4][lm=1]
        // [1,3,4]  IR3= LB3[MAC3]                                     [1,27,4][lm=1]
        long ir0 = reg_ir0;
        reg_mac1 = A1(r * ir1 + ((ir0 * LiB1_0((reg_rfc << 12) - r * ir1)) >> 12));
        reg_mac2 = A2(g * ir2 + ((ir0 * LiB2_0((reg_gfc << 12) - g * ir2)) >> 12));
        reg_mac3 = A3(b * ir3 + ((ir0 * LiB3_0((reg_bfc << 12) - b * ir3)) >> 12));

        // [0,8,0]   Cd0<-Cd1<-Cd2<- CODE
        // [0,8,0]   R0<-R1<-R2<- LC1[MAC1]                             [1,27,4]
        // [0,8,0]   G0<-G1<-G2<- LC2[MAC2]                             [1,27,4]
        // [0,8,0]   B0<-B1<-B2<- LC3[MAC3]                             [1,27,4]
        int rr = LiC1(reg_mac1 >> 4);
        int gg = LiC2(reg_mac2 >> 4);
        int bb = LiC3(reg_mac3 >> 4);
        reg_rgb0 = rr | (gg << 8) | (bb << 16) | chi;

        // 2 ----

        mac1 = A1(m11 * reg_v1.x + m12 * reg_v1.y + m13 * reg_v1.z);
        mac2 = A2(m21 * reg_v1.x + m22 * reg_v1.y + m23 * reg_v1.z);
        mac3 = A3(m31 * reg_v1.x + m32 * reg_v1.y + m33 * reg_v1.z);
        ir1 = LiB1_1(mac1);
        ir2 = LiB2_1(mac2);
        ir3 = LiB3_1(mac3);

        mac1 = A1(c11 * ir1 + c12 * ir2 + c13 * ir3 + (bkr << 12));
        mac2 = A2(c21 * ir1 + c22 * ir2 + c23 * ir3 + (bkg << 12));
        mac3 = A3(c31 * ir1 + c32 * ir2 + c33 * ir3 + (bkb << 12));

        ir1 = LiB1_1(mac1);
        ir2 = LiB2_1(mac2);
        ir3 = LiB3_1(mac3);

        reg_mac1 = A1(r * ir1 + ((ir0 * LiB1_0((reg_rfc << 12) - r * ir1)) >> 12));
        reg_mac2 = A2(g * ir2 + ((ir0 * LiB2_0((reg_gfc << 12) - g * ir2)) >> 12));
        reg_mac3 = A3(b * ir3 + ((ir0 * LiB3_0((reg_bfc << 12) - b * ir3)) >> 12));

        rr = LiC1(reg_mac1 >> 4);
        gg = LiC2(reg_mac2 >> 4);
        bb = LiC3(reg_mac3 >> 4);
        reg_rgb1 = rr | (gg << 8) | (bb << 16) | chi;

        // 3 ----

        mac1 = A1(m11 * reg_v0.x + m12 * reg_v0.y + m13 * reg_v0.z);
        mac2 = A2(m21 * reg_v0.x + m22 * reg_v0.y + m23 * reg_v0.z);
        mac3 = A3(m31 * reg_v0.x + m32 * reg_v0.y + m33 * reg_v0.z);
        ir1 = LiB1_1(mac1);
        ir2 = LiB2_1(mac2);
        ir3 = LiB3_1(mac3);

        mac1 = A1(c11 * ir1 + c12 * ir2 + c13 * ir3 + (bkr << 12));
        mac2 = A2(c21 * ir1 + c22 * ir2 + c23 * ir3 + (bkg << 12));
        mac3 = A3(c31 * ir1 + c32 * ir2 + c33 * ir3 + (bkb << 12));

        ir1 = LiB1_1(mac1);
        ir2 = LiB2_1(mac2);
        ir3 = LiB3_1(mac3);

        reg_mac1 = A1(r * ir1 + ((ir0 * LiB1_0((reg_rfc << 12) - r * ir1)) >> 12));
        reg_mac2 = A2(g * ir2 + ((ir0 * LiB2_0((reg_gfc << 12) - g * ir2)) >> 12));
        reg_mac3 = A3(b * ir3 + ((ir0 * LiB3_0((reg_bfc << 12) - b * ir3)) >> 12));

        rr = LiC1(reg_mac1 >> 4);
        gg = LiC2(reg_mac2 >> 4);
        bb = LiC3(reg_mac3 >> 4);
        reg_rgb2 = rr | (gg << 8) | (bb << 16) | chi;
    }

    public static void interpret_dpct(final int ci) {
        reg_flag = 0;

        int chi = reg_rgb & 0xff000000;
        int r = (reg_rgb & 0xff) << 4;
        int g = (reg_rgb & 0xff00) >> 4;
        int b = (reg_rgb & 0xff0000) >> 12;

        // TODO - is this B1 all the way correct?
        reg_mac1 = A1((r << 12) + reg_ir0 * LiB1_0(reg_rfc - r));
        reg_mac2 = A2((g << 12) + reg_ir0 * LiB1_0(reg_gfc - g));
        reg_mac3 = A3((b << 12) + reg_ir0 * LiB1_0(reg_bfc - b));

        int rr = LiC1(reg_mac1 >> 4);
        int gg = LiC2(reg_mac2 >> 4);
        int bb = LiC3(reg_mac3 >> 4);
        reg_rgb0 = rr | (gg << 8) | (bb << 16) | chi;

        // 2 ----

        // TODO - is this B1 all the way correct?
        reg_mac1 = A1((r << 12) + reg_ir0 * LiB1_0(reg_rfc - r));
        reg_mac2 = A2((g << 12) + reg_ir0 * LiB1_0(reg_gfc - g));
        reg_mac3 = A3((b << 12) + reg_ir0 * LiB1_0(reg_bfc - b));

        rr = LiC1(reg_mac1 >> 4);
        gg = LiC2(reg_mac2 >> 4);
        bb = LiC3(reg_mac3 >> 4);

        reg_rgb1 = rr | (gg << 8) | (bb << 16) | chi;

        // 3 ----

        // TODO - is this B1 all the way correct?
        reg_mac1 = A1((r << 12) + reg_ir0 * LiB1_0(reg_rfc - r));
        reg_mac2 = A2((g << 12) + reg_ir0 * LiB1_0(reg_gfc - g));
        reg_mac3 = A3((b << 12) + reg_ir0 * LiB1_0(reg_bfc - b));

        reg_ir1 = LiB1_0(reg_mac1);
        reg_ir2 = LiB2_0(reg_mac2);
        reg_ir3 = LiB3_0(reg_mac3);

        rr = LiC1(reg_mac1 >> 4);
        gg = LiC2(reg_mac2 >> 4);
        bb = LiC3(reg_mac3 >> 4);
        reg_rgb2 = rr | (gg << 8) | (bb << 16) | chi;
    }

    public static void interpret_nccs(final int ci) {
        // untested
        reg_flag = 0;

        int chi = reg_rgb & 0xff000000;
        int r = (reg_rgb & 0xff) << 4;
        int g = (reg_rgb & 0xff00) >> 4;
        int b = (reg_rgb & 0xff0000) >> 12;

        long m11 = reg_ls.m11;
        long m12 = reg_ls.m12;
        long m13 = reg_ls.m13;
        long m21 = reg_ls.m21;
        long m22 = reg_ls.m22;
        long m23 = reg_ls.m23;
        long m31 = reg_ls.m31;
        long m32 = reg_ls.m32;
        long m33 = reg_ls.m33;

        long ss1 = m11 * reg_v0.x + m12 * reg_v0.y + m13 * reg_v0.z;
        long ss2 = m21 * reg_v0.x + m22 * reg_v0.y + m23 * reg_v0.z;
        long ss3 = m31 * reg_v0.x + m32 * reg_v0.y + m33 * reg_v0.z;

        int mac1 = A1(ss1);
        int mac2 = A2(ss2);
        int mac3 = A3(ss3);

        int ir1 = LiB1_1(mac1);
        int ir2 = LiB2_1(mac2);
        int ir3 = LiB3_1(mac3);

        long c11 = reg_lc.m11;
        long c12 = reg_lc.m12;
        long c13 = reg_lc.m13;
        long c21 = reg_lc.m21;
        long c22 = reg_lc.m22;
        long c23 = reg_lc.m23;
        long c31 = reg_lc.m31;
        long c32 = reg_lc.m32;
        long c33 = reg_lc.m33;

        long bkr = reg_rbk;
        long bkg = reg_gbk;
        long bkb = reg_bbk;

        ss1 = c11 * ir1 + c12 * ir2 + c13 * ir3 + (bkr << 12);
        ss2 = c21 * ir1 + c22 * ir2 + c23 * ir3 + (bkg << 12);
        ss3 = c31 * ir1 + c32 * ir2 + c33 * ir3 + (bkb << 12);

        mac1 = A1(ss1);
        mac2 = A2(ss2);
        mac3 = A3(ss3);

        ir1 = LiB1_1(mac1);
        ir2 = LiB2_1(mac2);
        ir3 = LiB3_1(mac3);

        reg_mac1 = A1(r * ir1);
        reg_mac2 = A2(g * ir2);
        reg_mac3 = A3(b * ir3);
        reg_ir1 = LiB1_1(reg_mac1);
        reg_ir2 = LiB2_1(reg_mac2);
        reg_ir3 = LiB3_1(reg_mac3);

        int rr = LiC1(reg_mac1 >> 4);
        int gg = LiC2(reg_mac2 >> 4);
        int bb = LiC3(reg_mac3 >> 4);
        reg_rgb0 = reg_rgb1;
        reg_rgb1 = reg_rgb2;
        reg_rgb2 = rr | (gg << 8) | (bb << 16) | chi;
    }

    public static void interpret_cdp(final int ci) {
        if (true) throw new IllegalStateException("GTE UNIMPLEMENTED: CDP");
    }

    public static void interpret_cc(final int ci) {
        if (true) throw new IllegalStateException("GTE UNIMPLEMENTED: CC");
    }

    public static void interpret_gpl(final int ci) {
        reg_flag = 0;

        long i = reg_ir0;
        if (0 != (ci & GTE_SF_MASK)) {
            reg_mac1 = A1((((long) reg_mac1) << 12) + i * reg_ir1);
            reg_mac2 = A2((((long) reg_mac2) << 12) + i * reg_ir2);
            reg_mac3 = A3((((long) reg_mac3) << 12) + i * reg_ir3);
        } else {
            reg_mac1 = A1((reg_mac1 + i * reg_ir1) << 12);
            reg_mac2 = A2((reg_mac2 + i * reg_ir2) << 12);
            reg_mac3 = A3((reg_mac3 + i * reg_ir3) << 12);
        }
        reg_ir1 = LiB1_0(reg_mac1);
        reg_ir2 = LiB2_0(reg_mac2);
        reg_ir3 = LiB3_0(reg_mac3);
        int rr = LiC1(reg_mac1 >> 4);
        int gg = LiC2(reg_mac2 >> 4);
        int bb = LiC3(reg_mac3 >> 4);
        reg_rgb0 = reg_rgb1;
        reg_rgb1 = reg_rgb2;
        reg_rgb2 = (reg_rgb & 0xff000000) | rr | (gg << 8) | (bb << 16);
    }
}
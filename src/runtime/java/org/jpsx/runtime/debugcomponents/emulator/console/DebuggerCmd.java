package org.jpsx.runtime.debugcomponents.emulator.console;

public enum DebuggerCmd {
    CMD_SEND_DUMB(0x100, noArgs(), ""),
    CMD_GET_PCSXR_VER(0x101, noArgs(), ""),
    CMD_GET_PROTOCOL_VER(0x102, noArgs(), ""),
    CMD_GET_STATUS(0x103, noArgs(), ""),
    CMD_GET_PC_REG(0x110, noArgs(), "^210 PC=([0-9A-F]{8})$"),
    CMD_GET_GPR_REG(0x111, oneWordArg(), "^211 ([0-9A-F]{2})\\((\\w+)\\)=([0-9A-F]{8})$"),
    CMD_GET_LO_HI_REGS(0x112, noArgs(), "^212 LO=([0-9A-F]{8}) HI=([0-9A-F]{8})$"),
    CMD_GET_COP0_REG(0x113, noArgs(), ""),
    CMD_GET_COP2_CTRL_REG(0x114, noArgs(), ""),
    CMD_GET_COP2_DATA_REG(0x115, noArgs(), ""),
    CMD_SET_GP_REG(0x121, noArgs(), ""),
    CMD_SET_LO_HI(0x122, noArgs(), ""),
    CMD_SET_COP0_REG(0x123, noArgs(), ""),
    CMD_SET_COP2_CTRL_REG(0x124, noArgs(), ""),
    CMD_SET_COP2_DATA_REG(0x125, noArgs(), ""),
    CMD_DUMP_MEM(0x130, noArgs(), ""),
    CMD_SET_MEM(0x140, noArgs(), ""),
    CMD_START_STOP_EXEC_MAP(0x150, noArgs(), ""),
    CMD_START_STOP_READ8_MAP(0x151, noArgs(), ""),
    CMD_START_STOP_READ16_MAP(0x152, noArgs(), ""),
    CMD_START_STOP_READ32_MAP(0x153, noArgs(), ""),
    CMD_START_STOP_WRITE8_MAP(0x154, noArgs(), ""),
    CMD_START_STOP_WRITE16_MAP(0x155, noArgs(), ""),
    CMD_START_STOP_WRITE32_MAP(0x156, noArgs(), ""),
    CMD_BREAK_ON_EXEC_MAP(0x160, noArgs(), ""),
    CMD_BREAK_ON_READ8_MAP(0x161, noArgs(), ""),
    CMD_BREAK_ON_READ16_MAP(0x162, noArgs(), ""),
    CMD_BREAK_ON_READ32_MAP(0x163, noArgs(), ""),
    CMD_BREAK_ON_WRITE8_MAP(0x164, noArgs(), ""),
    CMD_BREAK_ON_WRITE16_MAP(0x165, noArgs(), ""),
    CMD_BREAK_ON_WRITE32_MAP(0x166, noArgs(), ""),

    CMD_GET_BPT_COUNT(0x300, noArgs(), ""),
    CMD_DELETE_BPT(0x301, noArgs(), ""),

    CMD_SET_EXEC_BPT(0x310, noArgs(), ""),
    CMD_SET_READ1_BPT(0x320, noArgs(), ""),
    CMD_SET_READ2_BPT(0x321, noArgs(), ""),
    CMD_SET_READ4_BPT(0x322, noArgs(), ""),

    CMD_SET_WRITE1_BPT(0x330, noArgs(), ""),
    CMD_SET_WRITE2_BPT(0x331, noArgs(), ""),
    CMD_SET_WRITE4_BPT(0x332, noArgs(), ""),

    CMD_PAUSE_EXECUTION(0x390, noArgs(), ""),
    CMD_RESUME_EXECUTION(0x391, noArgs(), ""),
    CMD_TRACE_EXECUTION(0x395, noArgs(), ""),

    CMD_SOFT_RESET(0x398, noArgs(), ""),
    CMD_HARD_RESET(0x399, noArgs(), ""),

    CMD_RUN_TO(0x3A0, oneLongArg(), ""),
    CMD_STEP_OVER(0x3A1, noArgs(), "");

    public static final int NO_ARGS = 0;
    public  static final int ONE_WORD_ARG = 1;
    public  static final int ONE_LONG_ARG = 2;

    public final int cmd;
    private final int sendFormat;
    private final String recvFormat;

    private DebuggerCmd(int cmd, int sendFormat, String recvFormat) {
        this.cmd = cmd;
        this.sendFormat = sendFormat;
        this.recvFormat = recvFormat;
    }

    public int getInt() {
        return cmd;
    }

    public int getSendFormat() {
        return sendFormat;
    }

    public String getRecvFormat() {
        return recvFormat;
    }

    private static int noArgs() {
        return NO_ARGS;
    }

    private static int oneWordArg() {
        return ONE_WORD_ARG;
    }

    private static int oneLongArg() {
        return ONE_LONG_ARG;
    }
}

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
package org.jpsx.runtime.components.emulator.compiler;

import org.apache.bcel.classfile.JavaClass;
import org.apache.log4j.Logger;
import org.jpsx.api.components.core.ContinueExecutionException;
import org.jpsx.api.components.core.ReturnFromExceptionException;
import org.jpsx.api.components.core.addressspace.AddressSpace;
import org.jpsx.api.components.core.cpu.CPUInstruction;
import org.jpsx.api.components.core.cpu.NativeCompiler;
import org.jpsx.api.components.core.cpu.R3000;
import org.jpsx.bootstrap.util.CollectionsFactory;
import org.jpsx.runtime.FinalComponentSettings;
import org.jpsx.runtime.FinalResolvedConnectionCache;
import org.jpsx.runtime.RuntimeConnections;
import org.jpsx.runtime.SingletonJPSXComponent;
import org.jpsx.runtime.components.core.CoreComponentConnections;
import org.jpsx.runtime.util.MiscUtil;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.Map;

// todo, compilation shouldn't fail it should just make a class which throws ContinueExecutionException

// todo set addressSpace.tagAddressAccess based on whether we're doing 2 stage compile

// todo, cope with compilation erros caused by data being overwitten in another thread

// todo consider caching bytecode for instruction cache flush

public class MultiStageCompiler extends SingletonJPSXComponent implements NativeCompiler {
    public static final String CATEGORY = "Compiler";
    private static final Logger log = Logger.getLogger(CATEGORY);

    protected static String CLASS = MultiStageCompiler.class.getName();

    protected static Stage1Generator immediateGenerator;
    // for use if we need to figure out what the real stage2 generator
    // would have done (to fixup unwritten regs when we mispredict memory access
    // and have to return to the interpreter);
    // note we can't use the real one since it is not thread safe
    private static Stage2Generator fixupStage2Generator;

    protected static CompilerClassLoader ramLoader;
    protected static CompilerClassLoader romLoader;
    protected static int ramLoaderCount = 0;

    protected static Map<Integer, CodeUnit> romUnits = CollectionsFactory.newHashMap();
    protected static Map<Integer, CodeUnit> ramUnits = CollectionsFactory.newHashMap();

    protected static CompilationBroker broker;

    private static final int MAX_BREAKPOINTS = 64;

    private static int[] breakpoints = new int[MAX_BREAKPOINTS];
    private static int breakpointLimit;

    private static class Refs extends FinalResolvedConnectionCache {
        public static final int[] interpreterRegs = CoreComponentConnections.R3000.resolve().getInterpreterRegs();
    }

    private static R3000 r3000;

    @Override
    public void resolveConnections() {
        super.resolveConnections();
        addressSpace = CoreComponentConnections.ADDRESS_SPACE.resolve();
        r3000 = CoreComponentConnections.R3000.resolve();
    }

    protected static class ExecutionContext {
        public int nativeDepth;
        public boolean cacheStale;
    }

    protected static int contextDepth = 0;
    protected static ExecutionContext context;
    protected static final int MAX_CONTEXT_DEPTH = 4;
    protected static final ExecutionContext[] contexts;

    protected static boolean ownRegs;
    private static AddressSpace addressSpace;

    public MultiStageCompiler() {
        super("JPSX Multi-Stage Compiler");
    }

    /**
     * Number of jr/jalr calls within native code... we
     * limit these if they get to big, in case we're
     * not running functions as we suspect... e.g.
     * Final Fantasy has a state machine with jal which recurses
     * consuming no stack rather than returning!
     */

    static {
        contexts = new ExecutionContext[MAX_CONTEXT_DEPTH];
        for (int i = 0; i < MAX_CONTEXT_DEPTH; i++) {
            contexts[i] = new ExecutionContext();
        }
        context = contexts[0];
    }

    protected static class Settings extends FinalComponentSettings {
        // todo, assert this happens late enough
        public static final boolean enableSpeculativeCompilation = true;
        public static final boolean enableSecondStage = getComponent().getBooleanProperty("stage2", true);
        public static final boolean secondStageInBackground = true;
        public static final int minSizeForSpeculativeCompile = 50;
        public static final int maxNativeDepth = 100;
        public static final int stage2Threshold = 30;
        public static final int maxR3000InstructionsPerUnit = 8000;
// 1       at       Assembler temporary.
// 2- 3    v0-v1    Subroutine return values
// 4- 7    a0-a3    Subroutine arguments
// 8-15    t0-t7    Temporaries, may be changed by subroutines
//16-23    s0-s7    Register variables, must be saved by subs.
//24-25    t8-t9    Temporaries, may be changed by subroutines
//26-27    k0-k1    Reserved for the kernel
//28       gp       Global pointer
//29       sp       Stack pointer
//30       fp(s8)   9th register variable, subs can use this as a frame
        //                  pointer
        //31       ra       Return address
        public static final int savedOnCallRegs = 0x30ff0001;
        public static final int alwaysRAMRegs = 0x20000000;
        public static final boolean saveClasses = getComponent().getBooleanProperty("saveClasses", false);
        public static final boolean biosInterruptWorkaround = true;
        public static final boolean printCode = getComponent().getBooleanProperty("printCode", false);
        protected static final boolean addLineNumbers = true;
        protected static final boolean debugPC = false;
        protected static final boolean profiling = false;
        protected static final boolean megaTrace = false;
        protected static final boolean printRare = false;
        protected static final boolean statistics = false;
    }

    @Override
    public void init() {
        super.init();
        Settings.setComponent(this);
        log.info("printCode " + Settings.printCode);
        log.info("Second stage enabled = " + Settings.enableSecondStage);
        CoreComponentConnections.NATIVE_COMPILER.set(this);
    }

    public void begin() {
        immediateGenerator = new Stage1Generator("c1gen.out");
        romLoader = new CompilerClassLoader("ROM classloader", MultiStageCompiler.class.getClassLoader());
        broker = new CompilationBroker();
        broker.begin();
        clearCache();
    }

    public boolean jumpAndLink(int address, int returnAddress) {
        int oldNativeDepth = contexts[contextDepth].nativeDepth;

        context = contexts[++contextDepth];
        assert !context.cacheStale;

        CodeUnit unit = getCodeUnit(address);

        Executable exec = unit.getExecutable();
        if (exec == null) {
            exec = makeExecutable(unit);
            if (exec == null) {
                return false;
            }
        }

        context.nativeDepth = oldNativeDepth;
        if (!ownRegs) {
            interpreterToCompiler();
        }
        int retaddr = exec.e(returnAddress, false);
        compilerToInterpreter();
        assert retaddr == returnAddress;
        assert oldNativeDepth == context.nativeDepth : "nativeDepth mismatch for " + MiscUtil.toHex(address, 8) + " " + oldNativeDepth + "!=" + context.nativeDepth;
        contextDepth--;
        assert contextDepth >= 0;
        r3000.setPC(retaddr);
        return true;
    }

    /**
     * Note this method is always called in the execution thread
     * either from the classloader, or from jumpAndLink
     */
    protected static Executable makeExecutable(CodeUnit unit) {
        assert r3000.isExecutionThread();
        JavaClass jclass = unit.getStage1JavaClass(immediateGenerator, true);
        Class clazz = createClass(unit, jclass);
        unit.stage1ClassReady();
        try {
            Executable executable = (Executable) clazz.newInstance();
            Field field = clazz.getField("unit");
            field.set(executable, unit);
            broker.registerLinkedFunctions(unit, true);
            unit.setExecutable(executable);
            return executable;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new IllegalStateException("could not create/cast to CodeUnit " + clazz.getName());
        }
    }

    public void clearCache() {
        synchronized (getClass()) {
            broker.reset();
            ramLoaderCount++;
            // we delegate to the rom loader for bios functions
            ramLoader = new CompilerClassLoader("RAM classloader " + ramLoaderCount, romLoader);
            synchronized (ramUnits) {
                ramUnits.clear();
            }
            // make sure we know that we cannot continue executing code
            // in any higher nested execution levels
            // todo this should be 0...?
            for (int i = 1; i < contextDepth; i++) {
                contexts[i].cacheStale = true;
            }
        }
    }

    public boolean exceptionInCompiler(Throwable t) {
        //System.out.println("Exception in compiler depth="+contextDepth+" "+t.getClass().getName());
        assert contextDepth >= 1;
        context = contexts[--contextDepth];
        compilerToInterpreter();
        if (t instanceof ReturnFromExceptionException || t instanceof ContinueExecutionException) {
            // don't need to update PC
            return true;
        }

        StackTraceElement trace[] = t.getStackTrace();
        int pc = -1;
        int base = 0;
        String className = null;
        for (int i = 0; i < trace.length; i++) {
            className = trace[i].getClassName();
            if (className.length() == 10 && className.startsWith("_")) {
                String methodName = trace[i].getMethodName();
                if (methodName.equals(Stage1Generator.STATIC_METHOD) || methodName.startsWith(Stage1Generator.UNINLINED_METHOD_PREFIX)) {
                    pc = base = MiscUtil.parseHex(className.substring(2));
                    int ln = trace[i].getLineNumber();
                    if (ln >= 0) {
                        pc += ln * 4;
                    }
                    break;
                    // todo constant
                } else if (methodName.equals(Stage1Generator.NORMAL_METHOD)) {
                    System.out.println("Can't get PC!");
                    break;
                }
            }
        }
        if (pc != -1) {
            r3000.setPC(pc);
            if (!className.startsWith(Stage1Generator.CLASS_NAME_PREFIX)) {
                if (0 != (addressSpace.getTag(pc) & AddressSpace.TAG_RESERVED_FOR_COMPILER)) {
                    // if we get here it is because we have had a memory mispredict, but we cannot safely
                    // restart, because we omitted code to update actual register values during this basic block.
                    // what we need to do here, is to re-examine the code unit, and figure out what the register values must have been (i.e.
                    // for any CR at this location, and update those).
                    if (log.isDebugEnabled()) {
                        log.debug("NEED REG WRITEBACK AT " + MiscUtil.toHex(pc, 8));
                    }
                    fixupUnwrittenCompilerRegs(base, pc);
                }
                if (t.getClass() == ArrayIndexOutOfBoundsException.class) {
                    // could be due to mis-predicted memory access
                    int ci = addressSpace.internalRead32(pc);
                    CPUInstruction inst = r3000.decodeInstruction(ci);
                    if (0 != (inst.getFlags() & CPUInstruction.FLAG_MEM)) {
                        if (log.isDebugEnabled()) {
                            log.debug("***** Mispredicted memory access at " + MiscUtil.toHex(pc, 8));
                        }
                        CodeUnit unit = getCodeUnit(base);
                        assert unit.useStage2;
                        unit.stage2ClassBroken();
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void interrupt() {
        isInterrupted = true;
    }

    public synchronized boolean addBreakpoint(int address) {
        boolean ok = false;
        for (int i = 0; i < breakpointLimit; i++) {
            if (breakpoints[i] == -1) {
                breakpoints[i] = address;
                ok = true;
                break;
            }
        }
        if (!ok) {
            if (breakpointLimit == MAX_BREAKPOINTS) {
                return false;
            }
            breakpoints[breakpointLimit++] = address;
        }
        Map<Integer, CodeUnit> map = AddressSpace.Util.isBIOS(address) ? romUnits : ramUnits;
        synchronized (map) {
            for (CodeUnit unit : map.values()) {
                unit.breakpointAdded(address);
            }
        }
        // make sure we know that we should not continue executing code
        // in any higher nested execution levels
        for (int i = 1; i < contextDepth; i++) {
            contexts[i].cacheStale = true;
        }
        return true;
    }

    public synchronized void removeBreakpoint(int address) {
        for (int i = 0; i < breakpointLimit; i++) {
            if (breakpoints[i] == address) {
                breakpoints[i] = -1;
                if (i == breakpointLimit - 1) {
                    breakpointLimit--;
                }
                Map<Integer, CodeUnit> map = AddressSpace.Util.isBIOS(address) ? romUnits : ramUnits;
                synchronized (map) {
                    for (CodeUnit unit : map.values()) {
                        unit.breakpointRemoved(address);
                    }
                }
                return;
            }
        }
        assert false : "attempt to remove non-existant breakpoint " + MiscUtil.toHex(address, 8);
    }

    public static void enumerateBreakpoints(CodeUnit unit) {
        for (int i = 0; i < breakpointLimit; i++) {
            if (breakpoints[i] != -1) {
                unit.breakpointAdded(breakpoints[i]);
            }
        }
    }

    public int getReg(int index) {
        assert false;
        return 0;
    }

    public void setReg(int index, int value) {
        assert false;
    }

    /**
     * called by the class loader when a referenced
     * but not yet generated class is executed
     */
    public static Class generateClass(String classname) {
        if (classname.startsWith(Stage1Generator.CLASS_NAME_PREFIX)) {
            int address = MiscUtil.parseHex(classname.substring(2));
            CodeUnit unit = getCodeUnit(address);
            Executable executable = makeExecutable(unit);
            // todo; what if this fails?
            return executable.getClass();
        } else {
            throw new IllegalStateException("findClass not called for C1: " + classname);
        }
    }

    protected static Class createClass(CodeUnit unit, JavaClass jclass) {
        if (AddressSpace.Util.isBIOS(unit.getBase())) {
            return romLoader.createClass(jclass.getClassName(), jclass.getBytes());
        } else {
            return ramLoader.createClass(jclass.getClassName(), jclass.getBytes());
        }
    }

    protected static void returnToInterpreter(int address) {
        compilerToInterpreter();
        r3000.setPC(address);
        throw ContinueExecutionException.DONT_SKIP_CURRENT;
    }

    public static void registerForStage2(CodeUnit unit) {
        broker.registerForStage2(unit);
    }

    protected static CodeUnit getCodeUnit(int address) {
        Map<Integer, CodeUnit> map = AddressSpace.Util.isBIOS(address) ? romUnits : ramUnits;
        Integer key = address;
        synchronized (map) {
            CodeUnit rc = (CodeUnit) map.get(key);
            if (rc == null) {
                rc = new CodeUnit(address);
                map.put(key, rc);
            }
            return rc;
        }
    }

    protected static class CompilationBroker implements Runnable {
        protected LinkedList<CodeUnit> unitsToFollow = new LinkedList<CodeUnit>();
        protected LinkedList<CodeUnit> unitsForStage1 = new LinkedList<CodeUnit>();
        protected LinkedList<CodeUnit> unitsForStage2 = new LinkedList<CodeUnit>();

        protected FlowAnalyzer flowAnalyzer = new FlowAnalyzer();
        protected Stage1Generator stage1Generator;
        protected Stage2Generator stage2Generator;
        protected int resetCount;

        public CompilationBroker() {
            if (Settings.enableSpeculativeCompilation) {
                stage1Generator = new Stage1Generator("c1specgen.out");
            }
            if (Settings.enableSecondStage) {
                stage2Generator = new Stage2Generator("c2gen.out");
            }
        }

        public void begin() {
            if (Settings.enableSpeculativeCompilation ||
                    (Settings.enableSecondStage && Settings.secondStageInBackground)) {
                log.info("Starting background compilation thread");
                Thread t = new Thread(this, "Background compilation");
                t.setPriority(Thread.NORM_PRIORITY - 2); // low priority
                t.start();
            }
        }

        public synchronized void registerLinkedFunctions(CodeUnit unit, boolean executionThread) {
            if (!Settings.enableSpeculativeCompilation || unit.linksFollowed || unit.isROM()) {
                return;
            }
            if (executionThread) {
                // when called from the main compile thread, we
                // put the function to the head of the list
                unitsToFollow.addFirst(unit);
                notify();
            } else {
                // ones we follow go at end, i.e. the go behind any functions we
                // have actually executed
                unitsToFollow.addLast(unit);
            }
        }

        public synchronized void registerForStage2(CodeUnit unit) {
            if (Settings.enableSecondStage) {
                if (Settings.secondStageInBackground) {
                    unitsForStage2.add(unit);
                    notify();
                } else {
                    //System.out.println( "foreground stage2 compile " + MiscUtil.toHex( unit.getBase(), 8 ) );
                    JavaClass jclass = unit.getStage2JavaClass(stage2Generator);
                    Class clazz = createClass(unit, jclass);
                    unit.stage2ClassReady(clazz);
                }
            }
        }

        // todo: since we're compiling in the background it is possible we'll compile
        // something that it is being overwritten...
        public void run() {
            try {
                while (true) {
                    CodeUnit linkUnit = null;
                    CodeUnit c1Unit = null;
                    CodeUnit c2Unit = null;
                    int priorResetCount = 0;
                    synchronized (this) {
                        priorResetCount = resetCount;
                        if (unitsToFollow.size() > 0) {
                            // first priority is to follow any new links
                            linkUnit = (CodeUnit) unitsToFollow.removeFirst();
                        } else if (unitsForStage1.size() > 0) {
                            // second priority is speculative compile
                            c1Unit = (CodeUnit) unitsForStage1.removeFirst();
                        } else if (unitsForStage2.size() > 0) {
                            // third priority is stage 2 compile
                            c2Unit = (CodeUnit) unitsForStage2.removeFirst();
                        } else {
                            try {
                                wait();
                            } catch (InterruptedException e) {
                            }
                        }
                    }
                    if (linkUnit != null && !linkUnit.linksFollowed) {
                        //System.out.println("Follow links "+MiscUtil.toHex( linkUnit.getBase(), 8));
                        followLinks(linkUnit);
                    } else if (c1Unit != null) {
                        // simply get the java class
                        //System.out.println("Spec compile "+MiscUtil.toHex( c1Unit.getBase(), 8));
                        JavaClass jclass = c1Unit.getStage1JavaClass(stage1Generator, false);
                    } else if (c2Unit != null) {
                        //System.out.println("background stage2 compile "+MiscUtil.toHex( c2Unit.getBase(), 8));
                        JavaClass jclass = c2Unit.getStage2JavaClass(stage2Generator);
                        // todo fix this;
                        // a) resetCount is not protected;
                        // b) don't want to delay
//                        synchronized( this) {
                        if (resetCount == priorResetCount) {
                            // only create the class if we haven't been reset
                            Class clazz = createClass(c2Unit, jclass);
                            c2Unit.stage2ClassReady(clazz);
                        }
//                        }
                    }
                }
            } catch (Throwable t) {
                log.error(this + " exits abnormally:", t);
                // todo fix this
                RuntimeConnections.MACHINE.resolve().close();
            }
        }

        public synchronized void reset() {
            unitsToFollow.clear();
            unitsForStage1.clear();
            unitsForStage2.clear();
            resetCount++;
        }

        protected void followLinks(CodeUnit unit) {
            FlowAnalyzer.FlowInfo flowInfo = unit.getFlowInfo(flowAnalyzer);
            if (!unit.stage1Ready() && flowInfo.instructionCount > Settings.minSizeForSpeculativeCompile) {
                //System.out.println(">>>>>>>>>>>> should compile "+MiscUtil.toHex( unit.base, 8));
                unitsForStage1.add(unit);
            }
            for (FlowAnalyzer.BasicBlock block = flowInfo.root; block != null; block = block.next) {
                if (block.type == FlowAnalyzer.BasicBlock.NORMAL) {
                    for (int offset = block.offset; offset < block.offset + block.size; offset++) {
                        int address = flowInfo.base + offset * 4;
                        int ci = addressSpace.internalRead32(address);
                        CPUInstruction inst = r3000.decodeInstruction(ci);
                        int iFlags = inst.getFlags();
                        if (0 != (iFlags & CPUInstruction.FLAG_LINK)) {
                            if (0 != (iFlags & CPUInstruction.FLAG_IMM_FAR_TARGET)) {
                                int target = ((address + 4) & 0xf0000000) | ((ci & 0x3fffff) << 2);
//                                System.out.println(MiscUtil.toHex( target, 8)+" called from "+MiscUtil.toHex( address, 8));
                                if (!AddressSpace.Util.isBIOS(target)) {
                                    registerLinkedFunctions(getCodeUnit(target), false);
                                }
                            }
                        }
                    }
                }
            }
            unit.linksFollowed = true;
        }
    }

    /**
     * for use of compiled code, since we always know what
     * register an instruction uses, there's no point
     * in taking the hit of an array lookup
     */
    public static int reg_1;
    public static int reg_2;
    public static int reg_3;
    public static int reg_4;
    public static int reg_5;
    public static int reg_6;
    public static int reg_7;
    public static int reg_8;
    public static int reg_9;
    public static int reg_10;
    public static int reg_11;
    public static int reg_12;
    public static int reg_13;
    public static int reg_14;
    public static int reg_15;
    public static int reg_16;
    public static int reg_17;
    public static int reg_18;
    public static int reg_19;
    public static int reg_20;
    public static int reg_21;
    public static int reg_22;
    public static int reg_23;
    public static int reg_24;
    public static int reg_25;
    public static int reg_26;
    public static int reg_27;
    public static int reg_28;
    public static int reg_29;
    public static int reg_30;
    public static int reg_31;
    public static boolean isInterrupted;

    public static final String INTERPRETER_TO_COMPILER_METHOD = "interpreterToCompiler";

    public static void interpreterToCompiler() {
        assert !ownRegs;
        reg_1 = Refs.interpreterRegs[1];
        reg_2 = Refs.interpreterRegs[2];
        reg_3 = Refs.interpreterRegs[3];
        reg_4 = Refs.interpreterRegs[4];
        reg_5 = Refs.interpreterRegs[5];
        reg_6 = Refs.interpreterRegs[6];
        reg_7 = Refs.interpreterRegs[7];
        reg_8 = Refs.interpreterRegs[8];
        reg_9 = Refs.interpreterRegs[9];
        reg_10 = Refs.interpreterRegs[10];
        reg_11 = Refs.interpreterRegs[11];
        reg_12 = Refs.interpreterRegs[12];
        reg_13 = Refs.interpreterRegs[13];
        reg_14 = Refs.interpreterRegs[14];
        reg_15 = Refs.interpreterRegs[15];
        reg_16 = Refs.interpreterRegs[16];
        reg_17 = Refs.interpreterRegs[17];
        reg_18 = Refs.interpreterRegs[18];
        reg_19 = Refs.interpreterRegs[19];
        reg_20 = Refs.interpreterRegs[20];
        reg_21 = Refs.interpreterRegs[21];
        reg_22 = Refs.interpreterRegs[22];
        reg_23 = Refs.interpreterRegs[23];
        reg_24 = Refs.interpreterRegs[24];
        reg_25 = Refs.interpreterRegs[25];
        reg_26 = Refs.interpreterRegs[26];
        reg_27 = Refs.interpreterRegs[27];
        reg_28 = Refs.interpreterRegs[28];
        reg_29 = Refs.interpreterRegs[29];
        reg_30 = Refs.interpreterRegs[30];
        reg_31 = Refs.interpreterRegs[31];
//        System.out.println("i->c");
        ownRegs = true;
    }

    public void restoreInterpreterState() {
        compilerToInterpreter();
    }

    public static final String COMPILER_TO_INTERPRETER_METHOD = "compilerToInterpreter";

    public static void compilerToInterpreter() {
        if (ownRegs) {
//            System.out.println("c->i");
            ownRegs = false;
            Refs.interpreterRegs[1] = reg_1;
            Refs.interpreterRegs[2] = reg_2;
            Refs.interpreterRegs[3] = reg_3;
            Refs.interpreterRegs[4] = reg_4;
            Refs.interpreterRegs[5] = reg_5;
            Refs.interpreterRegs[6] = reg_6;
            Refs.interpreterRegs[7] = reg_7;
            Refs.interpreterRegs[8] = reg_8;
            Refs.interpreterRegs[9] = reg_9;
            Refs.interpreterRegs[10] = reg_10;
            Refs.interpreterRegs[11] = reg_11;
            Refs.interpreterRegs[12] = reg_12;
            Refs.interpreterRegs[13] = reg_13;
            Refs.interpreterRegs[14] = reg_14;
            Refs.interpreterRegs[15] = reg_15;
            Refs.interpreterRegs[16] = reg_16;
            Refs.interpreterRegs[17] = reg_17;
            Refs.interpreterRegs[18] = reg_18;
            Refs.interpreterRegs[19] = reg_19;
            Refs.interpreterRegs[20] = reg_20;
            Refs.interpreterRegs[21] = reg_21;
            Refs.interpreterRegs[22] = reg_22;
            Refs.interpreterRegs[23] = reg_23;
            Refs.interpreterRegs[24] = reg_24;
            Refs.interpreterRegs[25] = reg_25;
            Refs.interpreterRegs[26] = reg_26;
            Refs.interpreterRegs[27] = reg_27;
            Refs.interpreterRegs[28] = reg_28;
            Refs.interpreterRegs[29] = reg_29;
            Refs.interpreterRegs[30] = reg_30;
            Refs.interpreterRegs[31] = reg_31;
        }
    }

    /**
     * called by compiled code, when it detects
     * that isInterrupted is true
     */
    public static final String INTERRUPTED_METHOD = "c_interrupted";

    public static void c_interrupted(int address) {
        r3000.setPC(address);
        isInterrupted = false;
        r3000.compilerInterrupted();
        // check to see if we should return to the interpreter
        if (context.cacheStale) {
            context.cacheStale = false;
            throw ContinueExecutionException.DONT_SKIP_CURRENT;
        }
        if (!ownRegs) {
            interpreterToCompiler();
        }
    }

    /**
     * called by compiled code when it executes
     * an instruction whose side effects are unknown
     */
    public static final String SAEF_RETURN_METHOD = "c_safe_return";

    public static void c_safe_return() {
        if (!ownRegs) {
            interpreterToCompiler();
        }
    }

    public static final String JUMP_METHOD = "c_jump";

    public static int c_jump(int address, int returnAddress) {
        context.nativeDepth++;
        if (context.nativeDepth > Settings.maxNativeDepth) {
            log.info("STACK OVERFLOW; COLLAPSING...");
            returnToInterpreter(address);
        }
        //if (Debug.traceExecutionFlow) {
        //    System.out.println("Indirect compiled jump "+MiscUtil.toHex( address,8)+" with retaddr "+MiscUtil.toHex( returnAddress, 8));
        //}
        CodeUnit unit = getCodeUnit(address);

        Executable exec = unit.getExecutable();
        if (exec == null) {
            exec = makeExecutable(unit);
            if (exec == null) {
                returnToInterpreter(address);
            }
        }
        int rc = exec.e(returnAddress, true);
        context.nativeDepth--;
        return rc;
    }

    public static final String CALL_METHOD = "c_call";

    public static void c_call(int address, int returnAddress) {
        context.nativeDepth++;
        if (context.nativeDepth > Settings.maxNativeDepth) {
            log.info("STACK OVERFLOW; COLLAPSING...");
            returnToInterpreter(address);
        }
        //if (Debug.traceExecutionFlow) {
        //    System.out.println("Indirect compiled jump "+MiscUtil.toHex( address,8)+" with retaddr "+MiscUtil.toHex( returnAddress, 8));
        //}
        CodeUnit unit = getCodeUnit(address);

        Executable exec = unit.getExecutable();
        if (exec == null) {
            exec = makeExecutable(unit);
            if (exec == null) {
                returnToInterpreter(address);
            }
        }
        int rc = exec.e(returnAddress, false);
        assert (rc == returnAddress) : "call to unit should always return to the right spot";
        context.nativeDepth--;
    }

    public static void traceEnterUnit(int address, int returnAddress, boolean jump) {
        System.out.println("Entering compiled unit " + MiscUtil.toHex(address, 8) + " retaddr=" + MiscUtil.toHex(returnAddress, 8) + " jump=" + jump + " r31=" + MiscUtil.toHex(reg_31, 8));
    }

    public static void traceCheckJumpTarget(int target, int returnAddress) {
        System.out.println("Check jump target " + MiscUtil.toHex(target, 8) + ", retaddr " + MiscUtil.toHex(returnAddress, 8));
    }

    public static void traceDirectCall(int address, int returnAddress) {
        System.out.println("Direct compiled call " + MiscUtil.toHex(address, 8) + " with retaddr " + MiscUtil.toHex(returnAddress, 8));
    }

    public static void traceDirectJump(int address, int returnAddress) {
        System.out.println("Direct compiled jump " + MiscUtil.toHex(address, 8) + " with retaddr " + MiscUtil.toHex(returnAddress, 8));
    }

    public static void traceLeaveUnit(int address) {
        System.out.println("Leaving compiled unit " + MiscUtil.toHex(address, 8));
    }

    /**
     * If we got here, then we are trying to return to the interpreter due to an exception
     * in stage2 compiled code, but the register values aren't all correct since the stage2
     * compiler deferred writing the values back until later and didn't expect to need the
     * value at this point; this is a rare occurrence, and as such we don't mind taking the
     * hit, which involves going back and figuring out what registers have constant values at this point.
     * <p/>
     * Note that being able to do this relies on the fact that we only omit writing back registers
     * whose values are constant; not that we calculate at run time
     *
     * @param pc
     */
    private void fixupUnwrittenCompilerRegs(int base, int pc) {
        if (fixupStage2Generator == null) {
            fixupStage2Generator = new Stage2Generator("c2fixup.out");
        }
        CodeUnit unit = getCodeUnit(base);
        assert unit.useStage2;
        fixupStage2Generator.fixupUnwrittenRegs(unit, pc);
    }
}

// when we execute a class for the first time, we want to follow all links and make sure we have a JavaClass
// available for large methods (note they may have been destroyed)...


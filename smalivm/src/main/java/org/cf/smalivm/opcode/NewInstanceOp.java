package org.cf.smalivm.opcode;

import org.cf.smalivm.SideEffect;
import org.cf.smalivm.VirtualMachine;
import org.cf.smalivm.context.ExecutionContext;
import org.cf.smalivm.context.ExecutionNode;
import org.cf.smalivm.context.HeapItem;
import org.cf.smalivm.context.MethodState;
import org.cf.smalivm.type.UninitializedInstance;
import org.jf.dexlib2.builder.MethodLocation;

public class NewInstanceOp extends ExecutionContextOp {

    private final String className;
    private final int destRegister;
    private final VirtualMachine vm;
    private SideEffect.Level sideEffectLevel;

    NewInstanceOp(MethodLocation location, MethodLocation child, int destRegister, String className, VirtualMachine vm) {
        super(location, child);

        this.destRegister = destRegister;
        this.className = className;
        this.vm = vm;
        sideEffectLevel = SideEffect.Level.STRONG;
    }

    @Override
    public void execute(ExecutionNode node, ExecutionContext ectx) {
        Object instance = new UninitializedInstance(className);
        if (vm.shouldTreatAsLocal(className)) {
            // New-instance causes static initialization (but not new-array!)
            ectx.readClassState(className); // access will initialize if necessary
            sideEffectLevel = ectx.getClassSideEffectLevel(className);
        } else if (vm.getConfiguration().isSafe(className)) {
            sideEffectLevel = SideEffect.Level.NONE;
        }

        MethodState mState = ectx.getMethodState();
        HeapItem instanceItem = new HeapItem(instance, className);
        mState.assignRegister(destRegister, instanceItem);
    }

    @Override
    public SideEffect.Level getSideEffectLevel() {
        return sideEffectLevel;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getName());
        sb.append(" r").append(destRegister).append(", ").append(className);

        return sb.toString();
    }

}

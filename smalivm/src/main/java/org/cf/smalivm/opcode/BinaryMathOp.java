package org.cf.smalivm.opcode;

import javax.annotation.Nonnull;

import org.cf.smalivm.VirtualException;
import org.cf.smalivm.context.ExecutionNode;
import org.cf.smalivm.context.HeapItem;
import org.cf.smalivm.context.MethodState;
import org.cf.smalivm.type.UnknownValue;
import org.cf.util.Utils;
import org.jf.dexlib2.builder.MethodLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BinaryMathOp extends MethodStateOp {

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(BinaryMathOp.class.getSimpleName());

    private static enum MathOperandType {
        DOUBLE("D"), FLOAT("F"), INT("I"), LONG("J"), ;

        private final String type;

        MathOperandType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    };

    private static enum MathOperator {
        ADD, AND, DIV, MUL, OR, REM, RSUB, SHL, SHR, SUB, USHR, XOR,
    };

    private final int arg1Register;
    private int arg2Register;
    private final int destRegister;
    private boolean hasLiteral;
    private final MathOperandType mathOperandType;
    private final MathOperator mathOperator;

    private int narrowLiteral;

    BinaryMathOp(MethodLocation location, MethodLocation child, int destRegister, int arg1Register, int otherValue,
                    boolean hasLiteral) {
        this(location, child, destRegister, arg1Register);

        this.hasLiteral = hasLiteral;
        if (hasLiteral) {
            narrowLiteral = otherValue;
        } else {
            arg2Register = otherValue;
        }
    }

    private BinaryMathOp(MethodLocation location, MethodLocation child, int destRegister, int arg1Register) {
        super(location, child);

        this.destRegister = destRegister;
        this.arg1Register = arg1Register;
        mathOperator = getMathOp(getName());
        mathOperandType = getMathOperandType(getName());

        addException(new VirtualException(ArithmeticException.class, "/ by zero"));
    }

    @Override
    public void execute(ExecutionNode node, MethodState mState) {
        HeapItem lhsItem = mState.readRegister(arg1Register);
        HeapItem rhsItem = null;
        if (hasLiteral) {
            rhsItem = new HeapItem(narrowLiteral, "I");
        } else {
            rhsItem = mState.readRegister(arg2Register);
        }

        Object result = null;
        if (!lhsItem.isUnknown() && !rhsItem.isUnknown()) {
            result = getResult(lhsItem.getValue(), rhsItem.getValue());
            if (result instanceof VirtualException) {
                node.setException((VirtualException) result);
                node.clearChildren();
                return;
            } else {
                node.clearExceptions();
            }
        }

        if (null == result) {
            result = new UnknownValue();
        }

        mState.assignRegister(destRegister, result, mathOperandType.getType());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getName());
        sb.append(" r").append(destRegister).append(", r");

        boolean is2Addr = getName().endsWith("/2addr");
        if (is2Addr) {
            sb.append(arg2Register);
        } else {
            sb.append(arg1Register);
            if (hasLiteral) {
                sb.append(", 0x").append(Integer.toHexString(narrowLiteral));
            } else {
                sb.append(", r").append(arg2Register);
            }
        }

        return sb.toString();
    }

    private @Nonnull Object getResult(Object lhs, Object rhs) {
        Object result = null;
        switch (mathOperandType) {
        case INT:
            lhs = Utils.getIntegerValue(lhs);
            rhs = Utils.getIntegerValue(rhs);
            result = doIntegerOperation(mathOperator, (Integer) lhs, (Integer) rhs);
            break;
        case LONG:
            lhs = Utils.getLongValue(lhs);
            rhs = Utils.getLongValue(rhs);
            result = doLongOperation(mathOperator, (Long) lhs, (Long) rhs);
            break;
        case FLOAT:
            lhs = Utils.getFloatValue(lhs);
            rhs = Utils.getFloatValue(rhs);
            result = doFloatOperation(mathOperator, (Float) lhs, (Float) rhs);
            break;
        case DOUBLE:
            lhs = Utils.getDoubleValue(lhs);
            rhs = Utils.getDoubleValue(rhs);
            result = doDoubleOperation(mathOperator, (Double) lhs, (Double) rhs);
            break;
        default:
            throw new RuntimeException("Unknown math operand type!");
        }

        return result;
    }

    @SuppressWarnings("incomplete-switch")
    private static Object doDoubleOperation(MathOperator mathOperator, Double lhs, Double rhs) {
        Object result = null;
        switch (mathOperator) {
        case ADD:
            result = lhs + rhs;
            break;
        case DIV:
            result = lhs / rhs;
            break;
        case MUL:
            result = lhs * rhs;
            break;
        case REM:
            result = lhs % rhs;
            break;
        case SUB:
            result = lhs - rhs;
            break;
        }

        return result;
    }

    @SuppressWarnings("incomplete-switch")
    private static Object doFloatOperation(MathOperator mathOperator, Float lhs, Float rhs) {
        Object result = null;
        switch (mathOperator) {
        case ADD:
            result = lhs + rhs;
            break;
        case DIV:
            result = lhs / rhs;
            break;
        case MUL:
            result = lhs * rhs;
            break;
        case REM:
            result = lhs % rhs;
            break;
        case SUB:
            result = lhs - rhs;
            break;
        }

        return result;
    }

    private static Object doIntegerOperation(MathOperator mathOperator, Integer lhs, Integer rhs) {
        Object result = null;
        try {
            switch (mathOperator) {
            case ADD:
                result = lhs + rhs;
                break;
            case AND:
                result = lhs & rhs;
                break;
            case DIV:
                result = lhs / rhs;
                break;
            case MUL:
                result = lhs * rhs;
                break;
            case OR:
                result = lhs | rhs;
                break;
            case REM:
                result = lhs % rhs;
                break;
            case RSUB:
                result = rhs - lhs;
                break;
            case SHL:
                result = lhs << (rhs & 0x1f);
                break;
            case SHR:
                result = lhs >> (rhs & 0x1f);
                break;
            case SUB:
                result = lhs - rhs;
                break;
            case USHR:
                result = lhs >>> (rhs & 0x1f);
                break;
            case XOR:
                result = lhs ^ rhs;
                break;
            }
        } catch (ArithmeticException e) {
            return new VirtualException(e);
        }

        return result;
    }

    @SuppressWarnings("incomplete-switch")
    private static Object doLongOperation(MathOperator mathOperator, Long lhs, Long rhs) {
        Object result = null;
        try {
            switch (mathOperator) {
            case ADD:
                result = lhs + rhs;
                break;
            case AND:
                result = lhs & rhs;
                break;
            case DIV:
                result = lhs / rhs;
                break;
            case MUL:
                result = lhs * rhs;
                break;
            case OR:
                result = lhs | rhs;
                break;
            case REM:
                result = lhs % rhs;
                break;
            case SHL:
                result = lhs << rhs;
                break;
            case SHR:
                result = lhs >> rhs;
                break;
            case SUB:
                result = lhs - rhs;
                break;
            case USHR:
                result = lhs >>> rhs;
                break;
            case XOR:
                result = lhs ^ rhs;
                break;
            }
        } catch (ArithmeticException e) {
            VirtualException exception = new VirtualException(ArithmeticException.class, e.getMessage());
            return exception;
        }

        return result;
    }

    private static MathOperator getMathOp(String opName) {
        MathOperator result = null;
        if (opName.startsWith("add")) {
            result = MathOperator.ADD;
        } else if (opName.startsWith("sub")) {
            result = MathOperator.SUB;
        } else if (opName.startsWith("mul")) {
            result = MathOperator.MUL;
        } else if (opName.startsWith("div")) {
            result = MathOperator.DIV;
        } else if (opName.startsWith("rem")) {
            result = MathOperator.REM;
        } else if (opName.startsWith("and")) {
            result = MathOperator.AND;
        } else if (opName.startsWith("or")) {
            result = MathOperator.OR;
        } else if (opName.startsWith("xor")) {
            result = MathOperator.XOR;
        } else if (opName.startsWith("shl")) {
            result = MathOperator.SHL;
        } else if (opName.startsWith("shr")) {
            result = MathOperator.SHR;
        } else if (opName.startsWith("ushr")) {
            result = MathOperator.USHR;
        } else if (opName.startsWith("rsub")) {
            result = MathOperator.RSUB;
        }

        return result;
    }

    private static MathOperandType getMathOperandType(String opName) {
        MathOperandType result = null;
        if (opName.contains("-int")) {
            result = MathOperandType.INT;
        } else if (opName.contains("-double")) {
            result = MathOperandType.DOUBLE;
        } else if (opName.contains("-float")) {
            result = MathOperandType.FLOAT;
        } else if (opName.contains("-long")) {
            result = MathOperandType.LONG;
        }

        return result;
    }

}

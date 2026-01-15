package intermediate;

public class Instruction {

    public enum OpCode {
        PUSH,
        POP,

        LOAD,
        STORE,
        ALOAD,
        ASTORE,

        ADD,
        SUB,
        MUL,
        DIV,
        MOD,
        NEG,

        EQ,
        NEQ,
        LT,
        LE,
        GT,
        GE,

        AND,
        OR,
        NOT,

        JMP,
        JZ,
        JNZ,
        LABEL,

        CALL,
        RET,

        PRINT,
        READ,

        CAST_TO_INT,
        CAST_TO_DOUBLE,

        HALT
    }

    public final OpCode opCode;
    public final Object operand;
    public final Object operand2;

    private int address = -1;

    public Instruction(OpCode opCode) {
        this(opCode, null, null);
    }

    public void setAddress(int address) {
        this.address = address;
    }

    public int getAddress() {
        return address;
    }

    public Instruction(OpCode opCode, Object operand) {
        this(opCode, operand, null);
    }

    public Instruction(OpCode opCode, Object operand, Object operand2) {
        this.opCode = opCode;
        this.operand = operand;
        this.operand2 = operand2;
    }

    @Override
    public String toString() {
        return toString(false, null);
    }

    public String toString(boolean showAddress, java.util.Map<String, Integer> labelAddresses) {
        StringBuilder sb = new StringBuilder();

        if (showAddress && address >= 0) {
            sb.append(String.format("%04d: ", address));
        }

        if (opCode == OpCode.LABEL) {
            sb.append(operand).append(":");
        } else {
            sb.append("    ").append(opCode.name().toLowerCase());
            if (operand != null) {
                if (labelAddresses != null && isJumpInstruction() && operand instanceof String) {
                    Integer targetAddr = labelAddresses.get(operand);
                    if (targetAddr != null) {
                        sb.append(" ").append(operand).append(" [→").append(String.format("%04d", targetAddr)).append("]");
                    } else {
                        sb.append(" ").append(operand);
                    }
                } else {
                    sb.append(" ").append(operand);
                }
            }
            if (operand2 != null) {
                sb.append(", ").append(operand2);
            }
        }

        return sb.toString();
    }

    public boolean isJumpInstruction() {
        return opCode == OpCode.JMP || opCode == OpCode.JZ || opCode == OpCode.JNZ;
    }
}

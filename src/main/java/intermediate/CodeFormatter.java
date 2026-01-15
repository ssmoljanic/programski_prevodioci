package intermediate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CodeFormatter {

    private final List<Instruction> instructions;
    private final Map<String, Integer> labelAddresses = new HashMap<>();
    private int ip = 0;

    public CodeFormatter(List<Instruction> instructions) {
        this.instructions = instructions;
        assignAddresses();
    }

    private void assignAddresses() {
        ip = 0;

        for (Instruction instr : instructions) {
            instr.setAddress(ip);

            if (instr.opCode == Instruction.OpCode.LABEL) {
                labelAddresses.put((String) instr.operand, ip);
            }

            ip++;
        }
    }

    public Map<String, Integer> getLabelAddresses() {
        return labelAddresses;
    }

    public int getInstructionCount() {
        return ip;
    }

    public String format() {
        StringBuilder sb = new StringBuilder();

        sb.append("===== MEDJUKOD SA IP ADRESAMA =====\n");
        sb.append("Ukupno instrukcija: ").append(ip).append("\n");
        sb.append("===================================\n\n");

        for (Instruction instr : instructions) {
            sb.append(instr.toString(true, labelAddresses));
            sb.append("\n");
        }

        return sb.toString();
    }

    public String formatSimple() {
        StringBuilder sb = new StringBuilder();

        for (Instruction instr : instructions) {
            sb.append(instr.toString(true, labelAddresses));
            sb.append("\n");
        }

        return sb.toString();
    }

    public String formatLabelTable() {
        StringBuilder sb = new StringBuilder();

        sb.append("===== TABELA LABELA =====\n");
        sb.append("LABELA                  ADRESA\n");
        sb.append("-----------------------------\n");

        for (Map.Entry<String, Integer> entry : labelAddresses.entrySet()) {
            sb.append(String.format("%-24s%04d\n", entry.getKey(), entry.getValue()));
        }

        sb.append("=============================\n");

        return sb.toString();
    }
}

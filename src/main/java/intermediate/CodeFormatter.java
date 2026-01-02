package intermediate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Formatira međukod sa IP (Instruction Pointer) adresama.
 *
 * Dodeljuje svakoj instrukciji jedinstvenu adresu i
 * resolvira labele u njihove numeričke adrese.
 */
public class CodeFormatter {

    private final List<Instruction> instructions;
    private final Map<String, Integer> labelAddresses = new HashMap<>();
    private int ip = 0;  // Instruction Pointer

    public CodeFormatter(List<Instruction> instructions) {
        this.instructions = instructions;
        assignAddresses();
    }

    /**
     * Dodeljuje IP adrese svim instrukcijama i mapira labele.
     */
    private void assignAddresses() {
        ip = 0;

        for (Instruction instr : instructions) {
            instr.setAddress(ip);

            // Ako je labela, zapamti njenu adresu
            if (instr.opCode == Instruction.OpCode.LABEL) {
                labelAddresses.put((String) instr.operand, ip);
            }

            ip++;
        }
    }

    /**
     * Vraća mapu labela -> adresa.
     */
    public Map<String, Integer> getLabelAddresses() {
        return labelAddresses;
    }

    /**
     * Vraća ukupan broj instrukcija.
     */
    public int getInstructionCount() {
        return ip;
    }

    /**
     * Formatira sve instrukcije sa IP adresama.
     */
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

    /**
     * Formatira instrukcije u jednostavnom formatu (bez okvira).
     */
    public String formatSimple() {
        StringBuilder sb = new StringBuilder();

        for (Instruction instr : instructions) {
            sb.append(instr.toString(true, labelAddresses));
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Vraća tabelu labela sa adresama.
     */
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

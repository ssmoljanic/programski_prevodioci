package intermediate;

/**
 * Reprezentacija jedne instrukcije međukoda.
 *
 * Stek mašina koristi sledeće operacije:
 * - PUSH vrednost: stavlja vrednost na stek
 * - POP: skida vrednost sa steka
 * - LOAD ime: učitava vrednost promenljive na stek
 * - STORE ime: skida vrednost sa steka i čuva u promenljivu
 * - ADD, SUB, MUL, DIV, MOD: aritmetičke operacije (skidaju 2, stavljaju 1)
 * - EQ, NEQ, LT, LE, GT, GE: relacione operacije
 * - AND, OR: logičke operacije (skidaju 2, stavljaju 1)
 * - NOT: logička negacija (skida 1, stavlja 1)
 * - NEG: aritmetička negacija
 * - JMP labela: bezuslovni skok
 * - JZ labela: skok ako je vrh steka 0 (false)
 * - JNZ labela: skok ako vrh nije 0 (true)
 * - LABEL labela: definicija labele
 * - CALL ime, argc: poziv funkcije sa argc argumenata
 * - RET: povratak iz funkcije
 * - PRINT: štampa vrednost sa vrha steka
 * - READ ime: učitava vrednost sa ulaza u promenljivu
 * - CAST_TO_INT: konvertuje double u int
 * - CAST_TO_DOUBLE: konvertuje int u double
 * - HALT: zaustavljanje programa
 */
public class Instruction {

    public enum OpCode {
        // Stek operacije
        PUSH,           // push vrednost
        POP,            // pop (odbacuje vrh)

        // Promenljive
        LOAD,           // load ime - učitava promenljivu na stek
        STORE,          // store ime - čuva vrh steka u promenljivu
        ALOAD,          // aload ime, dimenzija - učitava element niza
        ASTORE,         // astore ime, dimenzija - čuva u element niza

        // Aritmetičke operacije
        ADD,            // sabiranje
        SUB,            // oduzimanje
        MUL,            // množenje
        DIV,            // deljenje
        MOD,            // ostatak pri deljenju
        NEG,            // negacija (-x)

        // Relacione operacije
        EQ,             // jednako
        NEQ,            // različito
        LT,             // manje
        LE,             // manje ili jednako
        GT,             // veće
        GE,             // veće ili jednako

        // Logičke operacije
        AND,            // logičko i
        OR,             // logičko ili
        NOT,            // logička negacija

        // Kontrola toka
        JMP,            // bezuslovni skok
        JZ,             // skok ako je 0 (false)
        JNZ,            // skok ako nije 0 (true)
        LABEL,          // definicija labele

        // Funkcije
        CALL,           // poziv funkcije
        RET,            // povratak iz funkcije

        // Ulaz/izlaz
        PRINT,          // štampanje
        READ,           // čitanje

        // Konverzije tipova
        CAST_TO_INT,    // double -> int
        CAST_TO_DOUBLE, // int -> double

        // Kontrola programa
        HALT            // kraj programa
    }

    public final OpCode opCode;
    public final Object operand;    // vrednost, ime promenljive, ili labela
    public final Object operand2;   // drugi operand (za CALL - broj argumenata)

    // IP (Instruction Pointer) - adresa instrukcije u programu
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

    /**
     * Formatira instrukciju sa opcionalnom IP adresom.
     * @param showAddress da li da prikaže IP adresu
     * @param labelAddresses mapa labela -> adresa (za resoluciju skokova)
     */
    public String toString(boolean showAddress, java.util.Map<String, Integer> labelAddresses) {
        StringBuilder sb = new StringBuilder();

        // IP adresa (4 cifre)
        if (showAddress && address >= 0) {
            sb.append(String.format("%04d: ", address));
        }

        if (opCode == OpCode.LABEL) {
            sb.append(operand).append(":");
        } else {
            sb.append("    ").append(opCode.name().toLowerCase());
            if (operand != null) {
                // Za JMP, JZ, JNZ - prikaži i adresu labele ako je dostupna
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

    /**
     * Da li je ovo instrukcija skoka?
     */
    public boolean isJumpInstruction() {
        return opCode == OpCode.JMP || opCode == OpCode.JZ || opCode == OpCode.JNZ;
    }
}

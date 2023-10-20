import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

  static byte[] ram = new byte[128 * 1024];
  static byte programCounter;
  static byte aRegister;
  static byte bRegister;
  static byte outputRegister;
  static byte instructionRegister;
  static byte bus;
  static byte memoryAddressRegister;

  public static void main(String[] args) {
    loadProgram();

    while (instructionRegister != HLT) {
      System.out.println();
      System.out.println("#####################");
      System.out.printf("      LINE %d\n", programCounter);
      System.out.println("#####################");
      for (int microStep = 0; microStep < OPCODES_MICROS[0].length; microStep++) {
        System.out.println();
        System.out.printf("___STEP %d___\n", microStep);
        int[] opcode = OPCODES_MICROS[instructionRegister];
        if (opcode[microStep] == 0) break;
        for (int microcode : MICROCODES) {
          if ((opcode[microStep] & microcode) > 0) {
            commands.get(microcode).run();
          }
        }
        bus = 0;
      }
    }
  }

  private static void loadProgram() {
    byte[] program = {
      LDA, 6,     //0
      ADD, 7,     //2
      OUT,        //4
      HLT,        //5
      28,
      14
    };

    System.arraycopy(program, 0, ram, 0, program.length);
  }

  static int HLTM = 0b1000000000000000;// Halt clock                   // PIN 17 - IO7 - 1000_0000_0000_0000 - 80
  static int MI = 0b0100000000000000;  // Memory address register in   // PIN 16 - IO6 - 0100_0000_0000_0000 - 40
  static int RI = 0b0010000000000000;  // RAM data in                  // PIN 15 - IO5 - 0010_0000_0000_0000 - 20
  static int RO = 0b0001000000000000;  // RAM data out                 // PIN 14 - IO4 - 0001_0000_0000_0000 - 10
  static int NN = 0b0000100000000000;  // Not used                     // PIN 13 - IO3
  static int II = 0b0000010000000000;  // Instruction register in      // PIN 11 - IO2 - 0000_0100_0000_0000 - 04
  static int AI = 0b0000001000000000;  // A register in                // PIN 10 - IO1 - 0000_0010_0000_0000 - 02
  static int AO = 0b0000000100000000;  // A register out               // PIN 9  - IO0 - 0000_0001_0000_0000 - 01 - next goes after address 080
  static int EO = 0b0000000010000000;  // ALU out                      // PIN 17 - IO7 - 0000_0000_1000_0000 - 80
  static int SU = 0b0000000001000000;  // ALU subtract                 // PIN 16 - IO6 - 0000_0000_0100_0000 - 40
  static int BI = 0b0000000000100000;  // B register in                // PIN 15 - IO5 - 0000_0000_0010_0000 - 20
  static int OI = 0b0000000000010000;  // Output register in           // PIN 14 - IO4 - 0000_0000_0001_0000 - 10
  static int CE = 0b0000000000001000;  // Program counter enable       // PIN 13 - IO3 - 0000_0000_0000_1000 - 08
  static int CO = 0b0000000000000100;  // Program counter out          // PIN 11 - IO2 - 0000_0000_0000_0100 - 04
  static int J =  0b0000000000000010;  // Jump (program counter in)    // PIN 10 - IO1 - 0000_0000_0000_0010 - 02
  static int RS = 0b0000000000000001;  // Microcode counter reset      // PIN 9  - IO0 - 0000_0000_0000_0001 - 01

  static Map<Integer, Runnable> commands = new HashMap<>() {{
    put(HLTM, () -> System.out.printf("Program ended! Out: %d\n", outputRegister));
    put(MI, () -> {
      memoryAddressRegister = bus;
      System.out.printf("bus -> %d -> memory address register\n", bus);
    });
    put(RI, () -> {
      ram[memoryAddressRegister] = bus;
      System.out.printf("bus -> %d -> RAM[%d]\n", bus, memoryAddressRegister);
    });
    put(RO, () -> {
      bus = ram[memoryAddressRegister];
      System.out.printf("ram[%d] -> %d -> bus\n", memoryAddressRegister, bus);
    });
    put(NN, () -> System.out.println("noop"));
    put(II, () -> {
      instructionRegister = bus;
      System.out.printf("bus -> %d (%s) -> instruction register\n", bus, opcodeLabels.get(bus));
    });
    put(AI, () -> {
      aRegister = bus;
      System.out.printf("bus -> %d -> A register\n", bus);
    });
    put(AO, () -> {
      bus = aRegister;
      System.out.printf("A register -> %d -> bus\n", bus);
    });
    put(EO, () -> {
      bus = (byte) (aRegister + bRegister);
      System.out.printf("A + B -> %d -> bus\n", bus);
    });
    put(SU, () -> {
      bus = (byte) (aRegister - bRegister);
      System.out.printf("A - B -> %d -> bus\n", bus);
    });
    put(BI, () -> {
      bRegister = bus;
      System.out.printf("bus -> %d -> B register\n", bus);
    });
    put(OI, () -> {
      outputRegister = bus;
      System.out.printf("bus -> %d -> out\n", bus);
    });
    put(CE, () -> {
      programCounter++;
      System.out.printf("program counter ++, now %d\n", programCounter);
    });
    put(CO, () -> {
      bus = programCounter;
      System.out.printf("program counter -> %d -> bus\n", bus);
    });
    put(J, () -> {
      programCounter = bus;
      System.out.printf("bus -> %d -> program counter\n", bus);
    });
    put(RS, () -> {
    });
  }};

  //Order of precedence is important, out should be before in
  static int[] MICROCODES = {RO, NN, AO, EO, CO, SU, BI, OI, CE, J, RS, MI, RI, II, AI, HLTM};

  static int[][] OPCODES_MICROS = {
    { CO|MI,  RO|II|CE,  RS,     0,        0,        0,        0,     0 },   // 0000 - NOP
    { CO|MI,  RO|II|CE,  CO|MI,  RO|MI|CE, RO|AI,    RS,       0,     0 },   // 0001 - LDA
    { CO|MI,  RO|II|CE,  CO|MI,  RO|MI|CE, RO|BI,    EO|AI,    RS,    0 },   // 0010 - ADD
    { CO|MI,  RO|II|CE,  CO|MI,  RO|MI|CE, RO|BI,    EO|AI|SU, RS,    0 },   // 0011 - SUB
    { CO|MI,  RO|II|CE,  CO|MI,  RO|MI|CE, AO|RI,    RS,       0,     0 },   // 0100 - STA
    { CO|MI,  RO|II|CE,  CO|MI,  RO|AI|CE, RS,       0,        0,     0 },   // 0101 - LDI
    { CO|MI,  RO|II|CE,  CO|MI,  RO|J,     RS,       0,        0,     0 },   // 0110 - JMP
    { CO|MI,  RO|II|CE,  CE,     RS,       0,        0,        0,     0 },   // 0111 - JC
    { CO|MI,  RO|II|CE,  CE,     RS,       0,        0,        0,     0 },   // 1000 - JZ
    { CO|MI,  RO|II|CE,  CO|MI,  RO|BI|CE, EO|SU|CE, RS,       0,     0 },   // 1001 - JEQ
    { CO|MI,  RO|II|CE,  0,      0,        0,        0,        0,     0 },   // 1010
    { CO|MI,  RO|II|CE,  0,      0,        0,        0,        0,     0 },   // 1011
    { CO|MI,  RO|II|CE,  0,      0,        0,        0,        0,     0 },   // 1100
    { CO|MI,  RO|II|CE,  0,      0,        0,        0,        0,     0 },   // 1101
    { CO|MI,  RO|II|CE,  AO|OI,  RS,       0,        0,        0,     0 },   // 1110 - OUT
    { CO|MI,  RO|II|CE,  HLTM,   0,        0,        0,        0,     0 },   // 1111 - HLT
  };

  static byte NOP = 0;
  static byte LDA = 1;
  static byte ADD = 2;
  static byte SUB = 3;
  static byte STA = 4;
  static byte LDI = 5;
  static byte JMP = 6;
  static byte JC = 7;
  static byte JZ = 8;
  static byte JEQ = 9;
  static byte OUT = 14;
  static byte HLT = 15;
  static List<String> opcodeLabels = new ArrayList<>() {{
    add("NOP");
    add("LDA");
    add("ADD");
    add("SUB");
    add("STA");
    add("LDI");
    add("JMP");
    add("JC");
    add("JZ");
    add("JEQ");
    add("NOP");
    add("NOP");
    add("NOP");
    add("NOP");
    add("OUT");
    add("HLT");
  }};
}
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//todo write tests
public class Main {
  static byte NOP = 0, LDA = 1, ADD = 2, SUB = 3, STA = 4, LDI = 5, JMP = 6, JC = 7, JZ = 8, JEQ = 9, ADI = 10, OUT = 14, HLT = 15;

  public static void main(String[] args) {
    init();

    //@todo use labels, to automatically map memory addresses
    //@change me

    List<String> testResults = new ArrayList<>();

    testResults.add(runTest(new byte[]{
      LDA, 6,
      ADD, 7,
      OUT,
      HLT,
      28,
      14
    }, 42, "Addition"));

    testResults.add(runTest(new byte[]{
      LDA, 6,
      SUB, 7,
      OUT,
      HLT,
      50,
      8
    }, 42, "Subtraction"));

    testResults.add(runTest(new byte[]{
      LDI, 1,
      STA, 15,
      LDI, 3,
      SUB, 15,
      JZ, 12,
      JMP, 6,
      OUT,
      HLT
    }, 0, "Jump zero"));

    testResults.add(runTest(new byte[]{
      ADD, 9,
      JEQ, 10, 7,
      JMP, 0,
      OUT,
      HLT,
      1,
      2
    }, 2, "Jump equal"));

    testResults.add(runTest(new byte[]{
      LDI, 126,
      ADD, 10,
      JC, 8,
      JMP, 2,
      OUT,
      HLT,
      1
    }, -128, "Jump carry"));

    testResults.add(runTest(new byte[]{
      LDI, 3,
      SUB, 8,
      JC, 2,
      OUT,
      HLT,
      1
    }, 0, "Jump carry on zero"));

    testResults.add(runTest(new byte[]{
      LDI, 24,
      ADI, 18,
      OUT,
      HLT
    }, 42, "Add immediate"));

    testResults.forEach(System.out::println);
  }

  public static byte run(byte[] program) {
    reset();
    loadProgram(program);

    while (instructionRegister != HLT) {
      printNextLineComment(programCounter);
      for (int microStep = 0; microStep < OPCODES[carryFlag | (zeroFlag<<1)][0].length; microStep++) {
        printNextMicrocodeStepComment(microStep);
        int[] opcode = OPCODES[carryFlag | (zeroFlag<<1)][instructionRegister];
        if (opcode[microStep] == 0) break;
        for (int microcode : MICROCODES) {
          if ((opcode[microStep] & microcode) > 0) {
            commands.get(microcode).run();
          }
        }
        bus = 0;
      }
    }

    return outputRegister;
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

  //Order of precedence is important, out should be before in
  static int[] MICROCODES = {RO, NN, AO, SU, EO, CO, BI, OI, CE, J, RS, MI, RI, II, AI, HLTM};

  static int FLAGS_Z0C0 = 0;
  static int FLAGS_Z0C1 = 1;
  static int FLAGS_Z1C0 = 2;
  static int FLAGS_Z1C1 = 3;

  static int[][][] OPCODES = new int[4][16][8];

  static byte[] ram = new byte[128 * 1024];

  //volatile state
  static byte programCounter;
  static byte aRegister;
  static byte bRegister;
  static byte outputRegister;
  static byte instructionRegister;
  static byte bus;
  static byte memoryAddressRegister;
  static int zeroFlag;
  static int carryFlag;
  static boolean isSubtract;
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
    add("ADI");
    add("NOP");
    add("NOP");
    add("NOP");
    add("OUT");
    add("HLT");
  }};

  //@todo verify all conditional jumps
  static Map<Integer, Runnable> commands = new HashMap<>() {{
    put(HLTM, () -> System.out.printf("Program ended on line %d\nout: %d\nA: %d\nB: %d\n", programCounter, outputRegister, aRegister, bRegister));
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
      System.out.printf("bus -> %d -> instruction register, executing %s\n", bus, opcodeLabels.get(bus));
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
      int sum;
      if (!isSubtract) {
        sum = aRegister + bRegister;
      } else {
        sum = aRegister - bRegister;
      }
      int newZeroFlag = sum == 0 ? 1 : 0;
      if (newZeroFlag != zeroFlag) {
        zeroFlag = newZeroFlag;
        System.out.printf("Zero flag set to %d\n", zeroFlag);
      }
      int newCarryFlag = isSubtract && sum != 0 || sum > Byte.MAX_VALUE ? 1 : 0;
      if (newCarryFlag != carryFlag) {
        carryFlag = newCarryFlag;
        System.out.printf("Carry flag set to %d\n", carryFlag);
      }
      if (sum < Byte.MIN_VALUE) sum += 256;
      if (sum > Byte.MAX_VALUE) sum -= 256;
      bus = (byte) sum;
      System.out.printf("A %s B -> %d -> bus\n", isSubtract ? "-" : "+", bus);
      isSubtract = false;
    });
    put(SU, () -> {
      isSubtract = true;
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

  private static void loadProgram(byte[] programToExecute) {
    System.arraycopy(programToExecute, 0, ram, 0, programToExecute.length);
  }

  private static void reset() {
    ram = new byte[128 * 1024];
    programCounter = 0;
    aRegister = 0;
    bRegister = 0;
    outputRegister = 0;
    instructionRegister = 0;
    bus = 0;
    memoryAddressRegister = 0;
    zeroFlag = 0;
    carryFlag = 0;
    isSubtract = false;
  }

  private static void init() {
    int [][] template = {
      { CO|MI,  RO|II|CE,  RS,     0,        0,        0,        0,     0 },   // 0000 - NOP
      { CO|MI,  RO|II|CE,  CO|MI,  RO|MI|CE, RO|AI,    RS,       0,     0 },   // 0001 - LDA
      { CO|MI,  RO|II|CE,  CO|MI,  RO|MI|CE, RO|BI,    EO|AI,    RS,    0 },   // 0010 - ADD
      { CO|MI,  RO|II|CE,  CO|MI,  RO|MI|CE, RO|BI,    EO|AI|SU, RS,    0 },   // 0011 - SUB
      { CO|MI,  RO|II|CE,  CO|MI,  RO|MI|CE, AO|RI,    RS,       0,     0 },   // 0100 - STA
      { CO|MI,  RO|II|CE,  CO|MI,  RO|AI|CE, RS,       0,        0,     0 },   // 0101 - LDI
      { CO|MI,  RO|II|CE,  CO|MI,  RO|J,     RS,       0,        0,     0 },   // 0110 - JMP
      { CO|MI,  RO|II|CE,  CE,     RS,       0,        0,        0,     0 },   // 0111 - JC
      { CO|MI,  RO|II|CE,  CE,     RS,       0,        0,        0,     0 },   // 1000 - JZ
      { CO|MI,  RO|II|CE,  CO|MI,  RO|MI|CE, RO|BI,    EO|SU,    CE,    0 },   // 1001 - JEQ
      { CO|MI,  RO|II|CE,  CO|MI,  RO|BI|CE, EO|AI,    RS,       0,     0 },   // 1010 - ADI
      { CO|MI,  RO|II|CE,  0,      0,        0,        0,        0,     0 },   // 1011
      { CO|MI,  RO|II|CE,  0,      0,        0,        0,        0,     0 },   // 1100
      { CO|MI,  RO|II|CE,  0,      0,        0,        0,        0,     0 },   // 1101
      { CO|MI,  RO|II|CE,  AO|OI,  RS,       0,        0,        0,     0 },   // 1110 - OUT
      { CO|MI,  RO|II|CE,  HLTM,   0,        0,        0,        0,     0 },   // 1111 - HLT
    };

    //zero flag = 0, carry flag = 0
    OPCODES[FLAGS_Z0C0] = deepCopy(template);

    //zero flag = 0, carry flag = 1
    OPCODES[FLAGS_Z0C1] = deepCopy(template);
    OPCODES[FLAGS_Z0C1][JC][2] = CO|MI;
    OPCODES[FLAGS_Z0C1][JC][3] = RO|J;
    OPCODES[FLAGS_Z0C1][JC][4] = RS;

    // ZF = 1, CF = 0
    OPCODES[FLAGS_Z1C0] = deepCopy(template);
    OPCODES[FLAGS_Z1C0][JZ][2] = CO|MI;
    OPCODES[FLAGS_Z1C0][JZ][3] = RO|J;
    OPCODES[FLAGS_Z1C0][JZ][4] = RS;

    OPCODES[FLAGS_Z1C0][JEQ][6] = CO|MI;
    OPCODES[FLAGS_Z1C0][JEQ][7] = RO|J|CE;

    // ZF = 1, CF = 1
    OPCODES[FLAGS_Z1C1] = deepCopy(template);
    OPCODES[FLAGS_Z1C1][JC][2] = CO|MI;
    OPCODES[FLAGS_Z1C1][JC][3] = RO|J;
    OPCODES[FLAGS_Z1C1][JC][4] = RS;

    OPCODES[FLAGS_Z1C1][JZ][2] = CO|MI;
    OPCODES[FLAGS_Z1C1][JZ][3] = RO|J;
    OPCODES[FLAGS_Z1C1][JZ][4] = RS;

    OPCODES[FLAGS_Z1C1][JEQ][6] = CO|MI;
    OPCODES[FLAGS_Z1C1][JEQ][7] = RO|J|CE;
  }

  private static int[][] deepCopy(int[][] a) {
    int[][] result = new int[a.length][];
    for (int i = 0; i < a.length; i++) {
      result[i] = a[i].clone();
    }
    return result;
  }

  private static void printNextLineComment(byte lineNumber) {
    System.out.println();
    System.out.println("#####################");
    System.out.printf("      LINE %d\n", programCounter);
    System.out.println("#####################");
  }

  private static void printNextMicrocodeStepComment(int step) {
    System.out.println();
    System.out.printf("___STEP %d___\n", step);
  }

  private static String runTest(byte[] program, int expectedOutput, String testName) {
    System.out.println("Running " + testName);
    return run(program) == (byte)expectedOutput ? "." : "\"" + testName + "\" test FAILED!";
  }
}
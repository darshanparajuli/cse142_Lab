package mips;

import java.io.PrintStream;
import java.util.ArrayList;

public class Program {

    private ArrayList<String> codeSegment;
    private ArrayList<String> dataSegment;

    private int labelCounter;

    public Program() {
        labelCounter = -1;
        codeSegment = new ArrayList<>();
        dataSegment = new ArrayList<>();
    }

    // Returns a unique label
    public String newLabel() {
        labelCounter++;
        return "label." + labelCounter;
    }

    public String newFuncLabel(String name) {
        if (name.equals("main")) {
            return "main";
        } else {
            return "func." + name;
        }
    }

    // Insert an instruction into the code segment
    // Returns the position of the instruction in the stream
    public int appendInstruction(String instr) {
        codeSegment.add(instr);
        return codeSegment.size() - 1;
    }

    // Replaces the instruction at position pos
    public void replaceInstruction(int pos, String instr) {
        codeSegment.set(pos, instr);
    }

    // Inserts an instruction at position pos
    // All instructions after pos are shifted down
    public void insertInstruction(int pos, String instr) {
        codeSegment.add(pos, instr);
    }

    // Append item to data segment
    public void appendData(String data) {
        dataSegment.add(data);
    }

    // Push an integer register on the stack
    public void pushInt(String reg) {
        codeSegment.add("subu $sp, $sp, 4");
        codeSegment.add(String.format("sw %s, 0($sp)", reg));
    }

    // Push a single precision floating point register on the stack
    public void pushFloat(String reg) {
        codeSegment.add("subu $sp, $sp, 4");
        codeSegment.add(String.format("s.s %s, 0($sp)", reg));
    }

    // Pop an integer from the stack into register reg
    public void popInt(String reg) {
        codeSegment.add(String.format("lw %s, 0($sp)", reg));
        codeSegment.add("addi $sp, $sp, 4");
    }

    // Pop a floating point value from the stack into register reg
    public void popFloat(String reg) {
        codeSegment.add(String.format("l.s %s, 0($sp)", reg));
        codeSegment.add("addi $sp, $sp, 4");
    }

    // Insert a function prologue at position pos
    public void insertPrologue(int pos, int frameSize) {
        final ArrayList<String> prologue = new ArrayList<>();
        prologue.add("subu $sp, $sp, 8");
        prologue.add("sw   $fp, 0($sp)");
        prologue.add("sw   $ra, 4($sp)");
        prologue.add("addi $fp, $sp, 8");
        prologue.add("subu $sp, $sp, " + frameSize);
        codeSegment.addAll(pos, prologue);
    }

    // Append a function epilogue
    public void appendEpilogue(int frameSize) {
        codeSegment.add("addu $sp, $sp, " + frameSize);
        codeSegment.add("lw   $ra, 4($sp)");
        codeSegment.add("lw   $fp, 0($sp)");
        codeSegment.add("addu $sp, $sp, 8");
        codeSegment.add("jr   $ra");
    }

    // Insert code that terminates the program
    public void appendExitSequence() {
        codeSegment.add("li    $v0, 10");
        codeSegment.add("syscall");
    }

    //Print the program to the provided stream
    public void print(PrintStream s) {
        s.println(".data                         # BEGIN Data Segment");
        for (String data : dataSegment)
            s.println(data);
        s.println("data.newline:      .asciiz       \"\\n\"");
        s.println("data.floatquery:   .asciiz       \"float?\"");
        s.println("data.intquery:     .asciiz       \"int?\"");
        s.println("data.trueString:   .asciiz       \"true\"");
        s.println("data.falseString:  .asciiz       \"false\"");
        s.println("                              # END Data Segment");

        s.println(".text                         # BEGIN Code Segment");
        // provide the built-in functions
        funcPrintBool(s);
        funcPrintFloat(s);
        funcPrintInt(s);
        funcPrintln(s);
        funcReadFloat(s);
        funcReadInt(s);

        s.println(".text                         # BEGIN Crux Program");
        // write out the crux program
        for (String code : codeSegment)
            s.println(code);
        s.println("                              # END Code Segment");
    }

    // Prints the current stack value, assuming it's an int
    public void funcPrintInt(PrintStream s) {
        s.println("func.printInt:");
        s.println("lw   $a0, 0($sp)");
        s.println("li   $v0, 1");
        s.println("syscall");
        s.println("jr $ra");
    }

    // Prints the current stack value assuming it's a bool
    public void funcPrintBool(PrintStream s) {
        s.println("func.printBool:");
        s.println("lw $a0, 0($sp)");
        s.println("beqz $a0, label.printBool.loadFalse");
        s.println("la $a0, data.trueString");
        s.println("j label.printBool.join");
        s.println("label.printBool.loadFalse:");
        s.println("la $a0, data.falseString");
        s.println("label.printBool.join:");
        s.println("li   $v0, 4");
        s.println("syscall");
        s.println("jr $ra");
    }

    // Prints the current stack value assuming it's a float
    private void funcPrintFloat(PrintStream s) {
        s.println("func.printFloat:");
        s.println("l.s  $f12, 0($sp)");
        s.println("li   $v0,  2");
        s.println("syscall");
        s.println("jr $ra");
    }

    // Prints a newline
    private void funcPrintln(PrintStream s) {
        s.println("func.println:");
        s.println("la   $a0, data.newline");
        s.println("li   $v0, 4");
        s.println("syscall");
        s.println("jr $ra");
    }

    // Reads an int onto the stack
    private void funcReadInt(PrintStream s) {
        s.println("func.readInt:");
        s.println("la   $a0, data.intquery");
        s.println("li   $v0, 4");
        s.println("syscall");
        s.println("li   $v0, 5");
        s.println("syscall");
        s.println("jr $ra");
    }

    // Reads a float onto the stack
    private void funcReadFloat(PrintStream s) {
        s.println("func.readFloat:");
        s.println("la   $a0, data.floatquery");
        s.println("li   $v0, 4");
        s.println("syscall");
        s.println("li   $v0, 6");
        s.println("syscall");
        s.println("mfc1 $v0, $f0");
        s.println("jr $ra");
    }
}

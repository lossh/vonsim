package vonsim.simulator

import scala.collection.mutable.ListBuffer
import scala.collection.mutable
import scala.util.Random
import com.sun.org.apache.bcel.internal.generic.ArithmeticInstruction
import Simulator._
import ComputerWord._

object Flags {

  def apply(d: DWord)={
    new Flags(d.bit(0).toBoolean, d.bit(1).toBoolean, d.bit(2).toBoolean, d.bit(3).toBoolean)
  }
}
class Flags(var c: Boolean = false, var s: Boolean = false, var o: Boolean = false, var z: Boolean = false) {

  def reset() {
    c = false
    s = false
    z = false
    o = false
  }

  def satisfy(cd: Condition) = {
    cd match {
      case JC  => c
      case JNC => !c
      case JO  => o
      case JNO => !o
      case JZ  => z
      case JNZ => !z
      case JS  => s
      case JNS => !s
    }
  }
    def toDWord() = {
      DWord(IndexedSeq(c.toInt, s.toInt, z.toInt, o.toInt).toInt())
    }

  
}

class ALU {
  var o1 = DWord()
  var o2 = DWord()
  var res = DWord()
  var op: ALUOp = CMP

  var flags = new Flags()

  def reset() { flags.reset() }

  def setOps(op: ALUOp, o1: Word, o2: Word) {
    this.op = op
    this.o1 = o1.toDWord()
    this.o2 = o2.toDWord()
  }
  def setOps(op: ALUOp, o: Word) {
    this.op = op
    this.o1 = o.toDWord()
    this.o2 = DWord()
  }
  def setOps(op: ALUOp, o1: DWord, o2: DWord) {
    this.op = op
    this.o1 = o1
    this.o2 = o2
  }
  def setOps(op: ALUOp, o: DWord) {
    this.op = op
    this.o1 = o
    this.o2 = DWord()
  }

  def applyOp(op: ALUOpUnary, o: DWord): DWord = {
    setOps(op, o)

    val (result, newFlags) = op match {
      case au: ArithmeticOpUnary => {
        //TODO
        (DWord(), new Flags())
      }
      case lu: LogicalOpUnary => {
        (DWord(), new Flags())
      }
    }
    this.res = result
    this.flags = newFlags
    result
  }

  def applyOp(op: ALUOpBinary, o1: DWord, o2: DWord): DWord = {
    setOps(op, o1, o2)

    val (result, newFlags) = op match {
      case ab: ArithmeticOpBinary => { arithmeticDWord(ab, o1, o2, flags.c.toInt) }
      case lb: LogicalOpBinary    => { logicalDWord(lb, o1, o2) }
    }

    flags = newFlags
    result
  }

  def applyOp(op: ALUOpBinary, o1: Word, o2: Word): Word = {
    setOps(op, o1, o2)

    val (result, newFlags) = op match {
      case ab: ArithmeticOpBinary => { arithmeticWord(ab, o1, o2, flags.c.toInt) }
      case lb: LogicalOpBinary    => { logicalWord(lb, o1, o2) }
    }

    flags = newFlags
    result
  }

  def applyOp(op: ALUOpUnary, o: Word): Word = {
    setOps(op, o)

    val (result, newFlags) = op match {
      case au: ArithmeticOpUnary => { arithmetic(au, o) }
      case lu: LogicalOpUnary    => { logical(lu, o) }
    }
    this.res = result.toDWord()
    this.flags = newFlags
    result
  }

  def applyOp(op: ArithmeticOpBinary, v: Int, w: Int, carry: Int) = {
    op match {
      case ADD => v + w
      case ADC => v + w + carry
      case SUB => v - w
      case SBB => v - w - carry
      case CMP => v - w
    }
  }
  def logical(op: LogicalOpUnary, w: Word): (Word, Flags) = {
    val result = op match {
      case NOT => (~w).toByte
      case NEG => (-w).toByte
    }
    (result, logicalFlags(result))
  }
  def logicalFlags(result: Int) = {
    val f = new Flags()
    f.o = false
    f.c = false
    f.z = result == 0
    f.s = result < 0
    f

  }
  def applyLogical(op: LogicalOpBinary, b1: Int, b2: Int) = op match {
    case OR  => b1 | b2
    case AND => b1 & b2
    case XOR => b1 ^ b2
  }

  def logicalDWord(op: LogicalOpBinary, w: DWord, v: DWord): (DWord, Flags) = {
    val (result, flags) = logical(op, w, v)
    (DWord(result), flags)
  }
  def logicalWord(op: LogicalOpBinary, w: Word, v: Word): (Word, Flags) = {
    val (result, flags) = logical(op, w, v)
    (Word(result), flags)
  }
  def logical(op: LogicalOpBinary, w: ComputerWord, v: ComputerWord): (Int, Flags) = {

    val result = (w.toBits.zip(v.toBits()) map { case (b1, b2) => applyLogical(op, b1, b2) }).toInt
    (result, logicalFlags(result))
  }

  def arithmeticWord(op: ArithmeticOpBinary, w: Word, v: Word, carry: Int = 0): (Word, Flags) = {
    val (res, f) = arithmetic(op, w, v, carry)
    (Word(res), f)
  }
  def arithmetic(op: ArithmeticOpBinary, w: ComputerWord, v: ComputerWord, carry: Int = 0): (Int, Flags) = {
    val f = new Flags()

    var res = applyOp(op, w.toInt, v.toInt, carry)
    var unsignedRes = applyOp(op, w.toUnsignedInt, v.toUnsignedInt, carry)

    if (res < w.minSigned) {
      f.o = true
      res += w.numbers
    }
    if (res > w.maxSigned) {
      f.o = true
      res -= w.numbers
    }

    if (unsignedRes > w.maxUnsigned || unsignedRes < 0) {
      f.c = true
    }

    f.s = res < 0
    f.z = res == 0
    (res, f)
  }
  def arithmeticDWord(op: ArithmeticOpBinary, w: DWord, v: DWord, carry: Int = 0): (DWord, Flags) = {
    val (res, f) = arithmetic(op, w, v, carry)
    (DWord(res), f)
  }

  def arithmetic(op: ArithmeticOpUnary, w: Word): (Word, Flags) = {
    op match {
      case INC => arithmeticWord(ADD, w, Word(1))
      case DEC => arithmeticWord(SUB, w, Word(1))
    }
  }
  def arithmetic(op: ArithmeticOpUnary, w: DWord): (DWord, Flags) = {
    op match {
      case INC => arithmeticDWord(ADD, w, DWord(1))
      case DEC => arithmeticDWord(SUB, w, DWord(1))
    }
  }

}

class CPU {

  //gp registers
  var sp = Simulator.maxMemorySize
  var ip = 0x2000
  var halted = false
  val alu = new ALU()
  var registers = mutable.Map[FullRegister, DWord](AX -> DWord(), BX -> DWord(), CX -> DWord(), DX -> DWord())

  def reset() {
    ip = 0x2000
    sp = Simulator.maxMemorySize
    halted = false
    registers = mutable.Map[FullRegister, DWord](AX -> DWord(), BX -> DWord(), CX -> DWord(), DX -> DWord())
  }

  def get(r: FullRegister): DWord = {
    registers(r)
  }
  def set(r: FullRegister, v: DWord) {
    registers(r) = v
  }

  def get(r: HalfRegister): Word = {
    r match {
      case r: LowRegister  => Word(get(r.full).l)
      case r: HighRegister => Word(get(r.full).h)
    }
  }

  def set(r: HalfRegister, v: Word) {
    r match {
      case r: LowRegister  => set(r.full, DWord(v, get(r.high)))
      case r: HighRegister => set(r.full, DWord(get(r.low), v))
    }
  }

}

class Memory {
  val values = randomBytes().map(Word(_))

  def randomBytes() = {
    val values = Array.ofDim[Byte](Simulator.maxMemorySize)
    new Random().nextBytes(values)
    values
  }
  def getByte(address: Int) = {
    values(address)
  }
  def getBytes(address: Int): DWord = {
    DWord(values(address), values(address + 1))
  }
  def setByte(address: Int, v: Word) {
    values(address) = v
  }
  def setBytes(address: Int, v: DWord) {
    values(address) = v.l
    values(address + 1) = v.h
  }

}

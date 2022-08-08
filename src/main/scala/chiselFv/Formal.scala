package chiselFv

import chisel3.internal.sourceinfo.SourceInfo
import chisel3.{assert => cassert, _}
import chisel3.util._
import chisel3._
import firrtl.PrimOps.Pad


trait Formal {
  this: Module =>

  private val resetCounter = Module(new ResetCounter)
  resetCounter.io.clk := clock
  resetCounter.io.reset := reset
  val timeSinceReset = resetCounter.io.timeSinceReset
  val notChaos       = resetCounter.io.notChaos

  def assert(cond: Bool, msg: String = "")
            (implicit sourceInfo: SourceInfo,
             compileOptions: CompileOptions): Unit = {
    when(notChaos) {
      cassert(cond, msg)
    }
  }

  def assertAfterWhen(cond: Bool, n: Int, asert: Bool, msg: String = "")
                     (implicit sourceInfo: SourceInfo,
                      compileOptions: CompileOptions): Unit = {
    val flag = RegInit(false.B)
    when(cond) {
      flag := true.B
    }.otherwise {
      flag := false.B
    }
    when(ShiftRegister(flag, n - 1)) {
      assert(asert, msg)
    }
  }

  def assertNextStepWhen(cond: Bool, asert: Bool, msg: String = "")
                        (implicit sourceInfo: SourceInfo,
                         compileOptions: CompileOptions): Unit = {
    val flag = RegInit(false.B)
    when(cond) {
      flag := true.B
    }.otherwise {
      flag := false.B
    }
    when(flag) {
      assert(asert, msg)
    }
  }

  def past[T <: Data](value: T, n: Int)(block: T => Any)
                     (implicit sourceInfo: SourceInfo,
                      compileOptions: CompileOptions): Unit = {
    when(notChaos & timeSinceReset >= n.U) {
      block(Delay(value, n))
    }
  }

  def initialReg(w: Int, v: Int): InitialReg = {
    val reg = Module(new InitialReg(w, v))
    reg.io.clk := clock
    reg.io.reset := reset
    reg
  }

  def anyconst(w: Int): UInt = {
    val cst = Module(new AnyConst(w))
    cst.io.out
  }

}

package vonsim.webapp

import vonsim.simulator.InstructionInfo
import vonsim.utils.CollectionUtils._
import scalatags.JsDom.all._
import org.scalajs.dom.html._
import org.scalajs.dom.raw.HTMLElement
import org.scalajs.dom
import scala.scalajs.js
import js.JSConverters._
import scala.collection.mutable
import vonsim.simulator.Simulator
import scala.util.Random
import vonsim.simulator
import vonsim.simulator.Flags
import vonsim.simulator.DWord
import vonsim.simulator.Word
import vonsim.simulator.FullRegister
import scalatags.JsDom.all._
import vonsim.simulator.Flag

import vonsim.simulator.SimulatorProgramExecuting
import vonsim.simulator.SimulatorExecutionStopped
import vonsim.simulator.SimulatorExecutionError
import vonsim.simulator.SimulatorExecutionFinished
import vonsim.assembly.Compiler.CompilationResult


class RegistersUI(s: VonSimState,val registers:List[FullRegister],title:String,baseId:String="") extends VonSimUI(s){
  
  def getIdFor(part:String)=if (baseId=="") "" else baseId+part

  
  var registerToValueL=mutable.Map[FullRegister,TableCell]()
  var registerToValueH=mutable.Map[FullRegister,TableCell]()
  
//  registers.foreach(r => {
//    val valueElementH=td("00").render
//    val valueElementL=td("00").render
//    registerToValueL(r)=valueElementL
//    registerToValueH(r)=valueElementH
//    body.appendChild( tr(td(r.toString()),valueElementH,valueElementL).render )
//  })
  
  val names=registers.map(r => td(r.toString))
  val namesRow=thead(th("")).render
  val lowRow=tr(td("L")).render
  val highRow=tr(td("H")).render
  registers.foreach(r => {
    val valueElementH=td("00").render
    val valueElementL=td("00").render
    registerToValueL(r)=valueElementL
    registerToValueH(r)=valueElementH
    namesRow.appendChild(th(r.toString).render)
    lowRow.appendChild(valueElementL.render)
    highRow.appendChild(valueElementH.render)
    }
  )
  val body = tbody(id:= getIdFor("TableBody"), cls := "registersTableBody"
      ,lowRow
      ,highRow).render
  
  val registerTable = table(cls := "registerTable"
//    ,thead(th("Register"), th(colspan := 2, "Value"))
//    ,thead(th(""), th("H"), th("L")),
    ,namesRow
    ,body
    ).render
    
  val root = div(id := getIdFor("RegistersTable"), cls:="cpuElement",
    div(cls := "cpuElementHeader"
      ,i(cls:="icon-file-binary"," ")
      ,h3(title))
      ,registerTable
   ).render
   
  def simulatorEvent(){
    registers.foreach(r=>{
      val value=s.s.cpu.get(r)
      registerToValueL(r).textContent=formatWord(value.l)
      registerToValueH(r).textContent=formatWord(value.h)
    })
  }
  def simulatorEvent(i:InstructionInfo){
    simulatorEvent()
  }
  def compilationEvent(){
     
  }
}

class WordUI extends HTMLUI {
  val wordElement=td("00000000 00000000").render
  val root=table(cls := "bitTable", tr(wordElement)).render
  
  def update(v:DWord){
    val l=Word(v.l).bitString.reverse
    val h=Word(v.h).bitString.reverse
    wordElement.textContent=h+" "+l
  }
}

class FlagsUI extends HTMLUI {
  
  val flagElements=Flag.all.map(f=> (f,span("0").render)).toMap
  
  
  val headerRow=thead().render
  val valueRow=tr().render
  flagElements.foreach(f => {
    headerRow.appendChild(td(f._1.toString).render)
    valueRow.appendChild(td(f._2).render)
  })
  val root=table(cls := "flagsTable"
      ,headerRow
      ,tbody(valueRow)
  ).render
  
  def flagAsString(flag:Boolean)= if (flag) "1" else "0"
  def update(flags:Flags){
    flagElements.foreach(f =>{
      f._2.textContent=flagAsString(flags.get(f._1))
    })
    
  }
}

class AluUI(s: VonSimState) extends VonSimUI(s) {
  
  val bitTableA = new WordUI()
  val bitTableB = new WordUI()
  val resultBitTable = new WordUI()
  val flagsUI=new FlagsUI()
  val operation = span(cls:="operation","--").render
  val root = div(id := "alu", cls := "cpuElement"
    ,div(cls:="cpuElementHeader",i(cls:="icon-calculator"),h3(" ALU"))
    ,div(cls:="cpuElementContent"
      ,div(cls:="aluComputation"
        ,div(cls:="aluOperation",operation)
        ,div(cls:="aluOperands"
          ,div(cls:="aluOperandA",bitTableA.root)
          ,div(cls:="aluOperandB",bitTableB.root)
          ,div(cls:="aluResult",resultBitTable.root)
        ) 
      )
      ,div(cls:="aluFlags",i(cls:="fa fa-flag"), flagsUI.root)
    )
    ).render
    
  def simulatorEvent(){
    operation.textContent=s.s.cpu.alu.op.toString()
    bitTableA.update(s.s.cpu.alu.o1)
    bitTableB.update(s.s.cpu.alu.o2)
    resultBitTable.update(s.s.cpu.alu.res)
    flagsUI.update(s.s.cpu.alu.flags)
  }  
  def simulatorEvent(i:InstructionInfo){
     simulatorEvent() 
  }
  def compilationEvent(){
     
  }
}



class CpuUI(s: VonSimState) extends MainboardItemUI(s,"img/mainboard/microchip.png","cpu","CPU") {
  
  val generalPurposeRegistersTable = new RegistersUI(s,List(simulator.AX,simulator.BX,simulator.CX,simulator.DX),"General Purpose Registers","generalPurpose")
  val specialRegistersTable = new RegistersUI(s,List(simulator.IP,simulator.SP),"Special Registers","special")
  val alu=new AluUI(s)
  
  contentDiv.appendChild(generalPurposeRegistersTable.root)
  contentDiv.appendChild(specialRegistersTable.root)
  contentDiv.appendChild(alu.root)
  
  def simulatorEvent() {
    generalPurposeRegistersTable.simulatorEvent()
    specialRegistersTable.simulatorEvent()
    alu.simulatorEvent()
  }
  def simulatorEvent(i:InstructionInfo) {
    generalPurposeRegistersTable.simulatorEvent(i)
    specialRegistersTable.simulatorEvent(i)
    alu.simulatorEvent(i)
  }
  
}
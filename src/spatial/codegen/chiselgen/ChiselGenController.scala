package spatial.codegen.chiselgen

import argon._
import argon.codegen.Codegen
import spatial.lang._
import spatial.node._
import spatial.metadata.bounds._
import spatial.metadata.access._
import spatial.metadata.retiming._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.metadata.types._
import spatial.util.modeling.scrubNoise
import spatial.util.spatialConfig

trait ChiselGenController extends ChiselGenCommon {

  var hwblock: Option[Sym[_]] = None
  // var outMuxMap: Map[Sym[Reg[_]], Int] = Map()
  private var nbufs: List[(Sym[Reg[_]], Int)]  = List()
  private var memsWithReset: List[Sym[_]] = List()

  var instrumentCounters: List[(Sym[_], Int)] = List()

  /* For every iter we generate, we track the children it may be used in.
     Given that we are quoting one of these, look up if it has a map entry,
     and keep getting parents of the currentController until we find a match or 
     get to the very top
  */


  def createBreakpoint(lhs: Sym[_], id: Int): Unit = {
    // emitInstrumentation(src"top.io.argOuts(top.io_numArgOuts_reg + top.io_numArgIOs_reg + top.io_numArgOuts_instr + $id).bits := 1.U")
    // emitInstrumentation(src"top.io.argOuts(top.io_numArgOuts_reg + top.io_numArgIOs_reg + top.io_numArgOuts_instr + $id).valid := breakpoints($id)")
  }

  def createInstrumentation(lhs: Sym[_]): Unit = {
    if (spatialConfig.enableInstrumentation) {
      // val ctx = s"${lhs.ctx}"
      // emitInstrumentation(src"""// Instrumenting $lhs, context: ${ctx}, depth: ${controllerStack.length}""")
      // val id = instrumentCounters.length
      // if (spatialConfig.compressWires == 1 || spatialConfig.compressWires == 2) {
      //   emitInstrumentation(src"ic(${id*2}).io.enable := ${lhs}.en; ic(${id*2}).reset := accelReset")
      //   emitInstrumentation(src"ic(${id*2+1}).io.enable := risingEdge(${lhs}.done); ic(${id*2+1}).reset := accelReset")
      //   emitInstrumentation(src"""top.io.argOuts(top.io_numArgOuts_reg + top.io_numArgIOs_reg + 2 * ${id}).bits := ic(${id*2}).io.count""")
      //   emitInstrumentation(src"""top.io.argOuts(top.io_numArgOuts_reg + top.io_numArgIOs_reg + 2 * ${id}).valid := ${hwblock.get}.en""")
      //   emitInstrumentation(src"""top.io.argOuts(top.io_numArgOuts_reg + top.io_numArgIOs_reg + 2 * ${id} + 1).bits := ic(${id*2+1}).io.count""")
      //   emitInstrumentation(src"""top.io.argOuts(top.io_numArgOuts_reg + top.io_numArgIOs_reg + 2 * ${id} + 1).valid := ${hwblock.get}.en""")        
      // } else {
      //   emitInstrumentation(src"""val ${lhs}_cycles = Module(new InstrumentationCounter())""")
      //   emitInstrumentation(src"${lhs}_cycles.io.enable := ${lhs}.en; ${lhs}_cycles.reset := accelReset")
      //   emitInstrumentation(src"""val ${lhs}_iters = Module(new InstrumentationCounter())""")
      //   emitInstrumentation(src"${lhs}_iters.io.enable := risingEdge(${lhs}.done); ${lhs}_iters.reset := accelReset")
      //   emitInstrumentation(src"""top.io.argOuts(top.io_numArgOuts_reg + top.io_numArgIOs_reg + 2 * ${id}).bits := ${lhs}_cycles.io.count""")
      //   emitInstrumentation(src"""top.io.argOuts(top.io_numArgOuts_reg + top.io_numArgIOs_reg + 2 * ${id}).valid := ${hwblock.get}.en""")
      //   emitInstrumentation(src"""top.io.argOuts(top.io_numArgOuts_reg + top.io_numArgIOs_reg + 2 * ${id} + 1).bits := ${lhs}_iters.io.count""")
      //   emitInstrumentation(src"""top.io.argOuts(top.io_numArgOuts_reg + top.io_numArgIOs_reg + 2 * ${id} + 1).valid := ${hwblock.get}.en""")        
      // }
      // instrumentCounters = instrumentCounters :+ (lhs, controllerStack.length)
    }
  }

  final private def enterCtrl(lhs: Sym[_]): Sym[_] = {
    val parent = if (controllerStack.isEmpty) lhs else controllerStack.head
    controllerStack.push(lhs)
    val cchain = if (lhs.cchains.isEmpty) "" else s"${lhs.cchains.head}"
    if (lhs.isOuterControl)      { widthStats += lhs.children.filter(_.s.get != lhs).toList.length }
    else if (lhs.isInnerControl) { depthStats += controllerStack.length }

    parent
  }

  final private def exitCtrl(lhs: Sym[_]): Unit = {
    // Tree stuff
    controllerStack.pop()
  }

  final private def emitItersAndValids(lhs: Sym[_]) = {
    val cchain = lhs.cchains.head
    val iters = lhs.toScope.iters
    val valids = lhs.toScope.valids
    val Op(CounterChainNew(counters)) = cchain
    iters.zipWithIndex.foreach{ case (iter, id) =>
      val i = cchain.constPars.zipWithIndex.map{case(_,j) => cchain.constPars.take(j+1).sum}.indexWhere(id < _)
      val w = bitWidth(counters(i).typeArgs.head)
      emit(src"val $iter = ${cchain}.cchain.io.output.counts($id).FP(true, $w, 0)")
      if (lhs.isOuterPipeLoop && lhs.children.size > 1) {
        emit(src"""val ${iter}_chain = Module(new RegChainPass(${lhs.children.size}, ${w}, myName = "${iter}_chain"))""")
        emit(src"""${iter}_chain.chain_pass(${iter}, ${lhs}.sm.io.doneIn.head)""")
        forEachChild(lhs){case (c, i) => 
          emit(src"""${iter}_chain.connectStageCtrl(${c}.done, ${c}.en, $i)""")
          if (i > 0) emit(src"""val ${iter}_chain_read_$i = ${iter}_chain.read($i).FP(true,${w},0)""")
        }
      }
    }
    valids.zipWithIndex.foreach{ case (v,id) => 
      emit(src"val $v = ~${cchain}.cchain.io.output.oobs($id)")
      if (lhs.isOuterPipeLoop && lhs.children.size > 1) {
        emit(src"""val ${v}_chain = Module(new RegChainPass(${lhs.children.size}, 1, myName = "${v}_chain"))""")
        emit(src"""${v}_chain.chain_pass(${v}, ${lhs}.sm.io.doneIn.head)""")
        forEachChild(lhs){case (c, i) => 
          emit(src"""${v}_chain.connectStageCtrl(${c}.done, ${c}.en, $i)""")
          if (i > 0) emit(src"""val ${v}_chain_read_$i: Bool = ${v}_chain.read($i).apply(0)""")
        }
      }
    }
  }

  final private def emitItersAndValidsStream(lhs: Sym[_]) = {
    val cchain = lhs.cchains.head
    val iters = lhs.toScope.iters
    val valids = lhs.toScope.valids
    val Op(CounterChainNew(counters)) = cchain
    forEachChild(lhs){case (c, i) => 
      iters.zipWithIndex.foreach{ case (iter, id) =>
        val i = cchain.constPars.zipWithIndex.map{case(_,j) => cchain.constPars.take(j+1).sum}.indexWhere(id < _)
        val w = bitWidth(counters(i).typeArgs.head)
        emit(src"val ${iter}_copy$c = ${cchain}_copy$c.cchain.io.output.counts($id).FP(true, $w, 0)")
      }
      valids.zipWithIndex.foreach{ case (v,id) => 
        emit(src"val ${v}_copy$c = ~${cchain}_copy$c.cchain.io.output.oobs($id)")
      }

    }
  }

  private def createKernel(lhs: Sym[_], ens: Set[Bit], func: Block[_]*)(contents: => Unit): Unit = {
    // Find everything that is used in this scope
    // Only use the non-block inputs to LHS since we already account for the block inputs in nestedInputs
    val used: Set[Sym[_]] = {lhs.nonBlockInputs.toSet ++ func.flatMap{block => block.nestedInputs }}.filterNot(_.isCounterChain)
    val made: Set[Sym[_]] = lhs.op.map{d => d.binds }.getOrElse(Set.empty).filterNot(_.isCounterChain)
    val inputs: Seq[Sym[_]] = (used diff made).filterNot{s => s.isMem || s.isValue }.toSeq
    val isInner = lhs.isInnerControl

    dbgs(s"${stm(lhs)}")
    val chainPassedInputs = inputs.map{x => appendSuffix(lhs, x)}
    inputs.foreach{in => dbgs(s" - ${stm(in)}") }
    chainPassedInputs.foreach{in => dbgs(s" - ${in}") }

    val useMap = inputs.flatMap{s => scoped.get(s).map{v => s -> v}}
    scoped --= useMap.map(_._1)

    inGen(out, src"kernel_$lhs.scala"){
      emitHeader()

      val ret = if (lhs.op.exists(_.R.isBits)) src"${lhs.op.get.R.tp}" else "Unit"
      emit(src"/** BEGIN ${lhs.name} $lhs **/")
      open(src"object ${lhs}_kernel {")
        open(src"def run(")
          inputs.zipWithIndex.foreach{case (in,i) => emit(src"$in: ${arg(in.tp)},") }
          emit("top: AccelTop")
        closeopen(src"): $ret = {")
          // Wire signals to SM object
          if (!lhs.isOuterStreamControl) {
            if (lhs.cchains.nonEmpty && !lhs.cchains.head.isForever) {
              emitItersAndValids(lhs)
              val ctr = lhs.cchains.head
              emit(src"""${ctr}.en := ${lhs}.sm.io.ctrInc & ${lhs}.iiDone & ${getForwardPressure(lhs.toCtrl)}""")
              if (getReadStreams(lhs.toCtrl).nonEmpty) emit(src"""${ctr}.reset := ${DL(src"${lhs}.done", 1, true)} // Do not use rst_en for stream kiddo""")
              emit(src"""${ctr}.reset := ${lhs}.resetChildren""")
              emit(src"""${lhs}.sm.io.ctrDone := ${DL(src"${ctr}.done", 1, true)}""")
            } else if (lhs.isInnerControl & lhs.children.filter(_.s.get != lhs).nonEmpty & (lhs match {case Op(SwitchCase(_)) => true; case _ => false})) { // non terminal switch case
              emit(src"""${lhs}.sm.io.ctrDone := ${lhs.children.filter(_.s.get != lhs).head.s.get}.done""")
            } else if (lhs match {case Op(Switch(_,_)) => true; case _ => false}) { // switch, ctrDone is replaced with doneIn(#)
            } else if (lhs match {case Op(_:StateMachine[_]) if (isInner && lhs.children.filter(_.s.get != lhs).length > 0) => true; case _ => false }) {
              emit(src"""${lhs}.sm.io.ctrDone := ${lhs.children.filter(_.s.get != lhs).head.s.get}.done""")
            } else if (lhs match {case Op(x:StateMachine[_]) if (isInner && lhs.children.filter(_.s.get != lhs).length == 0) => true; case _ => false }) {
              val x = lhs match {case Op(_@StateMachine(_,_,_,_,nextState)) => nextState.result; case _ => throw new Exception("Unreachable SM Logic")}
              emit(src"""${lhs}.sm.io.ctrDone := ${lhs}.iiDone.D(${x.fullDelay})""")
            } else {
              emit(src"""${lhs}.sm.io.ctrDone := risingEdge(${lhs}.sm.io.ctrInc)""")
            }
          }
          else {
            if (lhs.isOuterStreamLoop) emitItersAndValidsStream(lhs)
          }
          contents
        close(s"}")
      close("}")
      emit(src"/** END ${lhs.op.get.name} $lhs **/")
      emitFooter()
    }
    scoped ++= useMap
    emit(src"${lhs}_kernel.run($chainPassedInputs ${if (inputs.nonEmpty) "," else ""} top)")
  }

  private def emitSMObject(lhs: Sym[_])(contents: => Unit): Unit = {
    inGen(out, "Controllers.scala"){
      // (0 until controllerStack.size).foreach{_ => state.incGenTab}
      open(src"object $lhs extends SMObject{")
        contents
      close("}")
      // (0 until controllerStack.size).foreach{_ => state.decGenTab}
    }
  }

  private def emitSwitchAddition(lhs: Sym[_]): Unit = {
    open("override def configure(): Unit = {")
      emit(src"datapathEn := parent.get._1.datapathEn")
      emit("super.configure()")
    close("}")
  }

  private def emitStreamChildAddition(lhs: Sym[_]): Unit = {
    open("override def configure(): Unit = {")
      emit(src"parent.get._1.sm.io.ctrCopyDone(parent.get._2) := parent.get._1.datapathEn")
      emit("super.configure()")
    close("}")
  }

  private def createSMObject(lhs:Sym[_], isFSM: Boolean): Unit = {

    val isInner = lhs.isInnerControl
    val lat = if (spatialConfig.enableRetiming & lhs.isInnerControl) scrubNoise(lhs.bodyLatency.sum) else 0.0
    val ii = scrubNoise(lhs.II)

    // Construct controller args
    val constrArg = if (lhs.isInnerControl) {s"$isFSM"} else {s"${lhs.children.filter(_.s.get != lhs).length}, isFSM = ${isFSM}"}
    val isPassthrough = lhs match{
      case Op(_: Switch[_]) if isInner && lhs.parent.s.isDefined && lhs.parent.s.get.isInnerControl => ",isPassthrough = true";
      case Op(_:SwitchCase[_]) if isInner && lhs.parent.s.get.parent.s.isDefined && lhs.parent.s.get.parent.s.get.isInnerControl => ",isPassthrough = true";
      case _ => ""
    }
    val stw = lhs match{case Op(StateMachine(_,_,notDone,_,_)) => s",stateWidth = ${bitWidth(notDone.input.tp)}"; case _ => ""}
    val ncases = lhs match{
      case Op(x: Switch[_]) => s",cases = ${x.cases.length}"
      case Op(x: SwitchCase[_]) if isInner & lhs.children.filter(_.s.get != lhs).nonEmpty => s",cases = ${lhs.children.filter(_.s.get != lhs).length}"
      case Op(_: StateMachine[_]) if isInner & lhs.children.filter(_.s.get != lhs).nonEmpty => s", cases=${lhs.children.filter(_.s.get != lhs).length}"
      case _ => ""
    }

    createInstrumentation(lhs)

    // Create controller
    emitSMObject(lhs) {
      emit(src"""val sm = Module(new ${lhs.level.toString}(${lhs.rawSchedule.toString}, ${constrArg.mkString} $stw $isPassthrough $ncases, latency = $lat.toInt, myName = "${lhs}_sm"))""")
      if (lhs.cchains.isEmpty) emit(src"""datapathEn := sm.io.datapathEn & mask""")
      else emit(src"""datapathEn := sm.io.datapathEn & mask & ~sm.io.ctrDone""")

      if (lhs.II <= 1 | !spatialConfig.enableRetiming | lhs.isOuterControl) {
        emit(src"""iiDone := true.B""")
      } else {
        emit(src"""private val iiCtr = Module(new IICounter(${ii}.toInt, 2 + fringe.utils.log2Up(${ii}.toInt), "${lhs}_iiCtr"));""")
        emit(src"""iiCtr.io.input.enable := datapathEn""")
        emit(src"""iiCtr.io.input.reset := sm.io.parentAck""")
        emit(src"""iiDone := iiCtr.io.output.done | ~mask""")
      }
      emit(src"override val children = List[SMObject](${lhs.children.map(_.s.get).map(quote).mkString(",")})")
      if (lhs.cchains.nonEmpty && !lhs.isOuterStreamLoop) emit(src"override val cchains = List[CChainObject](${lhs.cchains.head})")
      else if (lhs.cchains.nonEmpty && lhs.isOuterStreamLoop) emit(src"override val cchains = List[CChainObject](${lhs.children.map{c => src"${lhs.cchains.head}_copy${c.s.get}"}.mkString(",")})")
      emit(src"val parent = ${parentAndSlot(lhs)}")
      if (lhs.isSwitch) emitSwitchAddition(lhs)
    }

    emit(src"${lhs}.en := ${lhs}.baseEn & top.rr & ${getForwardPressure(lhs.toCtrl)}")
    emit(src"${lhs}.flow := ${getBackPressure(lhs.toCtrl)}")
    val suffix = if (lhs.isOuterStreamLoop) src"_copy${lhs.children.head.s.get}" else ""
    val noop = if (lhs.cchains.nonEmpty) src"~${lhs.cchains.head}$suffix.cchain.io.output.noop" else "true.B"
    val parentMask = and(controllerStack.head.enables.map{x => appendSuffix(lhs, x)})
    emit(src"${lhs}.mask := $noop & $parentMask")
    emit(src"${lhs}.configure()")
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case AccelScope(func) =>
      inAccel{
        hwblock = Some(enterCtrl(lhs))
        createSMObject(lhs, false)
        createKernel(lhs, Set(), func){
          emit(src"""${lhs}.baseEn := top.io.enable""")
          emit(src"""${lhs}.en := ${lhs}.baseEn & !top.io.done & ${getForwardPressure(lhs.toCtrl)}""")
          emit(src"""${lhs}.resetMe := getRetimed(top.accelReset, 1)""")
          emit(src"""${lhs}.mask := true.B""")
          emit(src"""val retime_counter = Module(new SingleCounter(1, Some(0), Some(top.max_latency), Some(1), Some(0))) // Counter for masking out the noise that comes out of ShiftRegister in the first few cycles of the app""")
          emit(src"""retime_counter.io.input.saturate := true.B; retime_counter.io.input.reset := top.accelReset; retime_counter.io.input.enable := true.B;""")
          emit(src"""top.retime_released := ${DL("retime_counter.io.output.done",1)} // break up critical path by delaying this """)

          emit(src"""${lhs}.sm.io.parentAck := top.io.done""")
          visitBlock(func)

          emit(src"""val done_latch = Module(new SRFF())""")
          if (earlyExits.nonEmpty) {
            appPropertyStats += HasBreakpoint
            inGen(out, "ArgInterface.scala"){emit(s"""val breakpoints = Wire(Vec(${earlyExits.length}, Bool()))""")}
            emit(src"""done_latch.io.input.set := ${lhs}.done | breakpoints.reduce{_|_}""")        
          } else {
            emit(src"""done_latch.io.input.set := ${lhs}.done""")                
          }
          emit(src"""done_latch.io.input.reset := ${lhs}.resetMe""")
          emit(src"""done_latch.io.input.asyn_reset := ${lhs}.resetMe""")
          emit(src"""top.io.done := done_latch.io.output.data""")
        }
        exitCtrl(lhs)
      }

    case ctrl: EnControl[_] if !lhs.isFSM=> 
      enterCtrl(lhs)
      createSMObject(lhs, false)
      createKernel(lhs, ctrl.ens, ctrl.bodies.flatMap{_.blocks.map(_._2)}:_*) {
        ctrl.bodies.flatMap{_.blocks.map(_._2)}.foreach{b => visitBlock(b); ()}
      }
      exitCtrl(lhs)


    case op@Switch(selects, body) => 
      enterCtrl(lhs)
      createSMObject(lhs, false)
      val cases = lhs.children.filter(_.s.get != lhs).map(_.s.get)

      if (lhs.isInnerControl) { // If inner, don't worry about condition mutation
        selects.zipWithIndex.foreach{case (s,i) => emit(src"""${lhs}.sm.io.selectsIn($i) := $s""")}
      } else { // If outer, latch in selects in case the body mutates the condition
        selects.indices.foreach{i => 
          emit(src"""val ${cases(i)}_switch_sel_reg = RegInit(false.B)""")
          emit(src"""${cases(i)}_switch_sel_reg := Mux(risingEdge(${lhs}.en), ${selects(i)}, ${cases(i)}_switch_sel_reg)""")
          emit(src"""${lhs}.sm.io.selectsIn($i) := ${selects(i)}""")
        }
      }

      createKernel(lhs, Set(), body) {
        visitBlock(body)
        if (op.R.isBits) {
          emit(src"val ${lhs}_onehot_selects = Wire(Vec(${selects.length}, Bool()))");emit(src"val ${lhs}_data_options = Wire(Vec(${selects.length}, ${lhs.tp}))")
          selects.indices.foreach { i => emit(src"${lhs}_onehot_selects($i) := ${selects(i)}");emit(src"${lhs}_data_options($i) := ${cases(i)}") }
          emit(src"Mux1H(${lhs}_onehot_selects, ${lhs}_data_options)")
        }
      }
      exitCtrl(lhs)


    case op@SwitchCase(body) =>
      enterCtrl(lhs)
      createSMObject(lhs, false)

      createKernel(lhs, Set(), body) {
        emit(s"// Controller Stack: ${controllerStack.tail}")
        visitBlock(body)
        if (op.R.isBits) {
          emit(src"${body.result}")
        }
      }
      exitCtrl(lhs)

    case StateMachine(ens,start,notDone,action,nextState) =>
      appPropertyStats += HasFSM
      enterCtrl(lhs)
      createSMObject(lhs, false)
      createKernel(lhs, ens, notDone, action, nextState) {
        // ctrl.bodies.flatMap{_.blocks.map(_._2)}.foreach{b => visitBlock(b); ()}
      }
      exitCtrl(lhs)


      // val state = notDone.input
      // emitGlobalWireMap(src"$state", src"Wire(${state.tp})")
      // emitGlobalWireMap(src"${lhs}_doneCondition", "Wire(Bool())")

      // emit("// Emitting notDone")
      // visitBlock(notDone)

      // emit("// Emitting action")
      // inSubGen(src"${lhs}", src"${parent_kernel}") {
      //   emit(s"// Controller Stack: ${controllerStack.tail}")
      //   visitBlock(action)
      // }
      // emit("// Emitting nextState")
      // visitBlock(nextState)
      // emit(src"${lhs}.sm.io.enable := ${lhs}.en ")
      // emit(src"${lhs}.sm.io.nextState := ${nextState.result}.r.asSInt ")
      // emit(src"${lhs}.sm.io.initState := ${start}.r.asSInt")
      // emit(src"${state}.r := ${lhs}.sm.io.state.r")
      // emit(src"${lhs}.doneCondition := ~${notDone.result}")
      // emit(src"${lhs}.sm.io.doneCondition := ${lhs}.doneCondition")
      // exitCtrl(lhs)


    case _ => super.gen(lhs, rhs)
  }

  override def emitPostMain(): Unit = {

    inGen(out, "Instantiator.scala") {
      emit ("")
      emit ("// Instrumentation")
      emit (s"val numArgOuts_instr = ${instrumentCounters.length*2}")
      instrumentCounters.zipWithIndex.foreach { case(p,i) =>
        val depth = " "*p._2
        emit (src"""// ${depth}${quote(p._1)}""")
      }
      emit (s"val numArgOuts_breakpts = ${earlyExits.length}")
      emit ("""/* Breakpoint Contexts:""")
      earlyExits.zipWithIndex.foreach {case (p,i) => 
        createBreakpoint(p, i)
        emit (s"breakpoint ${i}: ${p.ctx}")
      }
      emit ("""*/""")
    }

    inGen(out, s"IOModule.$ext") {
      emit (src"// Root controller for app: ${config.name}")
      emit ("")
      emit (src"// Widths: ${widthStats.sorted}")
      emit (src"//   Widest Outer Controller: ${if (widthStats.length == 0) 0 else widthStats.max}")
      emit (src"// Depths: ${depthStats.sorted}")
      emit (src"//   Deepest Inner Controller: ${if (depthStats.length == 0) 0 else depthStats.max}")
      emit (s"// App Characteristics: ${appPropertyStats.toList.map(_.getClass.getName.split("\\$").last.split("\\.").last).mkString(",")}")
      emit ("// Instrumentation")
      emit (s"val io_numArgOuts_instr = ${instrumentCounters.length*2}")
      emit (s"val io_numArgOuts_breakpts = ${earlyExits.length}")

      emit ("""// Set Build Info""")
      emit(s"val max_latency = $maxretime")

      emit (s"globals.target.fixmul_latency = ${latencyOption("FixMul", Some(1))}")
      emit (s"globals.target.fixdiv_latency = ${latencyOption("FixDiv", Some(1))}")
      emit (s"globals.target.fixadd_latency = ${latencyOption("FixAdd", Some(1))}")
      emit (s"globals.target.fixsub_latency = ${latencyOption("FixSub", Some(1))}")
      emit (s"globals.target.fixmod_latency = ${latencyOption("FixMod", Some(1))}")
      emit (s"globals.target.fixeql_latency = ${latencyOption("FixEql", None)}.toInt")
      // emit (s"tight_control   = ${spatialConfig.enableTightControl}")
      emit (s"globals.target.mux_latency    = ${latencyOption("Mux", None)}.toInt")
      emit (s"globals.target.sramload_latency    = ${latencyOption("SRAMBankedRead", None)}.toInt")
      emit (s"globals.target.sramstore_latency    = ${latencyOption("SRAMBankedWrite", None)}.toInt")
      emit (s"globals.target.SramThreshold = ${spatialConfig.sramThreshold}")
      emit (s"""globals.retime = ${spatialConfig.enableRetiming}""")

    }

    super.emitPostMain()
  }

}

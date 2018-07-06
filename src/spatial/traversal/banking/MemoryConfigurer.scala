package spatial.traversal
package banking

import argon._
import poly.ISL
import utils.implicits.collections._

import spatial.issues.UnbankableGroup
import spatial.lang._
import spatial.metadata.access._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.util.spatialConfig

import scala.collection.mutable.ArrayBuffer

class MemoryConfigurer[+C[_]](mem: Mem[_,C], strategy: BankingStrategy)(implicit state: State, isl: ISL) {
  protected val rank: Int = mem.rank.length
  protected val isGlobal: Boolean = mem.isArgIn || mem.isArgOut

  // TODO: This may need to be tweaked based on the fix for issue #23
  final val FLAT_BANKS = Seq(List.tabulate(rank){i => i})
  final val NEST_BANKS = List.tabulate(rank){i => Seq(i)}
  val dimGrps: Seq[Seq[Seq[Int]]] = if (rank > 1) Seq(FLAT_BANKS, NEST_BANKS) else Seq(FLAT_BANKS)

  def configure(): Unit = {
    dbg(s"---------------------------------------------------------------------")
    dbg(s"INFERRING...")
    dbg(s"Name: ${mem.fullname}")
    dbg(s"Type: ${mem.tp}")
    dbg(s"Src:  ${mem.ctx}")
    dbg(s"Src:  ${mem.ctx.content.getOrElse("<???>")}")
    dbg(s"Symbol:     ${stm(mem)}")
    dbg(s"---------------------------------------------------------------------")
    val readers = mem.readers
    val writers = mem.writers
    resetData(readers, writers)

    val readMatrices = readers.flatMap{rd => rd.affineMatrices }
    val writeMatrices = writers.flatMap{wr => wr.affineMatrices }

    val instances = bank(readMatrices, writeMatrices)

    summarize(instances)
    finalize(instances)
  }

  protected def resetData(readers: Set[Sym[_]], writers: Set[Sym[_]]): Unit = {
    (readers.iterator ++ writers.iterator).foreach{a =>
      metadata.clear[Dispatch](a)
      metadata.clear[Ports](a)
    }
    metadata.clear[Duplicates](mem)
  }

  protected def summarize(instances: Seq[Instance]): Unit = {
    dbg(s"---------------------------------------------------------------------")
    dbg(s"SUMMARY: ")
    dbg(s"Name: ${mem.fullname}")
    dbg(s"Type: ${mem.tp}")
    dbg(s"Src:  ${mem.ctx}")
    dbg(s"Src:  ${mem.ctx.content.getOrElse("<???>")}")
    dbg(s"Symbol:     ${stm(mem)}")
    dbg(s"---------------------------------------------------------------------")
    dbg(s"Instances: ${instances.length}")
    instances.zipWithIndex.foreach{case (inst,i) =>
      dbg(s"Instance #$i")
      dbgss("  ", inst)
      dbg("\n\n")
    }
    dbg(s"---------------------------------------------------------------------")
    dbg("\n\n\n")
  }

  /** Complete memory analysis by adding banking and buffering metadata to the memory and
    * all associated accesses.
    */
  protected def finalize(instances: Seq[Instance]): Unit = {
    val duplicates = instances.map{_.toMemory}
    mem.duplicates = duplicates

    instances.zipWithIndex.foreach{case (inst, dispatch) =>
      (inst.reads.iterator.flatten ++ inst.writes.iterator.flatten).foreach{a =>
        a.access.addPort(dispatch, a.unroll, inst.ports(a))
        a.access.addDispatch(a.unroll, dispatch)
        dbgs(s"  Added port ${inst.ports(a)} to ${a.access} {${a.unroll.mkString(",")}}")
        dbgs(s"  Added dispatch $dispatch to ${a.access} {${a.unroll.mkString(",")}}")
      }

      if (inst.writes.flatten.isEmpty && mem.name.isDefined && !mem.hasInitialValues) {
        inst.reads.iterator.flatten.foreach{read =>
          warn(read.access.ctx, s"Memory ${mem.name.get} appears to be read here before ever being written.")
          warn(read.access.ctx)
          warn(mem.ctx, s"For memory ${mem.name.get} originally defined here.", noWarning = true)
          warn(mem.ctx)
        }
      }
    }

    val used = instances.flatMap(_.accesses).toSet
    val unused = mem.accesses diff used
    unused.foreach{access =>
      val msg = if (access.isReader) s"Read of ${mem.nameOr("memory")} was unused. Read will be removed."
                else s"Write to memory ${mem.nameOr("memory")} is never used. Write will be removed."
      warn(access.ctx, msg)
      warn(access.ctx)

      access.isUnusedAccess = true

      dbgs(s"  Unused access: ${stm(access)}")
    }
  }

  /** True if a and b always occur at the exact same time.
    * This is trivially true if a and b are the same unrolled access.
    *
    * This method is used to enable broadcast reads.
    */
  def canBroadcast(a: AccessMatrix, b: AccessMatrix): Boolean = {
    // TODO[3]: What about accesses of the same form across different loops?
    if (a.access != b.access || a.matrix != b.matrix) return false

    val iters = accessIterators(a.access, mem)
    // The index of iterators which will differ between a and b
    // If this list is empty, a and b are identical (so they are trivially lockstep)
    val differ = a.unroll.indices.filter{i => a.unroll(i) != b.unroll(i) }
    val itersDiffer = differ.map{i => iters(i) }
    if (itersDiffer.nonEmpty) {
      val outermost = itersDiffer.head
      if (outermost.isInnerControl) true  // Unrolling takes care of this broadcast within inner ctrl
      else {
        // Need more specialized logic for broadcasting across controllers
        spatialConfig.enableBroadcast && outermost.isLockstepAcross(itersDiffer, Some(a.access))
      }
    }
    else true
  }

  /** Group accesses on this memory.
    * An access a is grouped with a set of accesses S if there exists some b in S such that:
    *   [Control] a and b occur simultaneously (in Parallel or pipeline parallel)
    *   [Space]   and a and b are guaranteed to never hit the same address
    * Otherwise a is placed in a new group
    */
  protected def groupAccesses(accesses: Set[AccessMatrix]): Set[Set[AccessMatrix]] = {
    val groups = ArrayBuffer[Set[AccessMatrix]]()
    val isWrite = accesses.exists(_.access.isWriter)
    val tp = if (isWrite) "Write" else "Read"

    dbgs(s"  Grouping ${accesses.size} ${tp}s: ")

    import scala.math.Ordering.Implicits._  // Seq ordering
    val sortedAccesses = accesses.toSeq.sortBy(_.access.toString).sortBy(_.unroll)

    sortedAccesses.foreach{a =>
      dbg(s"    Access: ${a.access} {${a.unroll.mkString(",")}} [${a.parent}]")
      val grpId = {
        if (a.parent == Ctrl.Host) { if (groups.isEmpty) -1 else 0 }
        else groups.zipWithIndex.indexWhere{case (grp, i) =>
          // Filter for accesses that require concurrent port access AND either don't overlap or are identical.
          // Should drop in data broadcasting node in this case
          val samePort = grp.filter{b => requireConcurrentPortAccess(a, b) }
          // A conflict occurs if there are accesses on the same port with overlapping addresses
          // for which we can cannot create a broadcaster read
          // (either because they are not lockstep, not reads, or because broadcasting is disabled)
          val conflicts = samePort.filter{b => a.overlapsAddress(b) && !canBroadcast(a, b) }
          if (samePort.nonEmpty) dbg(s"      Group #$i: ")
          else                   dbg(s"      Group #$i: <none>")
          samePort.foreach{b => dbgs(s"        ${b.access} {${b.unroll.mkString(",")}} [${b.parent}] Conflicts: ${conflicts.contains(b)}") }
          samePort.nonEmpty && conflicts.isEmpty
        }
      }
      if (grpId != -1) { groups(grpId) = groups(grpId) + a } else { groups += Set(a) }
    }

    if (config.enDbg) {
      if (groups.isEmpty) dbg(s"\n  <No $tp Groups>") else dbg(s"  ${groups.length} $tp Groups:")
      groups.zipWithIndex.foreach { case (grp, i) =>
        dbg(s"  Group #$i")
        grp.foreach{matrix => dbgss("    ", matrix) }
      }
    }
    groups.toSet
  }

  /** True if accesses a and b may occur concurrently and to the same buffer port.
    * This is true when any of the following hold:
    *   0. a and b are two different unrolled parts of the same node
    *   1. a and b are in the same inner pipeline
    *   2. a and b are in the same fully unrolled inner sequential
    *   3. a and b are in a Parallel controller
    *   4. TODO[2]: If a and b are in parallel stages in a controller's child dataflow graph
    */
  def requireConcurrentPortAccess(a: AccessMatrix, b: AccessMatrix): Boolean = {
    val lca = LCA(a.access, b.access)
    (a.access == b.access && a.unroll != b.unroll) ||
      lca.isInnerPipeLoop ||
      (lca.isInnerSeqControl && lca.isFullyUnrolledLoop) ||
      lca.isParallel
  }


  protected def bank(readers: Set[AccessMatrix], writers: Set[AccessMatrix]): Seq[Instance] = {
    val rdGroups = groupAccesses(readers)
    val wrGroups = groupAccesses(writers)
    if      (readers.nonEmpty) mergeReadGroups(rdGroups, wrGroups)
    else if (writers.nonEmpty) mergeWriteGroups(wrGroups)
    else Seq(Instance.Unit(rank))
  }

  /** Returns an approximation of the cost for the given banking strategy. */
  def cost(banking: Seq[Banking], depth: Int): Int = {
    val totalBanks = banking.map(_.nBanks).product
    depth * totalBanks
  }

  /** Computes the Ports for all accesses in all groups
    *
    *            |--------------|--------------|
    *            |   Buffer 0   |   Buffer 1   |
    *            |--------------|--------------|
    * bufferPort         0              1           The buffer port (None for access outside pipeline)
    * muxSize            3              3           Width of a single time multiplexed vector
    *                 |x x x|        |x x x|
    *
    *                /       \      /       \
    *
    *              |x x x|x x|     |x x x|x x x|
    * muxPort         0    1          0     1       The ID for the given time multiplexed vector
    *
    *              |( ) O|O O|    |(   )|( ) O|
    * muxOfs        0   2 0 1        0    0  2      Start offset into the time multiplexed vector
    *
    */
  protected def computePorts(groups: Set[Set[AccessMatrix]], bufPorts: Map[Sym[_],Option[Int]]): Map[AccessMatrix,Port] = {
    var ports: Map[AccessMatrix,Port] = Map.empty

    val bufferPorts: Map[Option[Int],Set[Set[AccessMatrix]]] = groups.flatMap{grp =>
      grp.groupBy{m => bufPorts(m.access) }.toSeq // These should generally always be all the same?
    }.groupBy(_._1).mapValues(_.map(_._2))

    bufferPorts.foreach{case (bufferPort, grps) =>
      var muxPortMap: Map[AccessMatrix,Int] = Map.empty

      // Each group is banked together, so all accesses in a group must be connected to the same muxPort
      grps.zipWithIndex.foreach{case (grp,muxPort) =>
        grp.foreach{m => muxPortMap += m -> muxPort }
      }

      val muxPorts: Map[Int,Set[AccessMatrix]] = grps.flatten.groupBy{m => muxPortMap(m) }
      val muxSize: Int = muxPorts.values.map(_.size).maxOrElse(0)

      muxPorts.foreach{case (muxPort, matrices) =>
        var muxOfs: Int = 0
        matrices.groupBy(_.access).values.foreach{mats =>
          import scala.math.Ordering.Implicits._
          mats.toSeq.sortBy(_.unroll).foreach{m =>
            val port = Port(
              bufferPort = bufferPort,
              muxPort    = muxPort,
              muxSize    = muxSize,
              muxOfs     = muxOfs,
              broadcast  = 0
            )
            ports += m -> port
            muxOfs += 1
          }
        }
      }
    }

    ports
  }

  /** Returns the memory instance required to support the given read and write sets.
    * Includes banking, buffering depth, and the mapping of accesses to buffer ports.
    * Also calculates whether the associated memory should be considered a "buffer accumulator";
    * this occurs if at least one read and write occur in the same controller within a buffer.
    */
  protected def bankGroups(rdGroups: Set[Set[AccessMatrix]], wrGroups: Set[Set[AccessMatrix]]): Either[Issue,Instance] = {
    val reads = rdGroups.flatten
    val ctrls = reads.map(_.parent)
    val writes = reachingWrites(reads,wrGroups.flatten,isGlobal)
    val reachingWrGroups = wrGroups.map{grp => grp intersect writes }.filterNot(_.isEmpty)
    val bankings = strategy.bankAccesses(mem, rank, rdGroups, reachingWrGroups, dimGrps)
    if (bankings.nonEmpty) {
      val (metapipe, bufPorts, issue) = findMetaPipe(mem, reads.map(_.access), writes.map(_.access))

      if (issue.isEmpty) {
        ctrlTree((reads ++ writes).map(_.access)).foreach{x => dbgs(x) }

        val depth = bufPorts.values.collect{case Some(p) => p}.maxOrElse(0) + 1
        val bankingCosts = bankings.map{b => b -> cost(b,depth) }
        val (banking, bankCost) = bankingCosts.minBy(_._2)
        // TODO[5]: Assumption: All memories are at least simple dual port
        val ports = computePorts(rdGroups,bufPorts) ++ computePorts(reachingWrGroups,bufPorts)
        val isBuffAccum = writes.cross(reads).exists{case (wr,rd) => rd.parent == wr.parent }
        val accum = if (isBuffAccum) AccumType.Buff else AccumType.None
        val accTyp = mem.accumType | accum

        val instance = Instance(rdGroups,reachingWrGroups,ctrls,metapipe,banking,depth,bankCost,ports,accTyp)

        dbgs(s"  Reads:")
        rdGroups.foreach{grp => grp.foreach{m => dbgss("    ", m) }}
        dbgs(s"  Writes:")
        reachingWrGroups.foreach{grp => grp.foreach{m => dbgss("    ", m) }}
        dbgs(s"  Instance: ")
        dbgss("  ", instance)

        Right(instance)
      }
      else Left(issue.get)
    }
    else {
      Left(UnbankableGroup(mem, reads, writes))
    }
  }

  /** Should not attempt to merge instances if any of the following conditions hold:
    *   1. The two instances have a common LCA controller (means they require separate banking)
    *   2. The two instances result in hierarchical buffers (for now)
    *   3. Either instance is an accumulator and there is at least one pipelined ancestor controller.
    */
  protected def getMergeAttemptError(a: Instance, b: Instance): Option[String] = {
    lazy val reads = a.reads.flatten ++ b.reads.flatten
    lazy val writes = a.writes.flatten ++ b.writes.flatten
    lazy val metapipes = findAllMetaPipes(reads.map(_.access), writes.map(_.access)).keys
    val commonCtrl = a.ctrls intersect b.ctrls

    if (commonCtrl.nonEmpty && !isGlobal)
      Some(s"Control conflict: Common control (${commonCtrl.mkString(",")})")
    else if (metapipes.size > 1)
      Some("Ambiguous metapipes")
    else if (metapipes.nonEmpty && (a.accType | b.accType) >= AccumType.Reduce)
      Some(s"Accumulator conflict (A Type: ${a.accType}, B Type: ${b.accType})")
    else
      None
  }

  /** Should not complete merging instances if any of the following hold:
    *   1. The merge was not successful
    *   2. The merge results in a multi-ported N-buffer (if this is disabled)
    *   3. The merged instance costs more than the total cost of the two separate instances
    */
  protected def getMergeError(i1: Instance, i2: Instance, i3: Instance): Option[String] = {
    if (i1.metapipe.isDefined && i2.metapipe.isDefined && !spatialConfig.enableBufferCoalescing)
      Some("Buffer conflict")
    else if (i3.cost > (i1.cost + i2.cost))
      Some(s"Too expensive: ${i3.cost} > ${i1.cost + i2.cost}")
    else
      None
  }

  /** Greedily banks and merges groups of readers into memory instances. */
  protected def mergeReadGroups(rdGroups: Set[Set[AccessMatrix]], wrGroups: Set[Set[AccessMatrix]]): Seq[Instance] = {
    dbgs("\n\n")
    dbgs(s"Merging memory instance groups:")
    val instances = ArrayBuffer[Instance]()

    rdGroups.zipWithIndex.foreach{case (grp,grpId) =>
      dbgs(s"Group #$grpId: ")
      state.logTab += 1
      bankGroups(Set(grp),wrGroups) match {
        case Right(i1) =>
          var instIdx = 0
          var merged = false
          while (instIdx < instances.length && !merged) {
            dbgs(s"Attempting to merge group #$grpId with instance #$instIdx: ")
            state.logTab += 1
            val i2 = instances(instIdx)

            val err = getMergeAttemptError(i1, i2)
            if (err.isEmpty) {
              bankGroups(i1.reads ++ i2.reads, wrGroups) match {
                case Right(i3) =>
                  val err = getMergeError(i1, i2, i3)
                  if (err.isEmpty) {
                    instances(instIdx) = i3
                    merged = true
                  }
                  else dbgs(s"Did not merge $grpId into instance $instIdx: ${err.get}")

                case Left(issue) =>
                  dbgs(s"Did not merge $grpId into instance $instIdx: ${issue.name}")
              }
            }
            else dbgs(s"Did not merge $grpId into instance $instIdx: ${err.get}")
            state.logTab -= 1
            instIdx += 1
          }
          if (!merged) {
            instances += i1
            dbgs(s"Result: Created instance #${instances.length-1}")
          }
          else {
            dbgs(s"Result: Merged $grpId into instance ${instIdx-1}")
          }

        case Left(issue) => raiseIssue(issue)
      }
      state.logTab -= 1
    }
    instances
  }

  /** Greedily banks and merges groups of writers into memory instances.
    * Only used if the memory has no readers.
    */
  protected def mergeWriteGroups(wrGroups: Set[Set[AccessMatrix]]): Seq[Instance] = {
    // Assumes that all writers reach some unknown reader external to Accel.
    bankGroups(Set.empty, wrGroups) match {
      case Right(instance) => Seq(instance)
      case Left(issue)     => raiseIssue(issue); Nil
    }
  }
}

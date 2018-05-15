package spatial.tests.compiler

import spatial.dsl._

@test class UnrollingTest extends SpatialTest {
  def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Unit = {
    val o = ArgOut[Int]

    Accel {
      Foreach(32*16 by 16 par 2){i =>
        val x = SRAM[Int](32)

        Pipe {
          println(x(16))
        }

      }

      o := 32
    }

    println(getArg(o))
    assert(o == 32)
  }

}

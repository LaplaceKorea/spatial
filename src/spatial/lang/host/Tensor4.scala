package spatial.lang
package host

import argon._
import forge.tags._
import spatial.node._

/** A 4-dimensional tensor on the host */
@ref class Tensor4[A:Type] extends Struct[Tensor4[A]] with Ref[scala.Array[Any],Tensor4[A]] {
  override val box = implicitly[Tensor4[A] <:< Struct[Tensor4[A]]]
  val A: Type[A] = Type[A]
  def fields = Seq(
    "data" -> Type[Array[A]],
    "dim0" -> Type[I32],
    "dim1" -> Type[I32],
    "dim2" -> Type[I32],
    "dim3" -> Type[I32]
  )

  @rig def data: Array[A] = field[Array[A]]("data")

  /** Returns the first dimension of this Tensor4. */
  @api def dim0: I32 = field[I32]("dim0")
  /** Returns the second dimension of this Tensor4. */
  @api def dim1: I32 = field[I32]("dim1")
  /** Returns the third dimension of this Tensor4. */
  @api def dim2: I32 = field[I32]("dim2")
  /** Returns the fourth dimension of this Tensor4. */
  @api def dim3: I32 = field[I32]("dim3")
  /** Returns the element in this Tensor4 at the given 4-dimensional address. */
  @api def apply(i: I32, j: I32, k: I32, l: I32): A = {
    data.apply(i*dim1*dim2*dim3 + j*dim2*dim3 + k*dim3 + l)
  }
  /** Updates the element at the given 4-dimensional address to `elem`. */
  @api def update(i: I32, j: I32, k: I32, l: I32, elem: A): Void = {
    data.update(i*dim1*dim2*dim3 + j*dim2*dim3 + k*dim3 + l, elem)
  }
  /** Returns a flattened, immutable @Array view of this Tensor4's data. */
  @api def flatten: Array[A] = data
  /** Returns the number of elements in the Tensor4. */
  @api def length: I32 = dim0 * dim1 * dim2 * dim3

  /** Applies the function `func` on each element in this Tensor4. */
  @api def foreach(func: A => Void): Void = data.foreach(func)

  /** Returns a new Tensor4 created using the mapping `func` over each element in this Tensor4. */
  @api def map[B:Type](func: A => B): Tensor4[B] = Tensor4(data.map(func), dim0, dim1, dim2, dim3)

  /** Returns a new Tensor4 created using the pairwise mapping `func` over each element in this Tensor4
    * and the corresponding element in `that`.
    */
  @api def zip[B:Type,C:Type](b: Tensor4[B])(func: (A,B) => C): Tensor4[C] = {
    Tensor4(data.zip(b.data)(func), dim0, dim1, dim2, dim3)
  }

  /** Reduces the elements in this Tensor4 into a single element using associative function `rfunc`. */
  @api def reduce(rfunc: (A,A) => A): A = data.reduce(rfunc)

  /** Returns true if this Tensor4 and `that` contain the same elements, false otherwise. */
  @api override def neql(that: Tensor4[A]): Bit = data !== that.data

  /** Returns false if this Tensor4 and `that` differ by at least one element, true otherwise. */
  @api override def eql(that: Tensor4[A]): Bit = data === that.data

}
object Tensor4 {
  @api def apply[A:Type](data: Array[A], dim0: I32, dim1: I32, dim2: I32, dim3: I32): Tensor4[A] = {
    Struct[Tensor4[A]]("data" -> data, "dim0" -> dim0, "dim1" -> dim1, "dim2" -> dim2, "dim3" -> dim3)
  }

  /** Returns an immutable Tensor4 with the given dimensions and elements defined by `func`. */
  @api def tabulate[T:Type](dim0: I32, dim1: I32, dim2: I32, dim3: I32)(func: (I32, I32, I32, I32) => T): Tensor4[T] = {
    val data = Array.tabulate(dim0*dim1*dim2*dim3){x =>
      val i0 = x / (dim1*dim2*dim3)       // Cube
    val i1 = (x / (dim2*dim3)) % dim1   // Page
    val i2 = (x / dim3) % dim2          // Row
    val i3 = x % dim3                   // Col

      func(i0,i1,i2,i3)
    }
    Tensor4(data, dim0, dim1, dim2, dim3)
  }
  /**
    * Returns an immutable Tensor4 with the given dimensions and elements defined by `func`.
    * Note that while `func` does not depend on the index, it is still executed multiple times.
    */
  @api def fill[T:Type](dim0: I32, dim1: I32, dim2: I32, dim3: I32)(func: => T): Tensor4[T]
    = Tensor4.tabulate(dim0,dim1,dim2,dim3){(_,_,_,_) => func}
}

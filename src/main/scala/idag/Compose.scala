package neurocat
package idag

import shapeless.HNil
import typeclasses._
import algebra.ring.AdditiveSemigroup


abstract class Compose[
  P, PA, PB
, Q, QC
// the merge of P & Q params
, PQ
// algebra visitor
, S, Alg[out[p, a, b]] <: DagAlgebra[S, Alg, out]
] private[neurocat] (
  val g: Dag[Q, PB, QC, S, Alg]
, val f: Dag[P, PA, PB, S, Alg]
) extends Dag[PQ, PA, QC, S, Alg] {
  self =>
  def merger: Merger.Aux[P, Q, PQ]

  def compile[Out[p, a, b]](
    compiler: Alg[Out]
  ): Out[PQ, PA, QC] = compiler.compile(this)

}

object Compose {

  abstract class Diff[
    P, PA, PB, Q, QC
  // the merger of P & Q params
  , PQ
  , S
  // algebra visitor
  , Alg[out[p, a, b]] <: DiffDagAlgebra[S, Alg, out]
  ] private[neurocat] (
    override val g: DiffDag[Q, PB, QC, S, Alg]
  , override val f: DiffDag[P, PA, PB, S, Alg]
  ) extends Compose[P, PA, PB, Q, QC, PQ, S, Alg](g, f) with DiffDag[PQ, PA, QC, S, Alg] {
    self =>
    def merger: Merger.Aux[P, Q, PQ]

    def gradA = new Dag[PQ, (PA, QC), PA, S, Alg] {
      def compile[Out[p, a, b]](
        compiler: Alg[Out]
      ): Out[PQ, (PA, QC), PA] = compiler.grada(self)
    }

    def gradP = new Dag[PQ, (PA, QC), PQ, S, Alg] {
      def compile[Out[p, a, b]](
        compiler: Alg[Out]
      ): Out[PQ, (PA, QC), PQ] = compiler.gradp(self)
    }

  }

  trait Algebra[S, Alg[out[p, a, b]] <: DagAlgebra[S, Alg, out], Out[p, a, b]] {    
    def compile[
      P, PA, PB, Q, QC
    , PQ
    ](
      dag: Compose[P, PA, PB, Q, QC, PQ, S, Alg]
    ): Out[PQ, PA, QC]

  }

  trait DiffAlgebra[
    S, Alg[out[p, a, b]] <: DiffDagAlgebra[S, Alg, out], Out[p, a, b]
  ] extends Algebra[S, Alg, Out] {

    def grada[
      P, PA, PB, Q, QC
    , PQ
    ](
      dag: Compose.Diff[P, PA, PB, Q, QC, PQ, S, Alg]
    ): Out[PQ, (PA, QC), PA]

    def gradp[
      P, PA, PB, Q, QC
    , PQ
    ](
      dag: Compose.Diff[P, PA, PB, Q, QC, PQ, S, Alg]
    ): Out[PQ, (PA, QC), PQ]
  }  

  trait Dsl[S, Alg[out[p, a, b]] <: DagAlgebra[S, Alg, out]] {
    def compose[
      P, A, B, PGP, PGA
    , Q, C, QGP, QGA
    ](g: Dag[Q, B, C, S, Alg], f: Dag[P, A, B, S, Alg])(
      implicit merger0: Merger[P, Q]
    ): Dag[merger0.Out, A, C, S, Alg]
    = new Compose[
        P, A, B
      , Q, C
      , merger0.Out
      , S, Alg
      ](g, f) {
        def merger = merger0
      }

  }

  trait DiffDsl[
    S, Alg[out[p, a, b]] <: DiffDagAlgebra[S, Alg, out]
  ] {

    def compose[
      P, A, B, PGP, PGA
    , Q, C, QGP, QGA
    ](g: DiffDag[Q, B, C, S, Alg], f: DiffDag[P, A, B, S, Alg])(
      implicit
        merger0: Merger[P, Q]
      // , costDiffInvertA: CostDiffInvertBuilder[A, S, Alg]
      // , costDiffC: CostDiffBuilder[C, S, Alg]
      // , minusA0: MinusBuilder[A, S, Alg]
      // , scalarTimesP0: ScalarTimesBuilder[P, S, Alg]
      // , scalarTimesQ0: ScalarTimesBuilder[Q, S, Alg]
      // , minusP0: MinusPBuilder[P, S, Alg]
      // , minusQ0: MinusPBuilder[Q, S, Alg]
    ): DiffDag[merger0.Out, A, C, S, Alg]
    = new Compose.Diff[
        P, A, B, Q, C
      , merger0.Out
      , S, Alg
      ](g, f) {
        val merger = merger0
        val costDiffInvert = f.costDiffInvert
        val costDiff = g.costDiff
        val scalarTimes = {
          implicit val sp = f.scalarTimes
          implicit val sq = g.scalarTimes
          implicitly[ScalarTimesBuilder[merger0.Out, S, Alg]]
        }
        val minusA = f.minusA
        val minusP = {
          implicit val mp = f.minusP
          implicit val mq = g.minusP
          implicitly[MinusPBuilder[merger0.Out, S, Alg]]
        }
      }
  }


    def compose[
      P, A, B, PGP, PGA
    , Q, C, QGP, QGA
    , S, Alg[out[p, a, b]] <: DiffDagAlgebra[S, Alg, out]
    ](g: DiffDag[Q, B, C, S, Alg], f: DiffDag[P, A, B, S, Alg])(
      implicit
        merger0: Merger[P, Q]
    ): DiffDag[merger0.Out, A, C, S, Alg]
    = new Compose.Diff[
        P, A, B, Q, C
      , merger0.Out
      , S, Alg
      ](g, f) {
        val merger = merger0
        val costDiffInvert = f.costDiffInvert
        val costDiff = g.costDiff
        val scalarTimes = {
          implicit val sp = f.scalarTimes
          implicit val sq = g.scalarTimes
          implicitly[ScalarTimesBuilder[merger0.Out, S, Alg]]
        }
        val minusA = f.minusA
        val minusP = {
          implicit val mp = f.minusP
          implicit val mq = g.minusP
          implicitly[MinusPBuilder[merger0.Out, S, Alg]]
        }
      }  
}

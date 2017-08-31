package neuroflow.playground

import breeze.stats.distributions.Gaussian
import neuroflow.common.Logs
import neuroflow.core.Activator._
import neuroflow.core.FFN.WeightProvider._
import neuroflow.core._
import neuroflow.nets.distributed.DefaultExecutor
import neuroflow.nets.distributed.DefaultNetwork._
import shapeless._

import scala.util.Random

/**
  * @author bogdanski
  * @since 28.08.17
  */

object DistributedTraining extends Logs {

  val nodesC = 5
  val nodes  = (1 to nodesC).map(i => Node("localhost", 2552 + i)).toSet
  val dim    = 12000
  val out    = 210

  def coordinator = {

    val f   = ReLU

    val net =
      Network(
        Input (dim)               ::
        Hidden(out, f)            ::
        Hidden(out, f)            ::
        Hidden(out, f)            ::
        Output(dim, f)            :: HNil,
        Settings(
          coordinator  = Node("localhost", 2552),
          learningRate = { case _ => 1E-10 },
          iterations   = 2000,
          prettyPrint  = true
        )
      )

    net.train(nodes)
  }

  val samples = 10

  def executors = {

    val xs = (1 to samples).toArray.map { i =>
      Array.fill(dim)(Gaussian.distribution((0.0, i.toDouble / samples.toDouble)).draw().abs)
    }

    val ys = (1 to samples).toArray.map { i =>
      val a = Array.fill(dim)(0.0)
      a.update(Random.nextInt(dim), 1.0)
      a
    }

    (1 to nodesC).par.foreach(i => DefaultExecutor(Node("localhost", 2552 + i), xs, ys))

  }

}
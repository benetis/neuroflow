package neuroflow.core

import breeze.linalg.{DenseMatrix, DenseVector}
import neuroflow.common._
import neuroflow.core.Network._
import shapeless._
import shapeless.ops.hlist._

import scala.annotation.implicitNotFound
import scala.collection._

/**
  * @author bogdanski
  * @since 03.01.16
  */

object Network extends TypeAliases {

  /**
    * Constructs a new [[Network]] with the respective [[Constructor]] in scope.
    * Additionally, it will prove that the architecture of the net is sound.
    */
  def apply[T <: Network[_], L <: HList](ls: L, settings: Settings = Settings())
                                     (implicit
                                      startsWith: L StartsWith Input,
                                      endsWith: L EndsWith Output,
                                      weightProvider: WeightProvider,
                                      constructor: Constructor[T],
                                      toList: L ToList Layer): T = {
    constructor(ls.toList, settings)
  }

}


/** For the sake of beauty. */
trait TypeAliases {

  type Data     = scala.Array[Double]
  type Vector   = scala.Vector[Double]
  type DVector  = DenseVector[Double]
  type Matrix   = DenseMatrix[Double]
  type Matrices = Array[Matrix]
  type Weights  = Array[Matrix]

}


/** A minimal constructor for a [[Network]]. */
@implicitNotFound("No network constructor in scope. Import your desired network or try: import neuroflow.nets.DefaultNetwork._")
trait Constructor[+T <: Network[_]] {
  def apply(ls: Seq[Layer], settings: Settings)(implicit weightProvider: WeightProvider): T
}


/** Distributed training node */
case class Node(host: String, port: Int)

/**
  * The `messageGroupSize` controls how many weights per batch will be sent.
  * The `frameSize` is the maximum message size for inter-node communication.
  */
case class Transport(messageGroupSize: Int, frameSize: String)


/**
  * The `verbose` flag indicates logging behavior.
  * The `learningRate` is a function from current iteration to learning rate, enabling dynamic rates.
  * The network will terminate either if `precision` is high enough or `iterations` is reached.
  * If `prettyPrint` is true, the layout will be rendered graphically.
  * The level of `parallelism` controls how many threads will be used for training.
  * For distributed training, `coordinator` and `transport` specific settings may be configured.
  * The `errorFuncOutput` option prints the error func graph to the specified file/closure.
  * When `regularization` is provided, the respective regulator will try to avoid over-fitting.
  * With `approximation`  the gradients will be approximated numerically.
  * With `partitions` a sequential training sequence can be partitioned for RNNs (0 index-based).
  * Some nets use specific parameters set in the `specifics` map.
  */
case class Settings(verbose: Boolean                            = true,
                    learningRate: PartialFunction[Int, Double]  = { case _ => 1E-4 },
                    precision: Double                           = 1E-5,
                    iterations: Int                             = 100,
                    prettyPrint: Boolean                        = false,
                    parallelism: Int                            = Runtime.getRuntime.availableProcessors,
                    coordinator: Node                           = Node("0.0.0.0", 2552),
                    transport: Transport                        = Transport(100000, "128 MiB"),
                    errorFuncOutput: Option[ErrorFuncOutput]    = None,
                    regularization: Option[Regularization]      = None,
                    approximation: Option[Approximation]        = None,
                    partitions: Option[Set[Int]]                = None,
                    specifics: Option[Map[String, Double]]      = None) extends Serializable


trait IllusionBreaker { self: Network[_] =>

  /**
    * Checks if the [[Settings]] are properly defined for this network.
    * Throws a [[neuroflow.core.IllusionBreaker.SettingsNotSupportedException]] if not. Default behavior is no op.
    */
  def checkSettings(): Unit = ()

}

object IllusionBreaker {

  class SettingsNotSupportedException(message: String) extends Exception(message)
  class NotSoundException(message: String) extends Exception(message)

}


trait Network[M] extends (M => M) with Logs with ErrorFuncGrapher with IllusionBreaker with Welcoming with Serializable {

  checkSettings()

  sayHi()

  val identifier: String

  /** Settings of this neural network. */
  val settings: Settings

  /** Layers of this neural network. */
  val layers: Seq[Layer]

  /** The weights are a bunch of matrices. */
  val weights: Weights

  /**
    * Computes output for given input `m`.
    * Alias for `net(x)` syntax.
    */
  def evaluate(m: M): M = apply(m)

  override def toString: String = weights.foldLeft("")(_ + "\n---\n" + _)

}


trait SupervisedTraining {

  /**
    * Takes a sequence of input vectors `xs` and trains this
    * network against the corresponding output vectors `ys`.
    */
  def train(xs: Array[Network.Data], ys: Array[Network.Data]): Unit
  def train(xs: Seq[Vector], ys: Seq[Vector]): Unit = train(xs.map(_.toArray).toArray, ys.map(_.toArray).toArray)

}


trait UnsupervisedTraining {

  /**
    * Takes a sequence of input vectors `xs` and trains this
    * network using the unsupervised learning strategy.
    */
  def train(xs: Array[Network.Data]): Unit
  def train(xs: Seq[Vector]): Unit = train(xs.map(_.toArray).toArray)

}


trait DistributedTraining {

  /**
    * Triggers execution of training for nodes `ns`.
    */
  def train(ns: collection.Set[Node])

}


trait FeedForwardNetwork extends Network[Vector] {

  override def checkSettings(): Unit = {
    if (settings.partitions.isDefined)
      warn("FFNs don't support partitions. This setting has no effect.")
  }

}


trait RecurrentNetwork extends Network[Seq[Vector]] {

  /**
    * Takes the input vector sequence `xs` to compute the mean output vector.
    */
  def evaluateMean(xs: Seq[Vector]): Vector =
    ~> (evaluate(xs)) map(res => res.reduce { (r, v) => r.zip(v).map { case (a, b) => a + b } } map { _ / res.size })

}

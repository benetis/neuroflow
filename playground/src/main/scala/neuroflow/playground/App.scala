package neuroflow.playground

/**
  * @author bogdanski
  * @since 03.01.16
  */

object App extends App {

  println("Run example (1-15): ")

  scala.io.StdIn.readInt() match {
    case  1 => XOR.apply
    case  2 => SigGap.apply
    case  3 => Trending.apply
    case  4 => AgeEarnings.apply
    case  5 => ImageRecognition.apply
    case  6 => DigitRecognition.apply
    case  7 => Sinusoidal.apply
    case  8 => AudioFileClassification.apply
    case  9 => LanguageProcessing.apply
    case 10 => LanguageProcessing.test
    case 11 => Sequences.apply
    case 12 => MovieSimilarity.apply
    case 13 => MovieSimilarity.find
    case 14 => ParityCluster.apply
    case 15 => PokeMonCluster.apply

    case  _ => sys.exit()
  }

}

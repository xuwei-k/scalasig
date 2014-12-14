/*     ___ ____ ___   __   ___   ___
**    / _// __// _ | / /  / _ | / _ \  Scala classfile decoder
**  __\ \/ /__/ __ |/ /__/ __ |/ ___/  (c) 2003-2013, LAMP/EPFL
** /____/\___/_/ |_/____/_/ |_/_/      http://scala-lang.org/
**
*/

package scala
package tools.scalap

import java.io.{ PrintStream, OutputStreamWriter, ByteArrayOutputStream }
import scala.reflect.NameTransformer
import scalax.rules.scalasig._

/**The main object used to execute scalap on the command-line.
 *
 * @author Matthias Zenger, Stephane Micheloud, Burak Emir, Ilya Sergey
 */
class Main(val printPrivates: Boolean = false) {
  val SCALA_SIG            = "ScalaSig"
  val SCALA_SIG_ANNOTATION = "Lscala/reflect/ScalaSignature;"
  val BYTES_VALUE          = "bytes"

  val versionMsg = "Scala classfile decoder %s -- %s\n".format(Properties.versionString, Properties.copyrightString)

  def isScalaFile(bytes: Array[Byte]): Boolean = {
    val byteCode  = ByteCode(bytes)
    val classFile = ClassFileParser.parse(byteCode)
    classFile.attribute("ScalaSig").isDefined
  }

  /**Processes the given Java class file.
   *
   * @param clazz the class file to be processed.
   */
  def processJavaClassFile(clazz: Classfile) {
    // construct a new output stream writer
    val out = new OutputStreamWriter(Console.out)
    val writer = new JavaWriter(clazz, out)
    // print the class
    writer.printClass
    out.flush()
  }

  def isPackageObjectFile(s: String) = s != null && (s.endsWith(".package") || s == "package")

  def parseScalaSignature(scalaSig: ScalaSig, isPackageObject: Boolean) = {
    val baos   = new ByteArrayOutputStream
    val stream = new PrintStream(baos)
    val syms   = scalaSig.topLevelClasses ++ scalaSig.topLevelObjects

    syms.head.parent match {
      // Partial match
      case Some(p) if (p.name != "<empty>") => {
        val path = p.path
        if (!isPackageObject) {
          stream.print("package ");
          stream.print(path);
          stream.print("\n")
        } else {
          val i = path.lastIndexOf(".")
          if (i > 0) {
            stream.print("package ");
            stream.print(path.substring(0, i))
            stream.print("\n")
          }
        }
      }
      case _ =>
    }
    // Print classes
    val printer = new ScalaSigPrinter(stream, printPrivates)
    syms foreach (printer printSymbol _)
    baos.toString
  }

  def decompileScala(bytes: Array[Byte], isPackageObject: Boolean): String = {
    val byteCode = ByteCode(bytes)
    val classFile = ClassFileParser.parse(byteCode)

    ScalaSigParser.parse(classFile) match {
      case Some(scalaSig) => parseScalaSignature(scalaSig, isPackageObject)
      case None           => ""
    }
  }

}

object Main extends Main(false) {

}

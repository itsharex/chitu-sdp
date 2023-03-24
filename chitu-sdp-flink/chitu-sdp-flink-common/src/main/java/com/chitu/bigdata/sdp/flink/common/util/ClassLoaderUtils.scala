
package com.chitu.bigdata.sdp.flink.common.util

import java.io.File
import java.net.{URL, URLClassLoader}
import java.util.function.Supplier

object ClassLoaderUtils extends Logger {

  /**
   * 指定 classLoader执行代码...
   *
   * @param targetClassLoader
   * @param func
   * @tparam R
   * @return
   */
  def runAsClassLoader[R](targetClassLoader: ClassLoader, func: () => R): R = {
    val originalClassLoader = Thread.currentThread.getContextClassLoader
    try {
      Thread.currentThread.setContextClassLoader(targetClassLoader)
      func()
    } catch {
      case e: Exception => throw e
    } finally {
      Thread.currentThread.setContextClassLoader(originalClassLoader)
    }
  }

  /**
   * 指定 classLoader执行代码...
   * for java
   *
   * @param targetClassLoader
   * @param supplier
   * @tparam R
   * @return
   */
  def runAsClassLoader[R](targetClassLoader: ClassLoader, supplier: Supplier[R]): R = {
    val originalClassLoader = Thread.currentThread.getContextClassLoader
    try {
      Thread.currentThread.setContextClassLoader(targetClassLoader)
      supplier.get()
    } catch {
      case e: Exception => throw e
    } finally {
      Thread.currentThread.setContextClassLoader(originalClassLoader)
    }
  }

  def loadJar(jarFilePath: String): Unit = {
    val jarFile = new File(jarFilePath)
    require(jarFile.exists, s"[SDP] jarFilePath:$jarFilePath is not exists")
    require(jarFile.isFile, s"[SDP] jarFilePath:$jarFilePath is not file")
    loadPath(jarFile.getAbsolutePath, List(".jar", ".zip"))
  }

  def loadJars(path: String): Unit = {
    val jarDir = new File(path)
    require(jarDir.exists, s"[SDP] jarPath: $path is not exists")
    require(jarDir.isDirectory, s"[SDP] jarPath: $path is not directory")
    require(jarDir.listFiles.length > 0, s"[SDP] have not jar in path:$path")
    jarDir.listFiles.foreach { x =>
      loadPath(x.getAbsolutePath, List(".jar", ".zip"))
    }
  }


  def loadResource(filepath: String): Unit = {
    val file = new File(filepath)
    addURL(file)
  }

  def loadResourceDir(filepath: String): Unit = {
    val file = new File(filepath)
    loopDirs(file)
  }

  private[this] def loadPath(filepath: String, ext: List[String]): Unit = {
    val file = new File(filepath)
    loopFiles(file, ext)
  }


  private[this] def loopDirs(file: File): Unit = { // 资源文件只加载路径
    if (file.isDirectory) {
      addURL(file)
      file.listFiles.foreach(loopDirs)
    }
  }


  private[this] def loopFiles(file: File, ext: List[String] = List()): Unit = {
    if (file.isDirectory) {
      file.listFiles.foreach(x => loopFiles(x, ext))
    } else {
      if (ext.isEmpty) {
        addURL(file)
      } else if (ext.exists(x => file.getName.endsWith(x))) {
        Utils.checkJarFile(file.toURI.toURL)
        addURL(file)
      }
    }
  }

  private[this] def addURL(file: File): Unit = {
    try {
      val classLoader = ClassLoader.getSystemClassLoader
      classLoader match {
        case c if c.isInstanceOf[URLClassLoader] =>
          val addURL = classOf[URLClassLoader].getDeclaredMethod("addURL", Array(classOf[URL]): _*)
          addURL.setAccessible(true)
          addURL.invoke(c, file.toURI.toURL)
        case _ =>
          val field = classLoader.getClass.getDeclaredField("ucp")
          field.setAccessible(true)
          val ucp = field.get(classLoader)
          val addURL = ucp.getClass.getDeclaredMethod("addURL", Array(classOf[URL]): _*)
          addURL.setAccessible(true)
          addURL.invoke(ucp, file.toURI.toURL)
      }
    } catch {
      case e: Exception => throw e
    }
  }


}

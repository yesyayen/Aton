package services.impl

import java.io.{BufferedReader, IOException, InputStreamReader}
import java.net.NoRouteToHostException
import java.sql.Timestamp
import java.util.Calendar

import com.google.inject.{Inject, Singleton}
import com.jcraft.jsch.{ChannelExec, JSch, JSchException}
import dao.{SSHOrderDAO, SSHOrderToComputerDAO}
import fr.janalyse.ssh.{Expect, SSHCommand, SSHOptions}
import model._
import services.SSHOrderService
import services.exec.SSHFunction._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.duration._

/**
  * Created by camilo on 14/05/16.
  */
@Singleton
class SSHOrderServiceImpl @Inject()(sSHOrderDAO: SSHOrderDAO, sSHOrderToComputerDAO: SSHOrderToComputerDAO) extends SSHOrderService {


  @throws(classOf[JSchException])
  override def execute(computer: Computer, sshOrder: SSHOrder): (String, Int) = {
    play.Logger.debug(s"""Executing: $sshOrder into: $computer""")
    val newSSHOrder = if(sshOrder.superUser){
      sshOrder.copy(command = sudofy(sshOrder.command))
    } else {
      sshOrder
    }

    val future = sSHOrderDAO.add(newSSHOrder).map {
      case Some(id) =>
        val settings = generateSSHSettings(computer, newSSHOrder)
        val (result, exitCode) = if (newSSHOrder.superUser) {
          executeWithSudoWorkaround(newSSHOrder, settings)
          //jassh.SSH.once(settings)(_.executeWithStatus("sudo " + sshOrder.command))
        } else {
          jassh.SSH.once(settings)(_.executeWithStatus(newSSHOrder.command))
        }
        //play.Logger.debug("ID: " + id)
        val resultSSHOrder = SSHOrderToComputer(computer.ip, id, now, Some(result), Some(exitCode))
        sSHOrderToComputerDAO.add(resultSSHOrder)
        (result, exitCode)
      case _ =>
        ("", 0)
    }
    Await.result(future, Duration.Inf)
  }


  def executeUntilResult(computer: Computer, sshOrders: Seq[SSHOrder]): (String, Int) = {
    play.Logger.debug(s"""Executing: $sshOrders into: $computer""")
    val joinedSSHOrder = sshOrders.headOption match {
      case Some(sshOrder) =>
        sshOrder.copy(command = sshOrders.map(_.command).mkString(" & "))
      case _ =>
        return ("", 1)
    }
    val future = sSHOrderDAO.add(joinedSSHOrder).map {
      case Some(id) =>
        val settings = generateSSHSettings(computer, joinedSSHOrder)
        val (result, exitCode) = if (joinedSSHOrder.superUser) {
          executeWithSudoWorkaround(joinedSSHOrder, settings)
          jassh.SSH.shell(settings) { ssh =>
            /*ssh.executeWithExpects("sudo -S su", List(new Expect(_.contains("password"), settings.password.password.getOrElse(""))))
            ssh.become("root", settings.password.password)*/

            ssh.executeWithExpects("""SUDO_PROMPT="prompt" sudo -S su -""", List(new Expect(_.endsWith("prompt"), settings.password.password.getOrElse(""))))
            val (result, exitCode) = ssh.executeWithStatus(joinedSSHOrder.command)
            ssh.execute("exit")
            (result, exitCode)
          }
          //jassh.SSH.once(settings)(_.executeWithStatus("sudo " + sshOrder.command))
        } else {
          jassh.SSH.once(settings) { ssh =>
            var (result, exitStatus) = ("", 1)
            val commands = sshOrders.map(_.command)
            var i = 0
            while (i < commands.size && result == "" && exitStatus != 0) {
              val command = commands(i)
              val (newResult, newExit) = ssh.executeWithStatus(SSHCommand.stringToCommand(command))
              result = newResult
              exitStatus = newExit
              i += 1
            }
            (result, exitStatus)
          }
        }
        val resultSSHOrder = SSHOrderToComputer(computer.ip, id, now, Some(result), Some(exitCode))
        sSHOrderToComputerDAO.add(resultSSHOrder)
        (result, exitCode)
      case _ =>
        ("", 0)
    }
    Await.result(future, 30 seconds)
  }

  override def getMac(computer: Computer, operatingSystem: Option[String])(implicit username: String): Option[String] = {
    //play.Logger.debug(s"""Looking for mac of "${computer.ip}"""")
    val orders = operatingSystem match {
      case Some(os) => macOrders(translateOS(os))
      case _ => macOrders("")
    }
    //play.Logger.debug(s"""Trying "${order}"""")
    try {
      val (result, a) = executeUntilResult(computer, orders.map(new SSHOrder(now, _, username)))
      //play.Logger.debug(s"""Result: $result""")
      if (result != "") {
        Some(result)
      } else {
        None
      }
    } catch {
      case e: JSchException => None
      case e: Exception => play.Logger.error("An error occurred while looking for computer's mac: " + computer, e)
        None
    }

  }

  @throws[JSchException]
  override def shutdown(computer: Computer)(implicit username: String): Boolean = {
    val (_, _) = execute(computer, new SSHOrder(now, superUser = true, interrupt = false, command = shutdownOrder, username = username))
    true
  }

  private def now = new Timestamp(Calendar.getInstance().getTime.getTime)

  @throws[JSchException]
  override def upgrade(computer: Computer,computerState: ComputerState)(implicit username: String): (String, Boolean) = {
    val order = upgradeOrder(translateOS(computerState.operatingSystem.getOrElse("")))
    val (result, exitCode) = execute(computer, new SSHOrder(now, superUser = true, interrupt = false, command = order, username = username))
    if (exitCode == 0) {
      ("", true)
    } else {
      (result, false)
    }
  }

  @throws[JSchException]
  override def unfreeze(computer: Computer)(implicit username: String): (String, Boolean) = ???

  @throws[JSchException]
  override def getOperatingSystem(computer: Computer)(implicit username: String) = {
    try {
      val (result, exitCode) = execute(computer, new SSHOrder(now, superUser = false, interrupt = true, command = operatingSystemCheck, username = username))
      if (exitCode == 0) Some(result) else None
    }
    catch {
      case e: Exception => None
    }
  }


  override def check(computer: Computer)(implicit username: String): (ComputerState, Seq[ConnectedUser]) = {
    //play.Logger.debug(s"""Checking the $computer's state""")
    //play.Logger.debug(s"""Checking if $computer's on""")
    try {
      val state = checkState(computer)
      play.Logger.debug(s"""$computer is  $state""")
      val date = now
      val (operatingSystem, mac, whoIsUsing) = if (state != Connected()) {
        (None, None, Seq.empty)
      } else {
        val os = getOperatingSystem(computer)
        (os, getMac(computer, os), whoAreUsing(computer).map { username => ConnectedUser(0, username, computer.ip, date) })
      }
      (ComputerState(computer.ip, date, state.id, operatingSystem, mac), whoIsUsing)
    } catch {
      case e: Exception => play.Logger.error(s"There was an error checking $computer's state")
        (ComputerState(computer.ip, now, NotConnected().id, None, None), Seq.empty)
    }
  }

  override def checkState(computer: Computer)(implicit username: String): StateRef = {
    val sSHOrder = new SSHOrder(now, false, false, dummy, username)
    val settings = generateSSHSettings(computer, sSHOrder)
    try {
      val isConnected = jassh.SSH.once(settings)(_.executeWithStatus(sSHOrder.command)._1 == "Ping from Aton")
      if (isConnected) {
        Connected()
      } else {
        NotConnected()
      }
    } catch {
      case ex: JSchException =>
        ex.getMessage match {
          case "Auth fail" => AuthFailed()
          case "timeout: socket is not established" => NotConnected()
          case "Session.connect: java.net.SocketTimeoutException: Read timed out" => NotConnected()
          case "java.net.NoRouteToHostException: No route to host" => NotConnected()
          case e => play.Logger.error(s"The error checking $computer was : " + e)
            UnknownError()
        }
      case e: NoRouteToHostException =>play.Logger.error("There was an error checking for " + computer + "'s state", e)
        NotConnected()
      case e: Exception => play.Logger.error("There was an error checking for " + computer + "'s state", e)
        UnknownError()
    }
  }

  private def generateSSHSettings(computer: Computer, sSHOrder: SSHOrder) = SSHOptions(host = computer.ip, username = computer.SSHUser, password = computer.SSHPassword, connectTimeout = 10000, prompt = Some("prompt"))

  @throws[JSchException]
  override def whoAreUsing(computer: Computer)(implicit username: String): Seq[String] = {
    try {
      val (result, _) = execute(computer, new SSHOrder(now, false, false, userListOrder, username))
      for (user <- result.split("\n") if user != "") yield user
    } catch {
      case e: Exception => Seq()
    }
  }

  private def executeWithSudoWorkaround(sshOrder: SSHOrder, settings: SSHOptions): (String,Int) = {
    play.Logger.debug(s"""Trying sudo workaround with $sshOrder""")
    settings.password.password match {
      case Some(password) =>
        val jsch = new JSch()
        val sshSession = jsch.getSession(settings.username, settings.host, settings.port)
        sshSession.setConfig("StrictHostKeyChecking", "no")
        sshSession.setPassword(password)
        sshSession.connect()
        play.Logger.debug("Connected!")
        val channel: ChannelExec = sshSession.openChannel("exec").asInstanceOf[ChannelExec]
        play.Logger.debug("Channel created")
        val (result, exitCode) = try {
          val stream = channel.getInputStream
          val out = channel.getOutputStream
          val err = channel.getErrStream
          channel.setCommand(sshOrder.command)
          play.Logger.debug("Command sent")
          channel.connect()
          play.Logger.debug("Channel connected")
          out.write((password + "\n").getBytes())
          play.Logger.debug("Wrote seccond password")
          out.flush()
          play.Logger.debug("Everything sent")
          if (sshOrder.interrupt) {
            Thread.sleep(1000)
            out.write("~.\n".getBytes())
            out.flush()
            play.Logger.debug("Interrupted succesfully")
            ("Interrupted succesfully", 0)
          } else {
            play.Logger.debug("Reading output")
            val readerInput = new BufferedReader(new InputStreamReader(stream))
            val readerErr = new BufferedReader(new InputStreamReader(err))
            val lines = Stream.continually(readerInput.readLine()).takeWhile{ line=>
                play.Logger.debug(line)
                line != null
            }.toList:::Stream.continually(readerErr.readLine()).takeWhile(_ != null).toList
            play.Logger.debug(s"Lines gotten: $lines")
            (lines.mkString("\n"), 0)
          }
        } catch {
          case e: IOException =>
            play.Logger.error("An exception occurred while creating stream executing a sudo action", e)
            ("Error, check aton log", 1)
          case e: JSchException =>
            play.Logger.error("SSH Exception ocurred while executing ssh sudo action", e)
            ("Error, check aton log", 1)
          case e: Exception =>
            play.Logger.error("Something unexpected happened executing with sudo", e)
            ("Error, check aton log", 1)
        }
        val exitStatus = channel.getExitStatus
        play.Logger.debug(s"Exit status: $exitStatus")
        if (channel != null) {
          channel.disconnect()
          play.Logger.debug("Channel disconnected")
        }
        if (sshSession != null) {
          play.Logger.debug("Session disconnected")
          sshSession.disconnect()
        }
        (result, exitStatus)
      case _ =>
        play.Logger.error("There is not a password for executing")
        ("There is not a password for executing",1)
    }

  }

  override def execute(computer: Computer, superUser: Boolean, command: String)(implicit username: String): (String, Int) = {
    execute(computer, new SSHOrder(now, superUser, false, command, username))
  }

  override def blockPage(computer: Computer, page: String)(implicit username: String): (String,Int) = {
    execute(computer, new SSHOrder(now,superUser = true,interrupt= false,blockPageOrder(page),username ))
  }

  override def sendMessage(computer: Computer, message: String, users: Seq[ConnectedUser])(implicit username: String): Unit = {

    users.map{user=>
      try {
        execute(computer, new SSHOrder(now,superUser = false, interrupt= false, notificationOrder(user.username,message),username))
      } catch {
        case e: JSchException => play.Logger.error(s"There was a SSH error sending messages to the $computer",e)
          ("There was a SSH error sending messages",1)
        case e: Exception =>play.Logger.error(s"There was a non SSH error sending messages to the $computer",e)
          ("There was a non SSH error sending messages",1)
      }
    }

  }
}

package todo.mining

import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import net.ceedubs.ficus.readers.ValueReader
import scorex.core.ModifierId
import scorex.core.settings.ScorexSettings.readConfigFromPath
import scorex.core.settings._
import scorex.core.utils.ScorexLogging

import scala.concurrent.duration._

case class AppSettings(mining: MiningSettings,
                       scorexSettings: ScorexSettings)

case class MiningSettings(offlineGeneration: Boolean,
                          targetBlockDelay: FiniteDuration,
                          blockGenerationDelay: FiniteDuration,
                          posAttachmentSize: Int,
                          rParamX10: Int,
                          initialDifficulty: BigInt) {
  lazy val MaxTarget = BigInt(1, Array.fill(32)(Byte.MinValue))
  lazy val GenesisParentId = ModifierId @@ Array.fill(32)(1: Byte)
}

object AppSettings extends ScorexLogging with SettingsReaders {
  def read(userConfigPath: Option[String]): AppSettings = {
    fromConfig(readConfigFromPath(userConfigPath, "scorex"))
  }

  implicit val networkSettingsValueReader: ValueReader[AppSettings] =
    (cfg: Config, path: String) => fromConfig(cfg.getConfig(path))

  private def fromConfig(config: Config): AppSettings = {
    log.info(config.toString)
    val miningSettings = config.as[MiningSettings]("scorex.miner")
    val scorexSettings = config.as[ScorexSettings]("scorex")
    AppSettings(miningSettings, scorexSettings)
  }
}


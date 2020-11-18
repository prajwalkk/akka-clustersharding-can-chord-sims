package com.chord.akka.utils
import net.jcazevedo.moultingyaml._
/*
*
* Created by: prajw
* Date: 18-Nov-20
*
*/
object MyYamlDataProtocol extends DefaultYamlProtocol {
  implicit val dataProperties = yamlFormat3(YamlDumpDataHolder.apply)
}

package com.chord.akkasharding.utils

import net.jcazevedo.moultingyaml._

/*
*
* Created by: prajw
* Date: 15-Nov-20
*
*/
object MyYamlProtocol extends DefaultYamlProtocol {
  implicit val fingerTableEntityFormat = yamlFormat3(YamlDumpFingerTableEntity.apply)
  implicit val nodeProperties = yamlFormat7(YamlDumpNodeProps.apply)
  implicit val mainProperties = yamlFormat2(YamlDumpMainHolder)
}
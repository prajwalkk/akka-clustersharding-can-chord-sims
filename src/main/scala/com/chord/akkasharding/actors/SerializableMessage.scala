package com.chord.akkasharding.actors

import com.fasterxml.jackson.annotation.JsonTypeInfo
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, property = "type")
trait SerializableMessage

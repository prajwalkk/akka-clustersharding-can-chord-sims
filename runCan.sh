#!/usr/bin/env bash
set -x
sbt "runMain com.can.akka.CAN_SimulationDriver -Dakka.http.server.default-http-port=8000 -Dakka.remote.artery.canonical.port=2551 -Dakka.management.http.port=8558"
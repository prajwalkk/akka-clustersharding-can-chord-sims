package Server

import Utils.Helper

import collection.mutable.Map

class Server(val name:String) {

  val bitSize:Int = 24
  val identity:Identity = new Identity(name,Helper.getIdentifier(name))
  var fingerTable:List[FingerTableEntity] = List()
  var dataMap:Map[String,String] = Map[String,String]()


  def lookup(key:String):Server={

//    lookup logic which will give the name of server where this key will sit.
    this  // has to be change
  }

  def getValue(key:String):Option[String]=dataMap.get(key)

  def insertData(key:String,value:String)={
    dataMap +(key -> value)
  }


  def findSuccessor:Server={

    this // has to be changed

  }


  def findPredecessor:Server={
    this // has to be changed

  }

  def initializeFingerTable:Unit={


    // logic for filling the finger table
    for(i <- 1 to bitSize-1){
      val start:Int = identity.identifier + Math.pow(2,i-1).asInstanceOf[Int]
      val end :Int = identity.identifier + Math.pow(2,i).asInstanceOf[Int]
      val interval:Array[Int] = Array(start,end)
//      fingerTable = fingerTable.appended(new FingerTableEntity(interval,))

    }


      }
}


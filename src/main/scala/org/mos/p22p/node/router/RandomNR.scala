package org.mos.p22p.node.router

import com.google.protobuf.{ByteString, Message}
import onight.oapi.scala.traits.OLog

import scala.collection.mutable.Set
import scala.math.BigInt

object RandomNR extends MessageRouter with OLog {
  def getRand() = Math.random(); //DHTConsRand.getRandFactor()

  override def routeMessage(gcmd:String,body: Either[Message,ByteString],priority:Byte)(implicit from: Node, //
    nextHops: IntNode = FullNodeSet(),
    network: Network,messageid:String) {
    //    log.debug("routeMessage:from=" + from.node_idx + ",next=" + nextHops)
    nextHops match {
      case fs: FlatSet =>
        val nextHopsCount = fs.nextHops.bitCount
        val (directCount, eachsetCount) = getDiv(nextHopsCount);
        //        var ran = ((getRand() * nextHopsCount) % directCount).asInstanceOf[Int];
        val mapSets = scala.collection.mutable.Map.empty[Int, BigInt]; //leader==>follow
        val startNodeSets = Set.empty[(Int, Node)]; //leader==>follow
        val offset = (getRand() * nextHopsCount).asInstanceOf[Int];
        var i: Int = offset;
        //        log.debug("nextHopsCount==" + nextHopsCount + ",directCount=" + directCount + ",eachsetCount=" + eachsetCount)
        //ffmapSets.clear()
        network.directNodes.filter(node => fs.nextHops.testBit(node.node_idx) && from.node_idx != node.node_idx)
          .map({ node =>
            i = i + 1;
            var setid = ((i) % directCount);
//            if (fs.nextHops.bitCount == network.node_bits.bitCount)
//              println("node to broadcast==" + node + ",setid=" + setid)
            mapSets.get(setid) match {
              case Some(v: BigInt) =>
                if (!v.testBit(node.node_idx)) {
                  mapSets.put(setid, v.setBit(node.node_idx));
                  //            v.nextHops = v.nextHops.setBit(node.node_idx);
                }
              case _ =>
                mapSets.put(setid, BigInt(0));
                startNodeSets.add((setid, node))
            }
          })
        if (fs.nextHops.bitCount == network.node_bits.bitCount) {
//          log.debug("nextHopsCount==" + nextHopsCount + ",directCount=" + directCount + ",eachsetCount=" + eachsetCount + ",offset=" + offset)
//          println("startNodes==" + startNodeSets + ",mapSets=" + mapSets)
        }
        startNodeSets.map { sn =>
          val (setid, node) = sn
          broadcastMessage(gcmd,body,node,priority)(node,
            FlatSet(node.node_idx, mapSets.getOrElse(setid, BigInt(0))), network,messageid)
        }
      //
      case n @ _ =>
        log.warn("cannot route not flat Set:" + n)
    }

  }

  def getDiv(n: Int): (Int, Int) = {
    val d: Int = Math.sqrt(n.asInstanceOf[Double]).asInstanceOf[Int];
    var i = d;
    for (i <- d until 1 by -1) {
      if (n % i == 0) {
        //        log.debug("d=" + d + "==>" + (i, n / i));
        return (i, n / i);
      }
    }
    //    log.debug("d.1=" + d + "==>" + (1, n));
    (1, n);
  }

}
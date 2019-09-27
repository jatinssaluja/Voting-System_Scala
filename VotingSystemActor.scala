package playground

import akka.actor._


object VotingSystemActor extends App{

  val actorSystem = ActorSystem("VotingSystem")
  println(actorSystem.name)


  /***  Stateless Simple Voting System ***/


  object Citizen{
    case class Vote(candidate: String)
    case object VoteStatusRequest

  }

  class Citizen extends Actor{
    import Citizen._
    import VoteAggregator._

    override def receive: Receive = voted(None)

    def voted(candidate:Option[String]):Receive = {

      case Vote(name) => context.become(voted(Some(name)))
      case VoteStatusRequest => sender() ! VoteStatusReply(candidate)

    }
  }

  object VoteAggregator{
    case class AggregateVotes(citizens:Set[ActorRef])
    case class VoteStatusReply(candidate:Option[String])
  }

  class VoteAggregator extends Actor {

    import Citizen._
    import VoteAggregator._

    override def receive: Receive = awaitingCommand

    def awaitingCommand: Receive = {

      case AggregateVotes(citizens) =>
        citizens.foreach(citizen => citizen ! VoteStatusRequest)
        context.become(awaitingStatuses(citizens, Map()))

    }

    def awaitingStatuses(citizensNotVoted: Set[ActorRef], currentStats: Map[String, Int]):Receive = {

      case VoteStatusReply(candidate) =>
        candidate match {
          case Some(value) =>
            val newCitizensNotVoted:Set[ActorRef] = citizensNotVoted - sender()
            val currentVotesOfCandidate = currentStats.getOrElse(value, 0)
            val newCurrentStats = currentStats + (value -> (currentVotesOfCandidate + 1))
            if (newCitizensNotVoted.isEmpty) {
              println(s"[AggregateVotes] Final Voting Result $newCurrentStats")
            } else{
              context.become(awaitingStatuses(newCitizensNotVoted, newCurrentStats))
            }
          case None => sender() ! VoteStatusRequest


        }
    }
  }

  import Citizen._
  import VoteAggregator._

  val alice = actorSystem.actorOf(Props[Citizen],"alice")
  val bob = actorSystem.actorOf(Props[Citizen],"bob")
  val charlie = actorSystem.actorOf(Props[Citizen],"charlie")
  val daniel = actorSystem.actorOf(Props[Citizen],"daniel")

  alice ! Vote("Andrew")
  bob ! Vote("Jonas")
  charlie ! Vote("Stephen")
  daniel ! Vote("Stephen")

  val aggregator = actorSystem.actorOf(Props[VoteAggregator],"aggregator")
  aggregator ! AggregateVotes(Set(alice,bob,charlie,daniel))





  /***  Stateful Simple Voting System ***/

  /*

  object Citizen{
    case class Vote(candidate: String)
    case object VoteStatusRequest

  }

  class Citizen extends Actor{
    import Citizen._
    import VoteAggregator._

    var candidate:Option[String] = None

    override def receive: Receive = {
      case Vote(name) => candidate = Some(name)
      case VoteStatusRequest => sender() ! VoteStatusReply(candidate)
    }
  }

  object VoteAggregator{
    case class AggregateVotes(citizens:Set[ActorRef])
    case class VoteStatusReply(candidate:Option[String])
  }

  class VoteAggregator extends Actor{
    import Citizen._
    import VoteAggregator._

    var citizensNotVoted:Set[ActorRef] = Set()
    var currentStats:Map[String,Int] = Map()

    override def receive: Receive = {

      case AggregateVotes(citizens) =>
        citizensNotVoted = citizens
        citizensNotVoted.foreach(citizen=>citizen ! VoteStatusRequest)

      case VoteStatusReply(candidate) =>
        candidate match{
          case Some(value) =>
            citizensNotVoted = citizensNotVoted - sender()
            val currentVotesOfCandidate = currentStats.getOrElse(value,0)
            currentStats = currentStats + (value -> (currentVotesOfCandidate+1))
            if(citizensNotVoted.isEmpty){
              println(s"[AggregateVotes] Final Voting Result $currentStats")
            }
          case None => sender() ! VoteStatusRequest
        }


    }
  }

  import Citizen._
  import VoteAggregator._

  val alice = actorSystem.actorOf(Props[Citizen],"alice")
  val bob = actorSystem.actorOf(Props[Citizen],"bob")
  val charlie = actorSystem.actorOf(Props[Citizen],"charlie")
  val daniel = actorSystem.actorOf(Props[Citizen],"daniel")

  alice ! Vote("Andrew")
  bob ! Vote("Jonas")
  charlie ! Vote("Stephen")
  daniel ! Vote("Stephen")

  val aggregator = actorSystem.actorOf(Props[VoteAggregator],"aggregator")
  aggregator ! AggregateVotes(Set(alice,bob,charlie,daniel))


   */

}

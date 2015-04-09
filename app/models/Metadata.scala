package models
import play.api.Logger
import java.net.URL
import java.util.Date
import play.api.libs.json.JsArray
import play.api.libs.json.JsObject
import play.api.libs.json.Writes
import play.api.libs.json.Reads
import play.api.libs.json.Json
import play.api.libs.json.JsValue
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsError
import play.api.libs.json.JsResult
import play.api.data.validation.ValidationError
/**
 * A piece of metadata for a section/file/dataset/collection/space
 * 
 * @author Luigi Marini
 * @author Smruti Padhy
 * @author Inna Zharnitsky
 *
 **/
case class Metadata (
  id : UUID = UUID.generate,
  attachedTo: Map[String, UUID], //metadata attached to an ElementType e.g. file, dataset, collection, space
                                 //e.g. (file_id->UUID)
  contextId : Option[UUID] = None,
  createdAt : Date,
  creator : Agent,
  content  : JsValue,
  version : Option[String] = None
)


trait Agent {
  val id: UUID
  def typeOfAgent: String
  def typeOfAgent_= (s: String): Unit
}

// User through the GUI
case class UserAgent( id: UUID, var typeOfAgent : String = "user", userId: Option[URL]) extends Agent

// Automatic extraction
case class ExtractorAgent( id: UUID, var typeOfAgent : String = "extractor" , extractorId: Option[URL]) extends Agent

object Agent {
  implicit object AgentReads extends Reads[Agent] {
    def reads(json: JsValue) = {
      //creator(agent) may be User or Extractor depending on the json 
      var creator: Option[models.Agent] = None
      
      //parse json input for type of agent
      val typeOfAgent = (json \ "agent" \ "@type").toString

      //if user_id is part of the request, then creator is a user
      val user_id = (json \ "agent" \ "user_id").asOpt[String]
      user_id map { uid =>
        val userId = Some(new URL(uid))
        creator = Some(UserAgent(UUID.generate, typeOfAgent, userId))
      }

      //if extractor_id is part of the request, then creator is an extractor
      val extr_id = (json \ "agent" \ "extractor_id").asOpt[String]
      extr_id map { exid =>
        val extractorId =  Some(new URL(exid))
        creator = Some(ExtractorAgent(UUID.generate, typeOfAgent, extractorId))
      }

      //if creator is still None - wrong user input
      creator match {
        case Some(c) => JsSuccess(c)
        case None => JsError(ValidationError("could not get creator"))
      }
    }
  }
}

object Metadata{
  implicit object ExtractorAgentWrites extends Writes[ExtractorAgent] {
    def writes(extractor: ExtractorAgent): JsObject = {
      val extractor_id_string = extractor.extractorId.map(_.toString).getOrElse("")
      Json.obj(
        "@type" -> "cat:extractor",
        "extractor_id" -> extractor_id_string)
    }
  }
 
  implicit object UserAgentWrites extends Writes[UserAgent] {
    def writes(user: UserAgent): JsObject = {
      val user_id_string = user.userId.map(_.toString).getOrElse("")
      Json.obj(
        "@type" -> "cat:user",
        "user_id" -> user_id_string)
    }
  }
	
  implicit object MetadataWrites extends Writes[Metadata] {
		def writes(metadata: Metadata) = Json.obj(		    
				"created_at" -> metadata.createdAt.toString,
				//if (i == 1) x else y
				//switch based on type of creator/agent and call appropriate class' implicit writes
				"agent"-> (if (metadata.creator.isInstanceOf[UserAgent]) metadata.creator.asInstanceOf[UserAgent] else metadata.creator.asInstanceOf[ExtractorAgent]) ,
				"content" -> metadata.content
				)
	}
}

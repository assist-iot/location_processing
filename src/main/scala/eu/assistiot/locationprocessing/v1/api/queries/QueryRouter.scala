package eu.assistiot.locationprocessing.v1.api.queries

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.Route
import eu.assistiot.locationprocessing.v1.api.queries.data.Query
import eu.assistiot.locationprocessing.v1.api.queries.data.dto.QueryDTO
import eu.assistiot.locationprocessing.v1.api.queries.utils.CustomSprayJsonSupport
import eu.assistiot.locationprocessing.v1.api.utils.Routable
import fr.davit.akka.http.metrics.core.scaladsl.server.HttpMetricsDirectives.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import spray.json.*

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success

object QueryRouter {
  sealed trait Response

  sealed trait GetQueryResponse extends Response
  final case class QueryResponse(query: QueryDTO) extends GetQueryResponse
  final case class QueryNotFoundResponse(description: String) extends GetQueryResponse
  final case class QueryNotRetrievedResponse(description: String) extends GetQueryResponse

  sealed trait GetAllQueriesResponse extends Response
  final case class QueriesResponse(queries: Seq[QueryDTO]) extends GetAllQueriesResponse
  final case class QueriesNotRetrievedResponse(description: String) extends GetAllQueriesResponse

  sealed trait CreateQueryResponse extends Response
  final case class QueryCreatedResponse(info: String, query: QueryDTO) extends CreateQueryResponse
  final case class QueryNotCreatedResponse(description: String) extends CreateQueryResponse

  sealed trait UpdateQueryResponse extends Response
  final case class QueryUpdatedResponse(info: String, query: QueryDTO) extends UpdateQueryResponse
  final case class QueryNotUpdatedResponse(description: String) extends UpdateQueryResponse

  sealed trait DeleteQueryResponse extends Response
  final case class QueryDeletedResponse(info: String, deletedQueriesCount: Int)
      extends DeleteQueryResponse
  final case class QueryNotDeletedResponse(description: String) extends DeleteQueryResponse

  sealed trait RunQueryForInputResponse extends Response
  final case class RunQueryForInputSuccessResponse(output: String) extends RunQueryForInputResponse
  final case class RunQueryForInputFailureResponse(description: String)
      extends RunQueryForInputResponse
}

@Path("/queries")
class QueryRouter(activeQueriesState: ActiveQueriesState)(implicit val system: ActorSystem[_])
    extends Routable
    with SprayJsonSupport
    with CustomSprayJsonSupport {
  import QueryRouter._

  def getQuery(name: String): GetQueryResponse =
    QueryService.getQuery(name, activeQueriesState) match {
      case Success(someQuery) =>
        someQuery match {
          case Some(query) => QueryResponse(query)
          case None        => QueryNotFoundResponse(s"Query $name not found")
        }
      case Failure(exception) => QueryNotRetrievedResponse(exception.getMessage)
    }

  @GET
  @Path("/{name}")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(
    summary = "Retrieves the query with the given name",
    operationId = "getQuery",
    parameters =
      Array(new Parameter(name = "name", in = ParameterIn.PATH, description = "Query name")),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "The requested query",
        content = Array(new Content(schema = new Schema(implementation = classOf[QueryResponse])))
      ),
      new ApiResponse(
        responseCode = "404",
        description = "Query not found",
        content =
          Array(new Content(schema = new Schema(implementation = classOf[QueryNotFoundResponse])))
      ),
      new ApiResponse(
        responseCode = "500",
        description = "Internal server error",
        content = Array(
          new Content(schema = new Schema(implementation = classOf[QueryNotRetrievedResponse]))
        )
      )
    )
  )
  def getQueryRoute: Route =
    pathPrefixLabeled(Segment, ":id") { name =>
      get {
        getQuery(name) match {
          case response @ QueryResponse(_) =>
            complete(HttpResponse(entity = HttpEntity(response.toJson.prettyPrint)))
          case response @ QueryNotFoundResponse(_) =>
            complete(
              HttpResponse(
                status = StatusCodes.NotFound,
                entity = HttpEntity(response.toJson.prettyPrint)
              )
            )
          case response @ QueryNotRetrievedResponse(_) =>
            complete(
              HttpResponse(
                status = StatusCodes.InternalServerError,
                entity = HttpEntity(response.toJson.prettyPrint)
              )
            )
        }
      }
    }

  def getAllQueries: GetAllQueriesResponse = {
    QueryService.getAllQueries(activeQueriesState) match {
      case Success(queries)   => QueriesResponse(queries)
      case Failure(exception) => QueriesNotRetrievedResponse(exception.getMessage)
    }

  }

  @GET
  @Consumes(Array(MediaType.APPLICATION_JSON))
  @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(
    summary = "Retrieves all queries",
    operationId = "getAllQueries",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "All queries",
        content = Array(new Content(schema = new Schema(implementation = classOf[QueriesResponse])))
      ),
      new ApiResponse(
        responseCode = "500",
        description = "Internal server error",
        content = Array(
          new Content(schema = new Schema(implementation = classOf[QueriesNotRetrievedResponse]))
        )
      )
    )
  )
  def getAllQueriesRoute: Route =
    get {
      getAllQueries match {
        case response @ QueriesResponse(_) =>
          complete(HttpResponse(entity = HttpEntity(response.toJson.prettyPrint)))
        case response @ QueriesNotRetrievedResponse(_) =>
          complete(
            HttpResponse(
              status = StatusCodes.InternalServerError,
              entity = HttpEntity(response.toJson.prettyPrint)
            )
          )
      }
    }

  def createQuery(query: Query): CreateQueryResponse =
    QueryService.createQuery(query) match {
      case Success(createdQuery) =>
        QueryCreatedResponse("Query created. Restart the enabler.", createdQuery)
      case Failure(exception) =>
        QueryNotCreatedResponse(s"Query not created: $exception")
    }

  @POST
  @Consumes(Array(MediaType.APPLICATION_JSON))
  @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(
    summary = "Creates a new query",
    operationId = "createQuery",
    requestBody = new RequestBody(
      required = true,
      content = Array(new Content(schema = new Schema(implementation = classOf[Query])))
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "201",
        description = "Query created",
        content =
          Array(new Content(schema = new Schema(implementation = classOf[QueryCreatedResponse])))
      ),
      new ApiResponse(
        responseCode = "400",
        description = "Query not created",
        content = Array(
          new Content(schema = new Schema(implementation = classOf[QueryNotCreatedResponse]))
        )
      )
    )
  )
  def createQueryRoute: Route =
    pathEndOrSingleSlash {
      post {
        entity(as[Query]) { query =>
          createQuery(query) match {
            case response @ QueryCreatedResponse(_, _) =>
              complete(
                HttpResponse(
                  status = StatusCodes.Created,
                  entity = HttpEntity(response.toJson.prettyPrint)
                )
              )
            case response @ QueryNotCreatedResponse(_) =>
              complete(
                HttpResponse(
                  status = StatusCodes.BadRequest,
                  entity = HttpEntity(response.toJson.prettyPrint)
                )
              )
          }
        }
      }
    }

  def updateQuery(name: String, query: Query): UpdateQueryResponse =
    QueryService.updateQuery(name, query) match {
      case Success(updatedQuery) =>
        QueryUpdatedResponse("Query updated. Restart the enabler.", updatedQuery)
      case Failure(exception) =>
        QueryNotUpdatedResponse(s"Query not updated: $exception")
    }

  @PUT
  @Path("/{name}")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(
    summary = "Update the query with the given name",
    operationId = "updateQuery",
    parameters =
      Array(new Parameter(name = "name", in = ParameterIn.PATH, description = "Query name")),
    requestBody = new RequestBody(
      required = true,
      content = Array(new Content(schema = new Schema(implementation = classOf[Query])))
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "201",
        description = "Query updated",
        content =
          Array(new Content(schema = new Schema(implementation = classOf[QueryUpdatedResponse])))
      ),
      new ApiResponse(
        responseCode = "400",
        description = "Query not updated",
        content = Array(
          new Content(schema = new Schema(implementation = classOf[QueryNotUpdatedResponse]))
        )
      )
    )
  )
  def updateQueryRoute: Route =
    pathPrefixLabeled(Segment, ":id") { name =>
      put {
        entity(as[Query]) { query =>
          updateQuery(name, query) match {
            case response @ QueryUpdatedResponse(_, _) =>
              complete(
                HttpResponse(
                  status = StatusCodes.Created,
                  entity = HttpEntity(response.toJson.prettyPrint)
                )
              )
            case response @ QueryNotUpdatedResponse(_) =>
              complete(
                HttpResponse(
                  status = StatusCodes.BadRequest,
                  entity = HttpEntity(response.toJson.prettyPrint)
                )
              )
          }
        }
      }
    }

  def deleteQuery(name: String): DeleteQueryResponse =
    QueryService.deleteQuery(name) match {
      case Success(deletedQueriesCount) =>
        QueryDeletedResponse("Query deleted. Restart the enabler.", deletedQueriesCount)
      case Failure(exception) =>
        QueryNotDeletedResponse(s"Query not deleted: $exception")
    }

  @DELETE
  @Path("/{name}")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(
    summary = "Delete the query with the given name",
    operationId = "deleteQuery",
    parameters =
      Array(new Parameter(name = "name", in = ParameterIn.PATH, description = "Query name")),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Query deleted",
        content =
          Array(new Content(schema = new Schema(implementation = classOf[QueryDeletedResponse])))
      ),
      new ApiResponse(
        responseCode = "400",
        description = "Query not deleted",
        content = Array(
          new Content(schema = new Schema(implementation = classOf[QueryNotDeletedResponse]))
        )
      )
    )
  )
  def deleteQueryRoute: Route =
    pathPrefixLabeled(Segment, ":id") { name =>
      delete {
        deleteQuery(name) match {
          case response @ QueryDeletedResponse(_, _) =>
            complete(HttpResponse(entity = HttpEntity(response.toJson.prettyPrint)))
          case response @ QueryNotDeletedResponse(_) =>
            complete(
              HttpResponse(
                status = StatusCodes.BadRequest,
                entity = HttpEntity(response.toJson.prettyPrint)
              )
            )
        }
      }
    }

  def runQueryForInput(
      name: String,
      inputTopic: String,
      input: String,
      passOutputToSink: Boolean
  ): Future[RunQueryForInputResponse] = {
    implicit val ec: ExecutionContextExecutor = system.executionContext
    QueryService.runQueryForInput(name, inputTopic, input, passOutputToSink, activeQueriesState) match {
      case Success(futureResult) =>
        futureResult
          .map { result =>
            RunQueryForInputSuccessResponse(result)
          }
          .recover { exception =>
            RunQueryForInputFailureResponse(exception.getMessage)
          }
      case Failure(exception) =>
        Future(RunQueryForInputFailureResponse(s"Query did not run: $exception"))
    }
  }

  @POST
  @Path("/{name}/input")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(
    summary = "Run the query with the given name for the given input",
    operationId = "runQueryForInput",
    parameters = Array(
      new Parameter(name = "name", in = ParameterIn.PATH, description = "Query name"),
      new Parameter(name = "topic", in = ParameterIn.QUERY, description = "Input topic"),
    ),
    requestBody = new RequestBody(
      required = true,
      content = Array(new Content(schema = new Schema(implementation = classOf[Any])))
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Query run for input",
        content = Array(
          new Content(
            schema = new Schema(implementation = classOf[RunQueryForInputSuccessResponse])
          )
        )
      ),
      new ApiResponse(
        responseCode = "500",
        description = "Internal server error",
        content = Array(
          new Content(
            schema = new Schema(implementation = classOf[RunQueryForInputFailureResponse])
          )
        )
      )
    )
  )
  def runQueryForInputRoute: Route =
    pathPrefixLabeled(Segment / "input", ":id/input") { name =>
      post {
        parameter("topic".optional) { topic =>
          entity(as[String]) { input =>
            onSuccess(runQueryForInput(name, topic.getOrElse("http"), input, passOutputToSink = false)) {
              case RunQueryForInputSuccessResponse(output) =>
                complete(
                  HttpResponse(
                    status = StatusCodes.OK,
                    entity = HttpEntity(output)
                  )
                )
              case response@RunQueryForInputFailureResponse(_) =>
                complete(
                  HttpResponse(
                    status = StatusCodes.InternalServerError,
                    entity = HttpEntity(response.toJson.prettyPrint)
                  )
                )
            }
          }
        }
      }
    }

  val routes: Route = pathPrefixLabeled("queries") {
    concat(
      getQueryRoute,
      getAllQueriesRoute,
      createQueryRoute,
      updateQueryRoute,
      deleteQueryRoute,
      runQueryForInputRoute
    )
  }
}

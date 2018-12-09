package com.api.service;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.jwt.JWTOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.api.OperationRequest;
import io.vertx.ext.web.api.OperationResponse;

public class TransactionServiceImpl implements TransactionService {
	private Vertx vertx;
	private MongoClient mongoClient;
	private JWTAuth authProvider;
	public TransactionServiceImpl(Vertx vertx, MongoClient mongoClient) {
		this.vertx = vertx;
		this.mongoClient = mongoClient;
		
	}
	
	public TransactionServiceImpl(Vertx vertx, MongoClient mongoClient,JWTAuth provider) {
		this.vertx = vertx;
		this.mongoClient = mongoClient;
		this.authProvider=provider;
	}
	
	
	@Override
	public void login(OperationRequest context, Handler<AsyncResult<OperationResponse>> resultHandler) {
		JsonObject payload = context.getParams().getJsonObject("body");
		String username = payload.getString("username");
		String password = payload.getString("password");
		JsonObject response = new JsonObject();
		if(username.equalsIgnoreCase(password)) {
			String token = authProvider.generateToken(new JsonObject().put("userName", username),new JWTOptions().setAlgorithm("ES256"));
			response.put("token", token);
			response.put("code", 200);
		}else {
			response.put("message", "Invalid user/password");
		}
		resultHandler.handle(Future.succeededFuture(OperationResponse.completedWithJson(response)));
		
	}

	@Override
	public void getUserDetailById(OperationRequest context, Handler<AsyncResult<OperationResponse>> resultHandler) {
		String userId = context.getParams().getJsonObject("path").getString("userId");
		JsonObject response = new JsonObject();
		JsonObject query = new JsonObject();
		query.put("_id", userId);
		mongoClient.findOne("books", new JsonObject().put("_id", userId), null, res -> {
			if (res.succeeded()) {
				if (res.result() != null) {
					resultHandler.handle(Future.succeededFuture(OperationResponse.completedWithJson(res.result())));
				} else {
					response.put("message", "we are unable to process yoor request,please try again");
					resultHandler.handle(Future.succeededFuture(OperationResponse.completedWithJson(response)));
				}

			} else {
				response.put("message", "we are unable to process yoor request,please try again");
				response.put("error", res.cause());
				resultHandler.handle(Future.succeededFuture(OperationResponse.completedWithJson(response)));
			}
		});

	}

	@Override
	public void getAllUsers(OperationRequest request, Handler<AsyncResult<OperationResponse>> resultHandler) {
		
		JsonObject response = new JsonObject();
		JsonObject query = new JsonObject();
		mongoClient.find("books", query, res -> {
			if (res.succeeded()) {
				response.put("users", res.result());
				resultHandler.handle(Future.succeededFuture(OperationResponse.completedWithJson(response)));
			} else {
				response.put("message", "we are unable to process yoor request,please try again");
				response.put("error", res.cause());
				resultHandler.handle(Future.succeededFuture(OperationResponse.completedWithJson(response)));
			}
		});

	}

	@Override
	public void addUser(OperationRequest request, Handler<AsyncResult<OperationResponse>> resultHandler) {
		JsonObject response = new JsonObject();
		JsonObject payload = request.getParams().getJsonObject("body");
		System.out.println(payload.toString());
		payload.remove("id");
		mongoClient.save("books", payload, res -> {
			if (res.succeeded()) {
				String id = res.result();
				response.put("id", id);
				resultHandler.handle(Future.succeededFuture(OperationResponse.completedWithJson(response)));
			} else {
				response.put("message", "we are unable to process yoor request,please try again");
				response.put("error", res.cause());
				resultHandler.handle(Future.succeededFuture(OperationResponse.completedWithJson(response)));

			}
		});

	}

	@Override
	public void deleteById(OperationRequest context, Handler<AsyncResult<OperationResponse>> resultHandler) {
		String userId = context.getParams().getJsonObject("path").getString("userId");
		JsonObject response = new JsonObject();
		JsonObject query = new JsonObject();
		query.put("_id", userId);
		mongoClient.findOneAndDelete("books", new JsonObject().put("_id", userId), res -> {
			if (res.succeeded()) {
				if (res.result() != null) {
					resultHandler.handle(Future.succeededFuture(OperationResponse.completedWithJson(res.result())));

				} else {
					resultHandler.handle(Future.succeededFuture(OperationResponse.completedWithJson(response)));
				}

			} else {
				response.put("error", res.cause());
				resultHandler.handle(Future.succeededFuture(OperationResponse.completedWithJson(response)));
			}
		});

	}

	@Override
	public void updateUser(OperationRequest requestOp, Handler<AsyncResult<OperationResponse>> resultHandler) {
		String userId = requestOp.getParams().getJsonObject("path").getString("userId");
		JsonObject response = new JsonObject();
		JsonObject query = new JsonObject();
		JsonObject request = requestOp.getParams().getJsonObject("body");
		query.put("_id", userId);
		JsonObject update = new JsonObject().put("$set", request);
		System.out.println(request.toString());
		mongoClient.updateCollection("books", new JsonObject().put("_id", userId), update, res -> {
			if (res.succeeded()) {
				if (res.result() != null) {
					resultHandler.handle(Future.succeededFuture(OperationResponse.completedWithJson(response)));

				} else {
					resultHandler.handle(Future.succeededFuture(OperationResponse.completedWithJson(response)));
				}

			} else {
				res.cause().printStackTrace();
				resultHandler.handle(Future.succeededFuture(OperationResponse.completedWithJson(response)));
			}
		});

	}

}

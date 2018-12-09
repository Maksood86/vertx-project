package com.api.service;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.ext.web.api.OperationRequest;
import io.vertx.ext.web.api.OperationResponse;
import io.vertx.ext.web.api.generator.WebApiServiceGen;

@WebApiServiceGen
public interface TransactionService {
	void addUser(OperationRequest context, Handler<AsyncResult<OperationResponse>> resultHandler);

	void getAllUsers(OperationRequest context, Handler<AsyncResult<OperationResponse>> resultHandler);

	void getUserDetailById(OperationRequest context, Handler<AsyncResult<OperationResponse>> resultHandler);

	void deleteById(OperationRequest context, Handler<AsyncResult<OperationResponse>> resultHandler);

	void updateUser(OperationRequest context, Handler<AsyncResult<OperationResponse>> resultHandler);
	
	void login(OperationRequest context, Handler<AsyncResult<OperationResponse>> resultHandler);
}
package com.api.demo;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.api.service.TransactionService;
import com.api.service.TransactionServiceImpl;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.streams.Pump;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.serviceproxy.ServiceBinder;

public class OpenAPIServer extends AbstractVerticle {
	private static final Logger logger = LoggerFactory.getLogger(OpenAPIServer.class);
	
	private String filePath;
	
	public static void main(String[] args) {
		Vertx vertx = Vertx.vertx();
		vertx.deployVerticle(new OpenAPIServer());
	}

	private MongoClient client;

	@Override
	public void start(final Future<Void> bootstrapFuture) throws Exception {
		this.filePath = "/Users/maksoodalam/Downloads/qrcode.png";
		OpenAPI3RouterFactory.create(vertx, "webroot/openapi.yaml", ar -> {
			if (ar.succeeded()) {
				JsonObject config = new JsonObject();
				config.put("host", "127.0.0.1");
				config.put("port", 27017);
				client = MongoClient.createShared(vertx, config);

				

				JWTAuth authProvider = JWTAuth.create(vertx, new JWTAuthOptions().addPubSecKey(new PubSecKeyOptions()
						.setAlgorithm("ES256")
						.setSecretKey(
								"MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQg/56PGdmRtQCykZdYWBOUfsRlXdwH53GZJMaljMYke3ShRANCAAQ9vS5GXjQZcLQ+B8EHgC2hJO+Zmaucv1E7D/p1MVwiX2qAZVyCx1ub/PWhjlArpDn0FIwRnQRbUriaL9+KASNV")
						.setPublicKey(
								"MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEPb0uRl40GXC0PgfBB4AtoSTvmZmrnL9ROw/6dTFcIl9qgGVcgsdbm/z1oY5QK6Q59BSMEZ0EW1K4mi/figEjVQ==")));
			
				TransactionService service = new TransactionServiceImpl(vertx, client,authProvider);
				final ServiceBinder serviceBinder = new ServiceBinder(vertx).setAddress("user.service");
				MessageConsumer<JsonObject> serviceConsumer = serviceBinder.register(TransactionService.class, service);
				serviceConsumer.completionHandler(h -> {
					System.out.println("Register service class");
				});
				
				OpenAPI3RouterFactory factory = ar.result();

				factory.addSecurityHandler("ApiKey", JWTAuthHandler.create(authProvider));
				factory.mountServicesFromExtensions();

				factory.addGlobalHandler(h -> {
					h.response().putHeader("content-type", "application/json")
							.putHeader("Access-Control-Allow-Origin", "*").putHeader("Access-Control-Max-Age", "3600")
							.putHeader("Access-Control-Allow-Credentials", "true")
							.putHeader("Access-Control-Allow-Headers",
									"Authorization,User-Agent,Connection,Host,Accept-Language,Accept-Encoding,Access-Control-Allow-Headers, Origin,Accept, X-Requested-With, Content-Type, Access-Control-Request-Method,OPTIONS")
							.putHeader("Access-Control-Allow-Methods", "GET, POST,PUT, DELETE, OPTIONS");
					h.next();
				});

				Router router = factory.getRouter();

				router.route().handler(CorsHandler.create("http://localhost:4200")
						.allowedMethod(io.vertx.core.http.HttpMethod.GET).allowedMethod(HttpMethod.POST)
						.allowedMethod(HttpMethod.DELETE).allowedMethod(HttpMethod.PUT)
						.allowedMethod(HttpMethod.OPTIONS).allowedHeader("Access-Control-Request-Method")
						.allowedHeader("Access-Control-Allow-Credentials").allowedHeader("Access-Control-Allow-Origin")
						.allowedHeader("Access-Control-Allow-Headers")//
						.allowedHeader("Access-Control-Request-Headers").allowedHeader("Content-Type")
						.allowedHeader("cross-origin").allowedHeader("Authorization").allowedHeader("X-PINGARUNER")
						.allowedHeader("application/json"));

				/*router.post("/login").handler(ctx -> {
					JsonObject jsonObject = new JsonObject();
					JsonObject request=ctx.getBodyAsJson();
					String username = request.getString("username");
					String password = request.getString("password");
					if(username.equalsIgnoreCase(password)) {
						String token = authProvider.generateToken(new JsonObject().put("userName", username),new JWTOptions().setAlgorithm("ES256"));
						jsonObject.put("token", token);
						jsonObject.put("code", 200);
					}else {
						jsonObject.put("message", "Invalid user/password");
					}
					
					ctx.response().end(jsonObject.toString());

				});*/

				router.get("/send").handler(ctx -> {

					this.client.findOne("SESSION_DETAIL", new JsonObject().put("_id", ctx.request().getParam("id")),
							null, res -> {
								if (res.succeeded()) {
									System.out.println(res.result().toString());
									if (res.result() != null) {
										JsonObject result = res.result();
										vertx.eventBus().send(result.getString("sessionId"), "Some Message");
									}

								}
							});
					JsonObject jsonObject = new JsonObject();
					jsonObject.put("Status", "OK");
					ctx.response().end(jsonObject.toString());

				});
				
				router.get("/download").handler(ctx -> {
					
					download(ctx);
					
					
					
				});
				
				
				router.route().handler(StaticHandler.create());
				HttpServer server = vertx.createHttpServer(new HttpServerOptions().setPort(8082).setHost("localhost"));
				server.websocketHandler(web -> {
					subscribeSession(web.path(), web.textHandlerID());
					logger.info("Test====> connetcted " + web.textHandlerID());
					String textId = web.textHandlerID();
					vertx.eventBus().consumer(textId, h -> {
						web.writeTextMessage((String) h.body());
					});
					web.textMessageHandler(bb -> {
						logger.info("message Recieved" + bb);
					});

					web.closeHandler(bb -> {
						unsubscribe(web.path(), web.textHandlerID());
					});
				});
				server.requestHandler(router).listen();
				
				vertx.setPeriodic(1000*60, id -> {
					 
					  System.out.println("timer fired!");
					  
					  this.client.findOne("SESSION_DETAIL", new JsonObject().put("_id", "123456789"),
								null, res -> {
									if (res.succeeded()) {
										System.out.println(res.result().toString());
										if (res.result() != null) {
											JsonObject result = res.result();
											vertx.eventBus().send(result.getString("sessionId"), "Some Message");
										}

									}
								});
					  
					  
					});
				
				
				bootstrapFuture.complete();
			} else {
				bootstrapFuture.fail(ar.cause());
			}
		});
	}

	private void unsubscribe(String userName, String id) {
		logger.info("====closed===" + userName);
		this.client.findOneAndDelete("SESSION_DETAIL", new JsonObject().put("_id", userName.replace("/", "")), res -> {
			if (res.succeeded()) {
				System.out.println(res.result());
			}
		});
	}

	private void subscribeSession(String userName, String id) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
		String currentDate = dateFormat.format(new Date());
		logger.info("Subscribe", userName);
		JsonObject payload = new JsonObject();
		payload.put("sessionId", id);
		payload.put("createdDate", new JsonObject().put("$date", currentDate));
		payload.put("_id", userName.replace("/", ""));
		this.client.save("SESSION_DETAIL", payload, res -> {
			System.out.println(res.result());
		});
	}
	
	private void download(RoutingContext routingContext) {
        vertx.fileSystem().open(this.filePath, new OpenOptions(), readEvent -> {

            if (readEvent.failed()) {
                routingContext.response().setStatusCode(500).end();
                return;
            }

            AsyncFile asyncFile = readEvent.result();

            routingContext.response().setChunked(true);
            routingContext.response().putHeader("Content-Type", "image/png");

            Pump pump = Pump.pump(asyncFile, routingContext.response());

            pump.start();

            asyncFile.endHandler(aVoid -> {
                asyncFile.close();
                routingContext.response().end();
            });
        });
    }

}

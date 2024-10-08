/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.karavan.api;

import io.smallrye.mutiny.Multi;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import org.apache.camel.karavan.service.NotificationService;
import org.jboss.resteasy.reactive.RestStreamElementType;

import static org.apache.camel.karavan.listener.NotificationListener.*;

@Path("/ui/notification")
public class NotificationResource {

    private static final String SERVICE_NAME_SYSTEM = "NOTIFICATION_SYSTEM";
    private static final String SERVICE_NAME_USER = "NOTIFICATION_USER";

    @Inject
    EventBus eventBus;

    @Inject
    NotificationService notificationService;

    @GET
    @Path("/system/{username}")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.TEXT_PLAIN)
    public Multi<OutboundSseEvent> karavanStream(
            @PathParam("username") String username,
            @Context SseEventSink eventSink,
            @Context Sse sse
    ) {
        notificationService.sinkCleanup(SERVICE_NAME_SYSTEM, username, eventSink);
        return eventBus.<JsonObject>consumer(NOTIFICATION_ADDRESS_SYSTEM).toMulti()
                .map(m -> sse.newEventBuilder()
                        .id(m.headers().get(NOTIFICATION_HEADER_EVENT_ID))
                        .name(m.headers().get(NOTIFICATION_HEADER_EVENT_NAME) + ":" + m.headers().get(NOTIFICATION_HEADER_CLASS_NAME))
                        .data(m.body())
                        .build());
    }

    @GET
    @Path("/user/{username}")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.TEXT_PLAIN)
    public Multi<OutboundSseEvent> userStream(
            @PathParam("username") String username,
            @Context SseEventSink eventSink,
            @Context Sse sse
    ) {
        notificationService.sinkCleanup(SERVICE_NAME_USER, username, eventSink);
        return eventBus.<JsonObject>consumer(username).toMulti()
                .map(m -> sse.newEventBuilder()
                        .id(m.headers().get(NOTIFICATION_HEADER_EVENT_ID))
                        .name(m.headers().get(NOTIFICATION_HEADER_EVENT_NAME) + ":" + m.headers().get(NOTIFICATION_HEADER_CLASS_NAME))
                        .data(m.body())
                        .build());
    }
}
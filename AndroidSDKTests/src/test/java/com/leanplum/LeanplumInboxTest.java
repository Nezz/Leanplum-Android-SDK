/*
 * Copyright 2016, Leanplum, Inc. All rights reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.leanplum;

import com.leanplum.__setup.AbstractTest;
import com.leanplum._whitebox.utilities.RequestHelper;
import com.leanplum._whitebox.utilities.ResponseHelper;
import com.leanplum.callbacks.NewsfeedChangedCallback;
import com.leanplum.internal.Constants;

import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests the inbox.
 *
 * @author Milos Jakovljevic
 */
public class LeanplumInboxTest extends AbstractTest {
    @Test
    public void testNewsfeed() throws Exception {
        setupSDK(mContext, "/responses/simple_start_response.json");

        // Seed newsfeed response which contains messages.
        ResponseHelper.seedResponse("/responses/newsfeed_response.json");

        // Validate downloadMessages() request.
        RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
            @Override
            public void onRequest(String httpMethod, String apiMethod, Map<String, Object> params) {
                assertEquals(Constants.Methods.GET_INBOX_MESSAGES, apiMethod);
            }
        });
        // Download messages.
        Leanplum.getInbox().downloadMessages();

        // Validate newsfeed callback when messages changes.
        NewsfeedChangedCallback callback = new NewsfeedChangedCallback() {
            @Override
            public void newsfeedChanged() {
                assertEquals(2, Leanplum.getInbox().unreadCount());
                assertEquals(2, Leanplum.getInbox().count());

                List<NewsfeedMessage> messageList = Leanplum.getInbox().unreadMessages();

                NewsfeedMessage message1 = messageList.get(0);
                NewsfeedMessage message2 = messageList.get(1);

                assertEquals("5231495977893888##1", message1.getMessageId());
                assertEquals("This is a test inbox message", message1.getTitle());
                assertEquals("This is a subtitle", message1.getSubtitle());
                assertNull(message1.getExpirationTimestamp());
                assertFalse(message1.isRead());

                assertEquals("4682943996362752##2", message2.getMessageId());
                assertEquals("This is a second test message", message2.getTitle());
                assertEquals("This is a second test message subtitle", message2.getSubtitle());
                assertNull(message2.getExpirationTimestamp());
                assertFalse(message2.isRead());
            }
        };

        // Add callback to be able to validate.
        Leanplum.getInbox().addChangedHandler(callback);

        // Remove it afterwards so we don't get callbacks anymore.
        Leanplum.getInbox().removeChangedHandler(callback);

        // Validate message state.
        List<NewsfeedMessage> messageList = Leanplum.getInbox().unreadMessages();

        NewsfeedMessage message1 = messageList.get(0);
        NewsfeedMessage message2 = messageList.get(1);

        message1.read();
        assertTrue(message1.isRead());
        assertEquals(1, Leanplum.getInbox().unreadCount());

        message2.read();
        assertTrue(message2.isRead());
        assertEquals(0, Leanplum.getInbox().unreadCount());

        assertEquals(2, Leanplum.getInbox().count());

        NewsfeedMessage messageById = Leanplum.getInbox().messageForId(message1.getMessageId());
        assertNotNull(messageById);
        assertEquals(message1, messageById);

        Leanplum.getInbox().removeMessage(messageById.getMessageId());
        assertEquals(1, Leanplum.getInbox().allMessages().size());
    }

    @Test
    public void testDisablePrefetching() {
        LeanplumInbox.disableImagePrefetching();
        assertFalse(LeanplumInbox.getInstance().isInboxImagePrefetchingEnabled());
    }

    @Test
    public void testMessageCreate() {
        Date delivery = new Date(100);
        Date expiration = new Date(200);
        HashMap<String, Object> map = new HashMap<>();
        map.put(Constants.Keys.MESSAGE_DATA, new HashMap<String, Object>());
        map.put(Constants.Keys.DELIVERY_TIMESTAMP, delivery.getTime());
        map.put(Constants.Keys.EXPIRATION_TIMESTAMP, expiration.getTime());
        map.put(Constants.Keys.IS_READ, true);

        LeanplumInboxMessage message = LeanplumInboxMessage.createFromJsonMap("message##Id", map);
        assertEquals("message##Id", message.getMessageId());
        assertEquals(delivery, message.getDeliveryTimestamp());
        assertEquals(expiration, message.getExpirationTimestamp());
        assertTrue(message.isRead());
        assertNull(message.getData());

        assertNull(message.getImageFilePath());
        assertNull(message.getImageUrl());

        LeanplumInboxMessage invalidMessage = LeanplumInboxMessage.createFromJsonMap("messageId", map);
        assertNull(invalidMessage);
    }
}

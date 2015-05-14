/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.activemq.artemis.core.protocol.mqtt.netty.extensions;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import org.apache.activemq.artemis.api.core.ActiveMQBuffer;

public class ActiveMQByteBufAllocator implements ByteBufAllocator
{
   private ByteBuf buf;

   public ActiveMQByteBufAllocator(ActiveMQBuffer buffer)
   {
      buf = buffer.byteBuf();
   }

   @Override
   public ByteBuf buffer()
   {
      return buf;
   }

   @Override
   public ByteBuf buffer(int initialCapacity)
   {
      return buf;
   }

   @Override
   public ByteBuf buffer(int initialCapacity, int maxCapacity)
   {
      return buf;
   }

   @Override
   public ByteBuf ioBuffer()
   {
      return buf;
   }

   @Override
   public ByteBuf ioBuffer(int initialCapacity)
   {
      return buf;
   }

   @Override
   public ByteBuf ioBuffer(int initialCapacity, int maxCapacity)
   {
      return buf;
   }

   @Override
   public ByteBuf heapBuffer()
   {
      return buf;
   }

   @Override
   public ByteBuf heapBuffer(int initialCapacity)
   {
      return buf;
   }

   @Override
   public ByteBuf heapBuffer(int initialCapacity, int maxCapacity)
   {
      return buf;
   }

   @Override
   public ByteBuf directBuffer()
   {
      return buf;
   }

   @Override
   public ByteBuf directBuffer(int initialCapacity)
   {
      return buf;
   }

   @Override
   public ByteBuf directBuffer(int initialCapacity, int maxCapacity)
   {
      return buf;
   }

   @Override
   public CompositeByteBuf compositeBuffer()
   {
      return null;
   }

   @Override
   public CompositeByteBuf compositeBuffer(int maxNumComponents)
   {
      return null;
   }

   @Override
   public CompositeByteBuf compositeHeapBuffer()
   {
      return null;
   }

   @Override
   public CompositeByteBuf compositeHeapBuffer(int maxNumComponents)
   {
      return null;
   }

   @Override
   public CompositeByteBuf compositeDirectBuffer()
   {
      return null;
   }

   @Override
   public CompositeByteBuf compositeDirectBuffer(int maxNumComponents)
   {
      return null;
   }

   @Override
   public boolean isDirectBufferPooled()
   {
      return false;
   }
}

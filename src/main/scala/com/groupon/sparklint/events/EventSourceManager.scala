/*
 Copyright 2016 Groupon, Inc.
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at
 http://www.apache.org/licenses/LICENSE-2.0
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/
package com.groupon.sparklint.events

import scala.collection.mutable

/**
  * The production implementation of EventSourceManagerLike. Manages and abstracts EventSourceLike instances
  * server side.
  *
  * @author swhitear 
  * @since 8/18/16.
  */
class EventSourceManager(initialSources: EventSourceDetail*) extends EventSourceManagerLike {

  // this sync'ed LinkedHashMap is necessary because we want to ensure ordering of items in the manager, not the UI.
  // insertion order works well enough here, we have no need for any other guarantees from the data structure.
  private val eventSourcesByAppId = new mutable.LinkedHashMap[String, EventSourceDetail]()
                                        with mutable.SynchronizedMap[String, EventSourceDetail]
  initialSources.foreach(es => eventSourcesByAppId += (es.source.appId -> es))

  override def addEventSource(eventDetail: EventSourceDetail): Unit = {
    eventSourcesByAppId.put(eventDetail.source.appId, eventDetail)
  }

  override def sourceCount: Int = eventSourcesByAppId.size

  override def eventSource: Iterable[EventSourceDetail] = eventSourcesByAppId.values

  override def containsAppId(appId: String): Boolean = eventSourcesByAppId.contains(appId)

  @throws[NoSuchElementException]
  override def getSource(appId: String): EventSourceLike = eventSourcesByAppId(appId).source

  @throws[NoSuchElementException]
  override def getScrollingSource(appId: String): FreeScrollEventSource = {
    eventSourcesByAppId.get(appId) match {
      case Some(EventSourceDetail(eventSource: FreeScrollEventSource, p: EventSourceProgressLike, s: EventStateLike))
                   => eventSource
      case Some(_) => throw new IllegalArgumentException(s"$appId cannot free scroll")
      case None    => throw new NoSuchElementException(s"Missing appId $appId")
    }
  }

    @throws[NoSuchElementException]
    override def getSourceDetail(appId: String): EventSourceDetail = eventSourcesByAppId(appId)
  }

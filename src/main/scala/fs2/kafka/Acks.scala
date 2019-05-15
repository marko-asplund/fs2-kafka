/*
 * Copyright 2018-2019 OVO Energy Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fs2.kafka

/**
  * The available options for [[ProducerSettings#withAcks]].<br>
  * <br>
  * Available options include:<br>
  * - [[Acks#Zero]] to not wait for any acknowledgement from the server,<br>
  * - [[Acks#One]] to only wait for acknowledgement from the leader node,<br>
  * - [[Acks#All]] to wait for acknowledgement from all in-sync replicas.
  */
sealed abstract class Acks

object Acks {
  private[kafka] case object ZeroAcks extends Acks {
    override def toString: String = "Zero"
  }

  private[kafka] case object OneAcks extends Acks {
    override def toString: String = "One"
  }

  private[kafka] case object AllAcks extends Acks {
    override def toString: String = "All"
  }

  /**
    * Option to not wait for any acknowledgement from the server
    * when producing records.
    */
  val Zero: Acks = ZeroAcks

  /**
    * Option to only wait for acknowledgement from the leader node
    * when producing records.
    */
  val One: Acks = OneAcks

  /**
    * Option to wait for acknowledgement from all in-sync replicas
    * when producing records.
    */
  val All: Acks = AllAcks
}

/*
 * Copyright 2023-2023 etspaceman
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

package kinesis4cats.kcl.ciris

import java.util.Collection

import software.amazon.awssdk.services.dynamodb.model.BillingMode
import software.amazon.awssdk.services.dynamodb.model.Tag

private[kinesis4cats] final class WorkerMetricsTableConfig(
    appName: String,
    override val billingMode: BillingMode,
    override val readCapacity: Long,
    override val writeCapacity: Long,
    override val pointInTimeRecoveryEnabled: Boolean,
    override val deletionProtectionEnabled: Boolean,
    override val tags: Collection[Tag]
) extends software.amazon.kinesis.leases.LeaseManagementConfig.WorkerMetricsTableConfig(
      appName
    ) { self =>
  val underlying
      : software.amazon.kinesis.leases.LeaseManagementConfig.WorkerMetricsTableConfig =
    self

  def copy(
      appName: String = self.appName,
      billingMode: BillingMode = self.billingMode,
      readCapacity: Long = self.readCapacity,
      writeCapacity: Long = self.writeCapacity,
      pointInTimeRecoveryEnabled: Boolean =
        self.pointInTimeRecoveryEnabled,
      deletionProtectionEnabled: Boolean =
        self.deletionProtectionEnabled,
      tags: Collection[Tag] = self.tags
  ): WorkerMetricsTableConfig = new WorkerMetricsTableConfig(
    appName,
    billingMode,
    readCapacity,
    writeCapacity,
    pointInTimeRecoveryEnabled,
    deletionProtectionEnabled,
    tags
  )
}

object WorkerMetricsTableConfig {
  def default(appName: String): WorkerMetricsTableConfig = {
    val underlying =
      new software.amazon.kinesis.leases.LeaseManagementConfig.WorkerMetricsTableConfig(
        appName
      )

    new WorkerMetricsTableConfig(
      appName,
      underlying.billingMode(),
      underlying.readCapacity(),
      underlying.writeCapacity(),
      underlying.pointInTimeRecoveryEnabled(),
      underlying.deletionProtectionEnabled(),
      underlying.tags()
    )
  }
}

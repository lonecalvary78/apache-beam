/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.beam.sdk.io.iceberg;

import static org.apache.beam.sdk.util.Preconditions.checkStateNotNull;

import com.google.auto.value.AutoValue;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.beam.sdk.schemas.AutoValueSchema;
import org.apache.beam.sdk.schemas.NoSuchSchemaException;
import org.apache.beam.sdk.schemas.Schema;
import org.apache.beam.sdk.schemas.SchemaCoder;
import org.apache.beam.sdk.schemas.SchemaRegistry;
import org.apache.beam.sdk.schemas.annotations.DefaultSchema;
import org.apache.beam.sdk.schemas.annotations.SchemaIgnore;
import org.apache.beam.sdk.values.Row;
import org.apache.beam.vendor.guava.v32_1_2_jre.com.google.common.annotations.VisibleForTesting;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.catalog.TableIdentifier;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

/**
 * This is an AutoValue representation of an Iceberg {@link Snapshot}.
 *
 * <p>Note: this only includes the subset of fields in {@link Snapshot} that are Beam
 * Schema-compatible.
 */
@DefaultSchema(AutoValueSchema.class)
@AutoValue
public abstract class SnapshotInfo {
  public static SnapshotInfo fromSnapshot(Snapshot snapshot) {
    return fromSnapshot(snapshot, null);
  }

  public static SnapshotInfo fromSnapshot(Snapshot snapshot, @Nullable String tableIdentifier) {
    return SnapshotInfo.builder()
        .setTableIdentifierString(tableIdentifier)
        .setSequenceNumber(snapshot.sequenceNumber())
        .setSnapshotId(snapshot.snapshotId())
        .setParentId(snapshot.parentId())
        .setTimestampMillis(snapshot.timestampMillis())
        .setOperation(snapshot.operation())
        .setSummary(snapshot.summary())
        .setManifestListLocation(snapshot.manifestListLocation())
        .setSchemaId(snapshot.schemaId())
        .build();
  }

  public Row toRow() {
    try {
      return SchemaRegistry.createDefault()
          .getToRowFunction(SnapshotInfo.class)
          .apply(this)
          .sorted()
          .toSnakeCase();
    } catch (NoSuchSchemaException e) {
      throw new RuntimeException(e);
    }
  }

  private static @MonotonicNonNull SchemaCoder<SnapshotInfo> coder;
  private static @MonotonicNonNull Schema schema;

  static SchemaCoder<SnapshotInfo> getCoder() {
    if (coder == null) {
      initSchemaAndCoder();
    }
    return checkStateNotNull(coder);
  }

  private transient @MonotonicNonNull TableIdentifier cachedTableIdentifier;

  public static Builder builder() {
    return new AutoValue_SnapshotInfo.Builder();
  }

  @SchemaIgnore
  public TableIdentifier getTableIdentifier() {
    if (cachedTableIdentifier == null) {
      cachedTableIdentifier = TableIdentifier.parse(checkStateNotNull(getTableIdentifierString()));
    }
    return cachedTableIdentifier;
  }

  public abstract long getSequenceNumber();

  public abstract long getSnapshotId();

  public abstract @Nullable Long getParentId();

  public abstract long getTimestampMillis();

  public abstract @Nullable String getOperation();

  public abstract @Nullable Map<String, String> getSummary();

  public abstract @Nullable String getManifestListLocation();

  public abstract @Nullable Integer getSchemaId();

  public abstract @Nullable String getTableIdentifierString();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setSequenceNumber(long sequenceNumber);

    public abstract Builder setSnapshotId(long snapshotId);

    public abstract Builder setParentId(Long parentId);

    public abstract Builder setTimestampMillis(long timestampMillis);

    public abstract Builder setOperation(String operation);

    public abstract Builder setSummary(Map<String, String> summary);

    public abstract Builder setManifestListLocation(String manifestListLocation);

    public abstract Builder setSchemaId(Integer schemaId);

    abstract Builder setTableIdentifierString(@Nullable String table);

    public abstract SnapshotInfo build();
  }

  @VisibleForTesting
  static Schema getSchema() {
    if (schema == null) {
      initSchemaAndCoder();
    }
    return checkStateNotNull(schema);
  }

  private static void initSchemaAndCoder() {
    try {
      SchemaRegistry registry = SchemaRegistry.createDefault();
      coder = registry.getSchemaCoder(SnapshotInfo.class);
      schema = registry.getSchema(SnapshotInfo.class).sorted().toSnakeCase();
    } catch (NoSuchSchemaException e) {
      throw new RuntimeException(e);
    }
  }
}

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
package org.apache.beam.sdk.io.gcp.bigquery;

import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.core.ApiFuture;
import com.google.api.services.bigquery.model.Dataset;
import com.google.api.services.bigquery.model.Job;
import com.google.api.services.bigquery.model.JobConfigurationExtract;
import com.google.api.services.bigquery.model.JobConfigurationLoad;
import com.google.api.services.bigquery.model.JobConfigurationQuery;
import com.google.api.services.bigquery.model.JobConfigurationTableCopy;
import com.google.api.services.bigquery.model.JobReference;
import com.google.api.services.bigquery.model.JobStatistics;
import com.google.api.services.bigquery.model.Table;
import com.google.api.services.bigquery.model.TableReference;
import com.google.api.services.bigquery.model.TableRow;
import com.google.cloud.bigquery.storage.v1.AppendRowsRequest;
import com.google.cloud.bigquery.storage.v1.AppendRowsResponse;
import com.google.cloud.bigquery.storage.v1.BatchCommitWriteStreamsResponse;
import com.google.cloud.bigquery.storage.v1.CreateReadSessionRequest;
import com.google.cloud.bigquery.storage.v1.FinalizeWriteStreamResponse;
import com.google.cloud.bigquery.storage.v1.FlushRowsResponse;
import com.google.cloud.bigquery.storage.v1.ProtoRows;
import com.google.cloud.bigquery.storage.v1.ReadRowsRequest;
import com.google.cloud.bigquery.storage.v1.ReadRowsResponse;
import com.google.cloud.bigquery.storage.v1.ReadSession;
import com.google.cloud.bigquery.storage.v1.SplitReadStreamRequest;
import com.google.cloud.bigquery.storage.v1.SplitReadStreamResponse;
import com.google.cloud.bigquery.storage.v1.TableSchema;
import com.google.cloud.bigquery.storage.v1.WriteStream;
import com.google.protobuf.DescriptorProtos;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import org.apache.beam.sdk.annotations.Internal;
import org.apache.beam.sdk.values.FailsafeValueInSingleWindow;
import org.apache.beam.sdk.values.ValueInSingleWindow;
import org.checkerframework.checker.nullness.qual.Nullable;

/** An interface for real, mock, or fake implementations of Cloud BigQuery services. */
@Internal
public interface BigQueryServices extends Serializable {

  /** Returns a real, mock, or fake {@link JobService}. */
  JobService getJobService(BigQueryOptions bqOptions);

  /** Returns a real, mock, or fake {@link DatasetService}. */
  DatasetService getDatasetService(BigQueryOptions bqOptions);

  /** Returns a real, mock, or fake {@link WriteStreamService}. */
  WriteStreamService getWriteStreamService(BigQueryOptions bqOptions);

  /** Returns a real, mock, or fake {@link StorageClient}. */
  StorageClient getStorageClient(BigQueryOptions bqOptions) throws IOException;

  /** An interface for the Cloud BigQuery load service. */
  public interface JobService extends AutoCloseable {
    /** Start a BigQuery load job. */
    void startLoadJob(JobReference jobRef, JobConfigurationLoad loadConfig)
        throws InterruptedException, IOException;

    /** Start a BigQuery load job with stream content. */
    void startLoadJob(
        JobReference jobRef,
        JobConfigurationLoad loadConfig,
        AbstractInputStreamContent streamContent)
        throws InterruptedException, IOException;

    /** Start a BigQuery extract job. */
    void startExtractJob(JobReference jobRef, JobConfigurationExtract extractConfig)
        throws InterruptedException, IOException;

    /** Start a BigQuery query job. */
    void startQueryJob(JobReference jobRef, JobConfigurationQuery query)
        throws IOException, InterruptedException;

    /** Start a BigQuery copy job. */
    void startCopyJob(JobReference jobRef, JobConfigurationTableCopy copyConfig)
        throws IOException, InterruptedException;

    /**
     * Waits for the job is Done, and returns the job.
     *
     * <p>Returns null if the {@code maxAttempts} retries reached.
     */
    Job pollJob(JobReference jobRef, int maxAttempts) throws InterruptedException;

    /** Dry runs the query in the given project. */
    JobStatistics dryRunQuery(
        String projectId, JobConfigurationQuery queryConfig, @Nullable String location)
        throws InterruptedException, IOException;

    /**
     * Gets the specified {@link Job} by the given {@link JobReference}.
     *
     * <p>Returns null if the job is not found.
     */
    Job getJob(JobReference jobRef) throws IOException, InterruptedException;
  }

  /** An interface to get, create and delete Cloud BigQuery datasets and tables. */
  interface DatasetService extends AutoCloseable {

    // maps the values at
    // https://cloud.google.com/bigquery/docs/reference/rest/v2/tables/get#TableMetadataView
    enum TableMetadataView {
      TABLE_METADATA_VIEW_UNSPECIFIED,
      BASIC,
      STORAGE_STATS,
      FULL;
    };

    /**
     * Gets the specified {@link Table} resource by table ID.
     *
     * <p>Returns null if the table is not found.
     */
    @Nullable
    Table getTable(TableReference tableRef) throws InterruptedException, IOException;

    @Nullable
    Table getTable(TableReference tableRef, List<String> selectedFields)
        throws InterruptedException, IOException;

    @Nullable
    Table getTable(TableReference tableRef, List<String> selectedFields, TableMetadataView view)
        throws InterruptedException, IOException;

    /** Creates the specified table if it does not exist. */
    void createTable(Table table) throws InterruptedException, IOException;

    /**
     * Deletes the table specified by tableId from the dataset. If the table contains data, all the
     * data will be deleted.
     */
    void deleteTable(TableReference tableRef) throws IOException, InterruptedException;

    /**
     * Returns true if the table is empty.
     *
     * @throws IOException if the table is not found.
     */
    boolean isTableEmpty(TableReference tableRef) throws IOException, InterruptedException;

    /** Gets the specified {@link Dataset} resource by dataset ID. */
    Dataset getDataset(String projectId, String datasetId) throws IOException, InterruptedException;

    /**
     * Create a {@link Dataset} with the given {@code location}, {@code description} and default
     * expiration time for tables in the dataset (if {@code null}, tables don't expire).
     */
    void createDataset(
        String projectId,
        String datasetId,
        @Nullable String location,
        @Nullable String description,
        @Nullable Long defaultTableExpirationMs)
        throws IOException, InterruptedException;

    /**
     * Deletes the dataset specified by the datasetId value.
     *
     * <p>Before you can delete a dataset, you must delete all its tables.
     */
    void deleteDataset(String projectId, String datasetId) throws IOException, InterruptedException;

    /**
     * Inserts {@link TableRow TableRows} with the specified insertIds if not null.
     *
     * <p>If any insert fail permanently according to the retry policy, those rows are added to
     * failedInserts.
     *
     * <p>Returns the total bytes count of {@link TableRow TableRows}.
     */
    <T> long insertAll(
        TableReference ref,
        List<FailsafeValueInSingleWindow<TableRow, TableRow>> rowList,
        @Nullable List<String> insertIdList,
        InsertRetryPolicy retryPolicy,
        List<ValueInSingleWindow<T>> failedInserts,
        ErrorContainer<T> errorContainer,
        boolean skipInvalidRows,
        boolean ignoreUnknownValues,
        boolean ignoreInsertIds,
        List<ValueInSingleWindow<TableRow>> successfulRows)
        throws IOException, InterruptedException;

    /** Patch BigQuery {@link Table} description. */
    Table patchTableDescription(TableReference tableReference, @Nullable String tableDescription)
        throws IOException, InterruptedException;
  }

  /** An interface to get, create and flush Cloud BigQuery STORAGE API write streams. */
  interface WriteStreamService extends AutoCloseable {
    /** Create a Write Stream for use with the Storage Write API. */
    WriteStream createWriteStream(String tableUrn, WriteStream.Type type)
        throws IOException, InterruptedException;

    @Nullable
    TableSchema getWriteStreamSchema(String writeStream);

    /**
     * Create an append client for a given Storage API write stream. The stream must be created
     * first.
     */
    StreamAppendClient getStreamAppendClient(
        String streamName,
        DescriptorProtos.DescriptorProto descriptor,
        boolean useConnectionPool,
        AppendRowsRequest.MissingValueInterpretation missingValueInterpretation)
        throws Exception;

    /** Flush a given stream up to the given offset. The stream must have type BUFFERED. */
    ApiFuture<FlushRowsResponse> flush(String streamName, long flushOffset)
        throws IOException, InterruptedException;

    /**
     * Finalize a write stream. After finalization, no more records can be appended to the stream.
     */
    ApiFuture<FinalizeWriteStreamResponse> finalizeWriteStream(String streamName);

    /** Commit write streams of type PENDING. The streams must be finalized before committing. */
    ApiFuture<BatchCommitWriteStreamsResponse> commitWriteStreams(
        String tableUrn, Iterable<String> writeStreamNames);
  }

  /** An interface for appending records to a Storage API write stream. */
  interface StreamAppendClient extends AutoCloseable {
    /** Append rows to a Storage API write stream at the given offset. */
    ApiFuture<AppendRowsResponse> appendRows(long offset, ProtoRows rows) throws Exception;

    /** If the table schema has been updated, returns the new schema. Otherwise returns null. */
    @Nullable
    TableSchema getUpdatedSchema();

    /**
     * If the previous call to appendRows blocked due to flow control, returns how long the call
     * blocked for.
     */
    default long getInflightWaitSeconds() {
      return 0;
    }

    /**
     * Pin this object. If close() is called before all pins are removed, the underlying resources
     * will not be freed until all pins are removed.
     */
    void pin();

    /**
     * Unpin this object. If the object has been closed, this will release any underlying resources.
     */
    void unpin() throws Exception;
  }

  /**
   * Container for reading data from streaming endpoints.
   *
   * <p>An implementation does not need to be thread-safe.
   */
  interface BigQueryServerStream<T> extends Iterable<T>, Serializable {
    /**
     * Cancels the stream, releasing any client- and server-side resources. This method may be
     * called multiple times and from any thread.
     */
    void cancel();
  }

  /** An interface representing a client object for making calls to the BigQuery Storage API. */
  interface StorageClient extends AutoCloseable {
    /**
     * Create a new read session against an existing table. This method variant collects request
     * count metric, table id in the request.
     */
    ReadSession createReadSession(CreateReadSessionRequest request);

    /** Read rows in the context of a specific read stream. */
    BigQueryServerStream<ReadRowsResponse> readRows(ReadRowsRequest request);

    /* This method variant collects request count metric, using the fullTableID metadata. */
    BigQueryServerStream<ReadRowsResponse> readRows(ReadRowsRequest request, String fullTableId);

    SplitReadStreamResponse splitReadStream(SplitReadStreamRequest request);

    /* This method variant collects request count metric, using the fullTableID metadata. */
    SplitReadStreamResponse splitReadStream(SplitReadStreamRequest request, String fullTableId);

    /**
     * Call this method on Work Item thread to report outstanding metrics.
     *
     * <p>Because incrementing metrics is only supported on the execution thread, callback thread
     * that has pending metrics cannot report it directly.
     */
    default void reportPendingMetrics() {}

    /**
     * Close the client object.
     *
     * <p>The override is required since {@link AutoCloseable} allows the close method to raise an
     * exception.
     */
    @Override
    void close();
  }
}

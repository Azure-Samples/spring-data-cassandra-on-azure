/*
 * The MIT License (MIT)
 *
 * Copyright (c) Microsoft. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.azure.cosmos.cassandra;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.connection.ClosedConnectionException;
import com.datastax.oss.driver.api.core.connection.HeartbeatException;
import com.datastax.oss.driver.api.core.context.DriverContext;
import com.datastax.oss.driver.api.core.servererrors.WriteType;
import com.datastax.oss.driver.api.core.session.Request;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.datastax.oss.driver.api.core.servererrors.CoordinatorException;
import com.datastax.oss.driver.api.core.servererrors.OverloadedException;
import com.datastax.oss.driver.api.core.servererrors.WriteFailureException;
import com.datastax.oss.driver.api.core.retry.RetryDecision;
import com.datastax.oss.driver.api.core.retry.RetryPolicy;

import java.util.Random;

/**
 * Implements a Cassandra {@link RetryPolicy} with back-offs for
 * {@link OverloadedException} failures
 * <p>
 * {@link #maxRetryCount} specifies the number of retries that should be
 * attempted. A value of -1 specifies that an indefinite number of retries
 * should be attempted. For {@link #onReadTimeout}, {@link #onWriteTimeout}, and
 * {@link #onUnavailable}, we retry immediately. For onRequestErrors such as
 * OverLoadedError, we try to parse the exception message and use RetryAfterMs
 * field provided from the server as the back-off duration. If RetryAfterMs is
 * not available, we default to exponential growing back-off scheme. In this
 * case the time between retries is increased by
 * {@link #growingBackOffTimeMillis} milliseconds (default: 1000 ms) on each
 * retry, unless maxRetryCount is -1, in which case we back-off with fixed
 * {@link #fixedBackOffTimeMillis} duration.
 * </p>
 */
public final class CosmosRetryPolicy implements RetryPolicy {

    public CosmosRetryPolicy(DriverContext context, String profileName) {
        Config root = ConfigFactory.load();

        // The driver's built-in defaults, under the default prefix in reference.conf:
        Config reference = root.getConfig("datastax-java-driver");
        this.maxRetryCount = Integer
                .parseInt(reference.getString("profiles.cosmos-policies.cosmos.retry-policy.maxRetryCount"));
        this.fixedBackOffTimeMillis = Integer
                .parseInt(reference.getString("profiles.cosmos-policies.cosmos.retry-policy.fixedBackOffTimeMillis"));
        this.growingBackOffTimeMillis = Integer
                .parseInt(reference.getString("profiles.cosmos-policies.cosmos.retry-policy.growingBackOffTimeMillis"));
    }

    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    @Override
    public void close() {
    }

    @Override
    public RetryDecision onReadTimeout(Request request, ConsistencyLevel consistencyLevel, int requiredResponses,
            int receivedResponses, boolean dataRetrieved, int retryNumber) {
        return retryManyTimesOrThrow(retryNumber);
    }

    @Override
    public RetryDecision onErrorResponse(Request request, CoordinatorException error, int retryNumber) {
        RetryDecision retryDecision;

        try {
            if (error instanceof OverloadedException || error instanceof WriteFailureException) {
                if (this.maxRetryCount == -1 || retryNumber < this.maxRetryCount) {
                    int retryMillis;
                    retryMillis = (this.maxRetryCount == -1) ? this.fixedBackOffTimeMillis
                            : this.growingBackOffTimeMillis * retryNumber + random.nextInt(growingBackOffSaltMillis);
                    Thread.sleep(retryMillis);
                    retryDecision = RetryDecision.RETRY_SAME;
                } else {
                    retryDecision = RetryDecision.RETHROW;
                }
            } else {
                retryDecision = RetryDecision.RETHROW;
            }
        } catch (InterruptedException exception) {
            retryDecision = RetryDecision.RETHROW;
        }

        return retryDecision;
    }

    @Override
    public RetryDecision onUnavailable(Request request, ConsistencyLevel consistencyLevel, int requiredReplica,
            int aliveReplica, int retryNumber) {
        return retryManyTimesOrThrow(retryNumber);
    }

    @Override
    public RetryDecision onWriteTimeout(Request request, ConsistencyLevel consistencyLevel, WriteType writeType,
            int requiredAcks, int receivedAcks, int retryNumber) {
        return retryManyTimesOrThrow(retryNumber);
    }

    private final static Random random = new Random();
    private final int growingBackOffSaltMillis = 2000;
    private int fixedBackOffTimeMillis = 1000;
    private int growingBackOffTimeMillis = 100;
    private int maxRetryCount = 10;

    private RetryDecision retryManyTimesOrThrow(int retryNumber) {
        return (this.maxRetryCount == -1 || retryNumber < this.maxRetryCount) ? RetryDecision.RETRY_NEXT
                : RetryDecision.RETHROW;
    }

    @Override
    public RetryDecision onRequestAborted(Request request, Throwable error, int retryNumber) {
        if(error instanceof ClosedConnectionException || error instanceof HeartbeatException){
            return retryManyTimesOrThrow(retryNumber);
        }
        return null;
    }

}
/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.indices.replication.checkpoint.crosscluster;

import org.opensearch.action.ActionResponse;
import org.opensearch.common.bytes.BytesReference;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.common.lease.Releasable;
import org.opensearch.index.store.StoreFileMetadata;
import org.opensearch.indices.recovery.MultiChunkTransfer;
import org.opensearch.indices.replication.SegmentFileTransferHandler;

import java.io.IOException;

public class GetSegmentChunkResponse extends ActionResponse implements MultiChunkTransfer.ChunkRequest {

    private StoreFileMetadata md;
    private BytesReference content;
    private long position;
    private boolean lastChunk;


    public GetSegmentChunkResponse(StoreFileMetadata md, BytesReference content, long position, boolean lastChunk) {
        this.md = md;
        this.content = content;
        this.position = position;
        this.lastChunk = lastChunk;
    }

    public GetSegmentChunkResponse(StreamInput streamInput) {
        try {
            this.md = new StoreFileMetadata(streamInput);
            this.content = streamInput.readBytesReference();
            this.position = streamInput.readLong();
            this.lastChunk = streamInput.readBoolean();
        } catch (IOException e) {
            this.md = null;
            this.content = null;
            this.position = 0;
            this.lastChunk = false;
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        md.writeTo(out);
        out.writeBytesReference(content);
        out.writeLong(position);
        out.writeBoolean(lastChunk);
    }

    public StoreFileMetadata getMd() {
        return md;
    }

    public BytesReference getContent() {
        return content;
    }

    public long getPosition() {
        return position;
    }

    public boolean isLastChunk() {
        return lastChunk;
    }

    @Override
    public boolean lastChunk() {
        return lastChunk;
    }

}

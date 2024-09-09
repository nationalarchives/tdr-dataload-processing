# TDR Data Load Processing Lambda

Lambda to handle the processing of metadata for non-network drive transfers.

The Lambda consumes the aggregated metadata for a non-network drive transfer.

The metadata needs to be aggregated before processing as the directory structure needs to be constructed from the individual filepaths of the records

The Lambda takes an input event:
```json
    {
      "userId":  "f0a73877-6057-4bbb-a1eb-7c7b73cab586",
      "s3SourceBucket":  "some-bucket",
      "s3SourceKey" :  "some-key.json"
    }
```

* `userId`: Id of the user who uploaded the data
* `s3SourceBucket`: S3 bucket containing the aggregated metadata
* `s3SourceKey`: S3 key of the aggregated metadata json

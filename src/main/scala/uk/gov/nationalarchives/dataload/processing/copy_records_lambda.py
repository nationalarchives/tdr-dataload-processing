s3_client = boto3.client('s3')


def lambda_handler(event, context):
    source_bucket = event['SourceBucket']
    source_key = event['SourceKey']
    destination_bucket = event.get('DestinationBucket', source_bucket)
    destination_key = event.get('DestinationKey', source_key)
    try:
        copy_source = { 'Bucket': source_bucket, 'Key': source_key }
        s3_client.copy(copy_source, destination_bucket, destination_key)
        print(f"Copy of {source_key} completed successfully.")
    except Exception as e:
        print(f"Error during copy of {source_key}: {e}")
        raise e
